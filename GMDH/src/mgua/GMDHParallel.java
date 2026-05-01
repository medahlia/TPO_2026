package mgua;

import Matrix.Matrix;
import Matrix.MatrixMathematics;
import Matrix.NoSquareException;
import mpi.*;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GMDHParallel — паралельний МГУА через MPI (MPJ Express v0.44).
 *
 * Компіляція (у Terminal, у папці проєкту):
 *   javac -cp "/Users/dasha/mpj-v0_44/lib/mpj.jar" -d out Matrix/*.java mgua/GMDHParallel.java
 *
 * Запуск:
 *   export MPJ_HOME=/Users/dasha/mpj-v0_44
 *   export PATH=$MPJ_HOME/bin:$PATH
 *   mpjrun.sh -np 4 -cp out mgua.GMDHParallel
 *
 * ПРИМІТКА: MPI.DOUBLE_INT та MPI.MINLOC відсутні у MPJ Express v0.44.
 * Замість Reduce(MINLOC) використовується:
 *   1. MPI.Reduce з MPI.MIN для знаходження мінімального значення критерію.
 *   2. Після цього root вручну шукає індекс моделі з цим мінімумом.
 */
public class GMDHParallel {

    // --- MPI теги ---
    private static final int TAG_HEADER_X    = 10, TAG_DATA_X    = 11;
    private static final int TAG_HEADER_Y    = 20, TAG_DATA_Y    = 21;
    private static final int TAG_HEADER_XB   = 30, TAG_DATA_XB   = 31;
    private static final int TAG_HEADER_YB   = 40, TAG_DATA_YB   = 41;
    private static final int TAG_MODEL_COUNT = 50;
    private static final int TAG_MODEL_LEN   = 51;
    private static final int TAG_MODELS      = 60;
    private static final int TAG_SLICE       = 70;
    private static final int TAG_RES_COUNT   = 80;
    private static final int TAG_RESULTS     = 90;

    public static void main(String[] args) throws Exception {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        if (rank == 0) {
            runRoot(size);
        } else {
            runWorker(rank);
        }

        MPI.Finalize();
    }

    // =========================================================================
    // ROOT (rank == 0)
    // =========================================================================

    private static void runRoot(int size) throws Exception {
        System.out.println("[Root] Запуск МГУА MPI, процесів: " + size);

        // --- Дані ---
        // Для реальних даних розкоментуй:
        //double[][] data = readData("clean_water_data.txt", 2011, 9);
        double[][] data = getData();

        int n = data.length;
        int m = data[0].length - 1;

        // --- Розбивка A/B (парні → A, непарні → B) ---
        int nA = n / 2 + n % 2;
        int nB = n - nA;
        double[][] xA = new double[nA][m], xB = new double[nB][m];
        double[]   yA = new double[nA],   yB = new double[nB];
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < nA; i++) xA[i][j] = data[2*i][j];
            for (int i = 0; i < nB; i++) xB[i][j] = data[2*i+1][j];
        }
        for (int i = 0; i < nA; i++) yA[i] = data[2*i][m];
        for (int i = 0; i < nB; i++) yB[i] = data[2*i+1][m];

        Matrix mX  = new Matrix(buildDesign(xA, nA, m));
        Matrix mY  = colMatrix(yA);
        Matrix mXB = new Matrix(buildDesign(xB, nB, m));
        Matrix mYB = colMatrix(yB);

        // --- Перелік всіх моделей ---
        int[][] models      = setOfModels(m);
        int     totalModels = models.length;
        int     modelLen    = m + 1;
        System.out.printf("[Root] Ознак: %d, моделей: %d%n", m, totalModels);

        // --- Масиви критеріїв (заповнюються нижче) ---
        double[] criterionUnb = new double[totalModels];
        double[] criterionLS  = new double[totalModels];
        double[] criterionReg = new double[totalModels];

        if (size == 1) {
            // ----------------------------------------------------------------
            // Послідовний режим — root рахує все сам
            // ----------------------------------------------------------------
            System.out.println("[Root] Послідовний режим (1 процес).");
            computeSlice(0, totalModels, models, mX, mY, mXB, mYB,
                    criterionLS, criterionReg, criterionUnb);

        } else {
            // ----------------------------------------------------------------
            // Паралельний режим: розсилаємо матриці та шматки моделей
            // ----------------------------------------------------------------

            // Розсилка матриць worker-ам
            for (int w = 1; w < size; w++) {
                sendMatrix(mX,  w, TAG_HEADER_X,  TAG_DATA_X);
                sendMatrix(mY,  w, TAG_HEADER_Y,  TAG_DATA_Y);
                sendMatrix(mXB, w, TAG_HEADER_XB, TAG_DATA_XB);
                sendMatrix(mYB, w, TAG_HEADER_YB, TAG_DATA_YB);
            }

            // Розсилка всіх моделей
            double[] modelsFlat = flattenModels(models, totalModels, modelLen);
            for (int w = 1; w < size; w++) {
                MPI.COMM_WORLD.Send(new int[]{totalModels}, 0, 1, MPI.INT, w, TAG_MODEL_COUNT);
                MPI.COMM_WORLD.Send(new int[]{modelLen},    0, 1, MPI.INT, w, TAG_MODEL_LEN);
                MPI.COMM_WORLD.Send(modelsFlat, 0, modelsFlat.length, MPI.DOUBLE, w, TAG_MODELS);
            }

            // Розподіл шматків між workers
            int workers = size - 1;
            int chunk   = (totalModels + workers - 1) / workers;
            for (int w = 1; w < size; w++) {
                int start = (w - 1) * chunk;
                int end   = Math.min(start + chunk, totalModels);
                MPI.COMM_WORLD.Send(new int[]{start, end}, 0, 2, MPI.INT, w, TAG_SLICE);
                System.out.printf("[Root] Worker %d → моделі [%d, %d)%n", w, start, end);
            }

            // Збираємо результати від workers
            int resultWidth = 4 + modelLen;
            for (int w = 1; w < size; w++) {
                int[] cntBuf = new int[1];
                MPI.COMM_WORLD.Recv(cntBuf, 0, 1, MPI.INT, w, TAG_RES_COUNT);
                double[] flat = new double[cntBuf[0] * resultWidth];
                MPI.COMM_WORLD.Recv(flat, 0, flat.length, MPI.DOUBLE, w, TAG_RESULTS);
                for (int i = 0; i < cntBuf[0]; i++) {
                    int idx = (int) flat[i * resultWidth];
                    criterionLS[idx]  = flat[i * resultWidth + 1];
                    criterionReg[idx] = flat[i * resultWidth + 2];
                    criterionUnb[idx] = flat[i * resultWidth + 3];
                }
                System.out.printf("[Root] ← Отримано %d результатів від worker %d%n",
                        cntBuf[0], w);
            }
        }

        // ------------------------------------------------------------------
        // Емуляція MINLOC через два Reduce — сумісно з MPJ Express v0.44.
        //
        // MPI.DOUBLE_INT / MPI.MINLOC відсутні у MPJ Express v0.44.
        // Стратегія:
        //
        //   Крок 1 — Reduce(MPI.MIN):
        //     Кожен процес надсилає своє локальне мінімальне значення unb.
        //     Root отримує глобальний мінімум globalMinUnb.
        //
        //   Крок 2 — Reduce(MPI.MAX) з кодуванням індексу:
        //     Кожен процес кодує пару (значення, індекс) як одне double:
        //
        //       encoded = -unb * SCALE + localOptIdx
        //
        //     де SCALE достатньо велике, щоб від'ємна частина домінувала.
        //     Процес з мінімальним unb матиме МАКСИМАЛЬНЕ encoded (бо -min
        //     є найбільшим серед -unb). Reduce(MAX) дає це значення root-у,
        //     який декодує індекс назад:
        //
        //       globalOptIdx = round(encoded + globalMinUnb * SCALE)
        //
        //   Таким чином два стандартних Reduce повністю замінюють MINLOC.
        // ------------------------------------------------------------------

        // Крок 1: знаходимо локальний мінімум на root-і
        // (root вже зібрав усі criterionUnb від workers)
        int    localOptIdx = 0;
        double localMin    = criterionUnb[0];
        for (int j = 1; j < totalModels; j++) {
            if (criterionUnb[j] < localMin) {
                localMin    = criterionUnb[j];
                localOptIdx = j;
            }
        }

        double[] sendVal = new double[]{localMin};
        double[] recvVal = new double[1];
        MPI.COMM_WORLD.Reduce(sendVal, 0, recvVal, 0, 1, MPI.DOUBLE, MPI.MIN, 0);
        double globalMinUnb = (size == 1) ? localMin : recvVal[0];

        // Крок 2: кодуємо (значення, індекс) → одне double, Reduce(MAX)
        final double SCALE = 1e9;
        double encodedSend = -localMin * SCALE + localOptIdx;
        double[] sendEnc = new double[]{encodedSend};
        double[] recvEnc = new double[1];
        MPI.COMM_WORLD.Reduce(sendEnc, 0, recvEnc, 0, 1, MPI.DOUBLE, MPI.MAX, 0);

        // Декодуємо глобальний індекс
        int optIdx = (size == 1)
                ? localOptIdx
                : (int) Math.round(recvEnc[0] + globalMinUnb * SCALE);

        int[] modelOpt = models[optIdx];

        System.out.printf("%n[Root] Оптимальна модель: індекс=%d, Unb=%.9f%n",
                optIdx, globalMinUnb);

        // --- Виведення таблиці ---
        PrintWriter out = new PrintWriter(new FileWriter("RESULTS_MPI.txt"));
        System.out.println("\nМодель\t\t\tLS\t\tReg\t\tUnb");
        out.println("Модель\tLS\tReg\tUnb");
        for (int j = 0; j < totalModels; j++) {
            StringBuilder sb = new StringBuilder();
            for (int v : models[j]) sb.append(v);
            String line = String.format("%s\t%.6f\t%.6f\t%.6f",
                    sb, criterionLS[j], criterionReg[j], criterionUnb[j]);
            System.out.println(line);
            out.println(line);
        }

        // --- Рефіт на A∪B ---
        double[][] XAB = new double[nA + nB][m + 1];
        double[][] YAB = new double[nA + nB][1];
        for (int j = 0; j < nA; j++) {
            for (int i = 0; i <= m; i++) XAB[j][i] = mX.getValueAt(j, i);
            YAB[j][0] = yA[j];
        }
        for (int j = 0; j < nB; j++) {
            for (int i = 0; i <= m; i++) XAB[nA + j][i] = mXB.getValueAt(j, i);
            YAB[nA + j][0] = yB[j];
        }
        Matrix mB = regressParam(subMatrix(modelOpt, new Matrix(XAB)), new Matrix(YAB));

        // --- Формула оптимальної моделі ---
        String[] nameX = new String[m + 1];
        nameX[0] = "";
        for (int j = 1; j <= m; j++) nameX[j] = "x" + j;

        System.out.print("\n f(x) = ");
        out.print("\n f(x) = ");
        int k = 0;
        for (int j = 0; j <= m; j++) {
            if (modelOpt[j] == 1) {
                double coeff = mB.getValues()[k][0];
                if (j == 0 && coeff < 0) { System.out.print(" - "); out.print(" - "); }
                System.out.printf("%.6f %s", Math.abs(coeff), nameX[j]);
                out.printf("%.6f %s", Math.abs(coeff), nameX[j]);
                k++;
                if (k < mB.getNrows()) {
                    double next = mB.getValues()[k][0];
                    System.out.print(next >= 0 ? " + " : " - ");
                    out.print(next >= 0 ? " + " : " - ");
                }
            }
        }
        System.out.printf("%n Criterion (Unb) = %.9f%n", globalMinUnb);
        out.printf("%n Criterion (Unb) = %.9f%n", globalMinUnb);
        if (globalMinUnb < 0.05) {
            System.out.println(" Congratulations! You have a quality model.");
            out.println(" Congratulations! You have a quality model.");
        }
        out.close();
        System.out.println("\n[Root] Готово. Результати збережено у RESULTS_MPI.txt");
    }

    // =========================================================================
    // WORKER (rank >= 1)
    // =========================================================================

    private static void runWorker(int rank) throws Exception {
        System.out.printf("[Worker %d] Запущено%n", rank);

        // Отримуємо матриці
        Matrix mX  = recvMatrix(0, TAG_HEADER_X,  TAG_DATA_X);
        Matrix mY  = recvMatrix(0, TAG_HEADER_Y,  TAG_DATA_Y);
        Matrix mXB = recvMatrix(0, TAG_HEADER_XB, TAG_DATA_XB);
        Matrix mYB = recvMatrix(0, TAG_HEADER_YB, TAG_DATA_YB);

        // Отримуємо моделі
        int[] cntBuf = new int[1], lenBuf = new int[1];
        MPI.COMM_WORLD.Recv(cntBuf, 0, 1, MPI.INT, 0, TAG_MODEL_COUNT);
        MPI.COMM_WORLD.Recv(lenBuf, 0, 1, MPI.INT, 0, TAG_MODEL_LEN);
        int totalModels = cntBuf[0], modelLen = lenBuf[0];

        double[] modelsFlat = new double[totalModels * modelLen];
        MPI.COMM_WORLD.Recv(modelsFlat, 0, modelsFlat.length, MPI.DOUBLE, 0, TAG_MODELS);

        // Отримуємо свій шматок
        int[] slice = new int[2];
        MPI.COMM_WORLD.Recv(slice, 0, 2, MPI.INT, 0, TAG_SLICE);
        int sliceStart = slice[0], sliceEnd = slice[1], sliceSize = sliceEnd - sliceStart;
        System.out.printf("[Worker %d] Рахую моделі [%d, %d)%n", rank, sliceStart, sliceEnd);

        double yA2  = squaresOfY(mY);
        double yB2  = squaresOfY(mYB);
        double yAB2 = yA2 + yB2;

        int    resultWidth  = 4 + modelLen;
        double[] resultsFlat = new double[sliceSize * resultWidth];

        // Локальний мінімум для Reduce
        double localMin = Double.MAX_VALUE;

        for (int i = 0; i < sliceSize; i++) {
            int   globalIdx = sliceStart + i;
            int[] model     = new int[modelLen];
            for (int j = 0; j < modelLen; j++)
                model[j] = (int) modelsFlat[globalIdx * modelLen + j];

            double ls = Double.MAX_VALUE, reg = Double.MAX_VALUE, unb = Double.MAX_VALUE;
            try {
                ls  = calcLS (model, mX, mY)            / yA2;
                reg = calcReg(model, mX, mY, mXB, mYB)  / yB2;
                unb = calcUnb(model, mX, mY, mXB, mYB)  / yAB2;
            } catch (NoSquareException e) {
                System.out.printf("[Worker %d] Сингулярна матриця, модель %d%n",
                        rank, globalIdx);
            }

            if (unb < localMin) localMin = unb;

            int base = i * resultWidth;
            resultsFlat[base]     = globalIdx;
            resultsFlat[base + 1] = ls;
            resultsFlat[base + 2] = reg;
            resultsFlat[base + 3] = unb;
            for (int j = 0; j < modelLen; j++)
                resultsFlat[base + 4 + j] = model[j];

            System.out.printf("[Worker %d] Модель %d: LS=%.4f Reg=%.4f Unb=%.4f%n",
                    rank, globalIdx, ls, reg, unb);
        }

        // Надсилаємо результати root-у
        MPI.COMM_WORLD.Send(new int[]{sliceSize}, 0, 1, MPI.INT,    0, TAG_RES_COUNT);
        MPI.COMM_WORLD.Send(resultsFlat, 0, resultsFlat.length,
                MPI.DOUBLE, 0, TAG_RESULTS);

        // ------------------------------------------------------------------
        // Емуляція MINLOC через два Reduce — сумісно з MPJ Express v0.44.
        //
        // Крок 1 — Reduce(MPI.MIN): worker надсилає своє локальне мінімальне
        //   значення unb. Root отримує глобальний мінімум.
        //
        // Крок 2 — Reduce(MPI.MAX) з кодуванням:
        //   encoded = -localMin * SCALE + localOptIdx
        //   Worker з мінімальним unb має максимальний encoded.
        //   Root декодує індекс: optIdx = round(recvMax + globalMin * SCALE)
        //
        // Workers не використовують recvBuf (root == 0).
        // ------------------------------------------------------------------

        // Крок 1
        double[] sendVal = new double[]{localMin};
        double[] recvVal = new double[1];
        MPI.COMM_WORLD.Reduce(sendVal, 0, recvVal, 0, 1, MPI.DOUBLE, MPI.MIN, 0);

        // Крок 2
        final double SCALE   = 1e9;
        int localOptIdxW = sliceStart; // значення вже збережено як localMin
        // знаходимо індекс локального мінімуму в своєму шматку
        for (int i = 0; i < sliceSize; i++) {
            int gIdx = sliceStart + i;
            double unb = resultsFlat[i * (4 + modelLen) + 3];
            if (unb <= localMin) { localOptIdxW = gIdx; }
        }
        double encodedSend = -localMin * SCALE + localOptIdxW;
        double[] sendEnc = new double[]{encodedSend};
        double[] recvEnc = new double[1];
        MPI.COMM_WORLD.Reduce(sendEnc, 0, recvEnc, 0, 1, MPI.DOUBLE, MPI.MAX, 0);

        System.out.printf("[Worker %d] Завершено, локальний мін Unb=%.6f (модель %d)%n",
                rank, localMin, localOptIdxW);
    }

    // =========================================================================
    // Обрахунок шматка моделей (послідовний режим, size==1)
    // =========================================================================

    private static void computeSlice(int start, int end, int[][] models,
                                     Matrix mX, Matrix mY, Matrix mXB, Matrix mYB,
                                     double[] ls, double[] reg, double[] unb) {
        double yA2  = squaresOfY(mY);
        double yB2  = squaresOfY(mYB);
        double yAB2 = yA2 + yB2;
        for (int i = start; i < end; i++) {
            try {
                ls[i]  = calcLS (models[i], mX, mY)            / yA2;
                reg[i] = calcReg(models[i], mX, mY, mXB, mYB)  / yB2;
                unb[i] = calcUnb(models[i], mX, mY, mXB, mYB)  / yAB2;
            } catch (NoSquareException e) {
                ls[i] = reg[i] = unb[i] = Double.MAX_VALUE;
            }
        }
    }

    // =========================================================================
    // MPI helpers
    // =========================================================================

    private static void sendMatrix(Matrix m, int dest, int tagH, int tagD)
            throws Exception {
        MPI.COMM_WORLD.Send(new int[]{m.getNrows(), m.getNcols()}, 0, 2,
                MPI.INT, dest, tagH);
        double[] flat = new double[m.getNrows() * m.getNcols()];
        for (int i = 0; i < m.getNrows(); i++)
            for (int j = 0; j < m.getNcols(); j++)
                flat[i * m.getNcols() + j] = m.getValueAt(i, j);
        MPI.COMM_WORLD.Send(flat, 0, flat.length, MPI.DOUBLE, dest, tagD);
    }

    private static Matrix recvMatrix(int src, int tagH, int tagD) throws Exception {
        int[] h = new int[2];
        MPI.COMM_WORLD.Recv(h, 0, 2, MPI.INT, src, tagH);
        double[] flat = new double[h[0] * h[1]];
        MPI.COMM_WORLD.Recv(flat, 0, flat.length, MPI.DOUBLE, src, tagD);
        double[][] d = new double[h[0]][h[1]];
        for (int i = 0; i < h[0]; i++)
            for (int j = 0; j < h[1]; j++)
                d[i][j] = flat[i * h[1] + j];
        return new Matrix(d);
    }

    // =========================================================================
    // Критерії
    // =========================================================================

    private static double calcLS(int[] model, Matrix mx, Matrix my)
            throws NoSquareException {
        Matrix X    = subMatrix(model, mx);
        Matrix b    = regressParam(X, my);
        if (b == null) return Double.MAX_VALUE;
        Matrix ymod = MatrixMathematics.multiply(X, b);
        double c    = 0;
        for (int j = 0; j < my.getNrows(); j++) {
            double d = my.getValueAt(j, 0) - ymod.getValueAt(j, 0);
            c += d * d;
        }
        return c;
    }

    private static double calcReg(int[] model, Matrix mx, Matrix my,
                                  Matrix mxB, Matrix myB) throws NoSquareException {
        Matrix b = regressParam(subMatrix(model, mx), my);
        if (b == null) return Double.MAX_VALUE;
        Matrix ymodB = MatrixMathematics.multiply(subMatrix(model, mxB), b);
        double c = 0;
        for (int j = 0; j < myB.getNrows(); j++) {
            double d = myB.getValueAt(j, 0) - ymodB.getValueAt(j, 0);
            c += d * d;
        }
        return c;
    }

    private static double calcUnb(int[] model, Matrix mx, Matrix my,
                                  Matrix mxB, Matrix myB) throws NoSquareException {
        Matrix bA = regressParam(subMatrix(model, mx),  my);
        Matrix bB = regressParam(subMatrix(model, mxB), myB);
        if (bA == null || bB == null) return Double.MAX_VALUE;
        Matrix yAA = MatrixMathematics.multiply(subMatrix(model, mx),  bA);
        Matrix yAB = MatrixMathematics.multiply(subMatrix(model, mxB), bA);
        Matrix yBA = MatrixMathematics.multiply(subMatrix(model, mx),  bB);
        Matrix yBB = MatrixMathematics.multiply(subMatrix(model, mxB), bB);
        double c = 0;
        for (int j = 0; j < my.getNrows(); j++) {
            double d = yAA.getValueAt(j, 0) - yBA.getValueAt(j, 0);
            c += d * d;
        }
        for (int j = 0; j < myB.getNrows(); j++) {
            double d = yAB.getValueAt(j, 0) - yBB.getValueAt(j, 0);
            c += d * d;
        }
        return c;
    }

    // =========================================================================
    // Математика
    // =========================================================================

    private static Matrix regressParam(Matrix x, Matrix y) throws NoSquareException {
        Matrix XtX = MatrixMathematics.multiply(MatrixMathematics.transpose(x), x);
        if (MatrixMathematics.determinant(XtX) == 0) return null;
        return MatrixMathematics.multiply(
                MatrixMathematics.inverse(XtX),
                MatrixMathematics.multiply(MatrixMathematics.transpose(x), y));
    }

    private static Matrix subMatrix(int[] model, Matrix X) {
        int cols = 0;
        for (int v : model) if (v == 1) cols++;
        Matrix sub = new Matrix(X.getNrows(), cols);
        int col = 0;
        for (int j = 0; j < model.length; j++) {
            if (model[j] == 1) {
                for (int i = 0; i < X.getNrows(); i++)
                    sub.setValueAt(i, col, X.getValueAt(i, j));
                col++;
            }
        }
        return sub;
    }

    private static int[][] setOfModels(int q) {
        int min = pow2(q) + 1, max = 0;
        for (int i = 0; i <= q; i++) max += pow2(i);
        int[][] models = new int[pow2(q) - 1][q + 1];
        int i = 0;
        for (int j = min; j <= max; j++) models[i++] = convertIntToBinary(q, j);
        return models;
    }

    private static int[] convertIntToBinary(int q, int r) {
        int[] w = new int[q + 1];
        int k = 0;
        for (int j = q; j >= 0; j--) {
            if (r < pow2(j)) { w[k++] = 0; }
            else { w[k++] = r / pow2(j); r = r % pow2(j); }
        }
        return w;
    }

    private static int pow2(int n) {
        if (n < 0) return -1;
        int a = 1;
        for (int j = 1; j <= n; j++) a *= 2;
        return a;
    }

    private static double squaresOfY(Matrix my) {
        double c = 0;
        for (int j = 0; j < my.getNrows(); j++) {
            double v = my.getValueAt(j, 0);
            c += v * v;
        }
        return c;
    }

    private static double[][] buildDesign(double[][] x, int rows, int m) {
        double[][] X = new double[rows][m + 1];
        for (int j = 0; j < rows; j++) {
            X[j][0] = 1.0;
            System.arraycopy(x[j], 0, X[j], 1, m);
        }
        return X;
    }

    private static Matrix colMatrix(double[] y) {
        double[][] Y = new double[y.length][1];
        for (int j = 0; j < y.length; j++) Y[j][0] = y[j];
        return new Matrix(Y);
    }

    private static double[] flattenModels(int[][] models, int total, int len) {
        double[] flat = new double[total * len];
        for (int i = 0; i < total; i++)
            for (int j = 0; j < len; j++)
                flat[i * len + j] = models[i][j];
        return flat;
    }

    // =========================================================================
    // Дані
    // =========================================================================

    /** Синтетичні дані для тесту: y = -1 - 2*x2 + шум */
    private static double[][] getData() {
        double[][] data = new double[7][3];
        double[][] xv = {{-2,-1},{-1,0},{0,5},{1,2},{2,4},{3,-2},{4,5}};
        for (int i = 0; i < 7; i++) {
            data[i][0] = xv[i][0];
            data[i][1] = xv[i][1];
            data[i][2] = -1.0 - 2.0 * data[i][1] + new Random().nextGaussian();
        }
        return data;
    }

    /**
     * Читання даних з файлу.
     * Числа через пробіл/таб, рядки-коментарі з # ігноруються.
     */
    public static double[][] readData(String name, int rows, int cols) {
        double[][] data = new double[rows][cols];
        try (BufferedReader in = new BufferedReader(new FileReader(name))) {
            int i = 0;
            String s = in.readLine();
            while (s != null && i < rows) {
                s = s.trim();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    StringTokenizer t = new StringTokenizer(s);
                    int j = 0;
                    while (t.hasMoreTokens() && j < cols)
                        data[i][j++] = Double.parseDouble(t.nextToken());
                    i++;
                }
                s = in.readLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(GMDHParallel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }
}