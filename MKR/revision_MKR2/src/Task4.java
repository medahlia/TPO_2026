// Напишіть фрагмент коду OpenMPI/MPI Express,
// який виконує паралельне обчислення елементу матриці C,
// де кожний i-ий елемент масиву C обчислюється як добуток
// середнього значення елементів i-ого рядка матриці A та
// середнього значення елементів i-ого рядка матриці B.
// В усіх масивах зберігаються цілі числа. Розмір матриць A і B однаковий n×n.

import mpi.*;
import java.util.Random;

public class Task4 { //ParallelMatrixProduct

    private static final int MASTER = 0;
    private static final int N           = 10;
    private static final int MIN_VAL     = 0;
    private static final int MAX_VAL     = 9;

    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int numProcs = MPI.COMM_WORLD.Size();
        int rank     = MPI.COMM_WORLD.Rank();

        if (numProcs < 2) {
            System.out.println("Need at least 2 processes");
            MPI.COMM_WORLD.Abort(1);
            MPI.Finalize();
            return;
        }

        // ── Крок 1: master генерує матриці ───────────────────────────────
        int[][] matA = null;
        int[][] matB = null;
        if (rank == MASTER) {
            matA = generateMatrix(N, MIN_VAL, MAX_VAL);
            matB = generateMatrix(N, MIN_VAL, MAX_VAL);
        }

        // ── Крок 2: розраховуємо розподіл рядків між процесами ───────────
        int base  = N / numProcs;
        int extra = N % numProcs;

        int[] rowCounts = new int[numProcs];
        int[] offsets   = new int[numProcs];

        for (int i = 0; i < numProcs; i++) {
            rowCounts[i] = (i < extra) ? base + 1 : base;
            offsets[i]   = (i == 0)   ? 0 : offsets[i - 1] + rowCounts[i - 1];
        }

        // ── Крок 3: Scatterv — розсилаємо рядки матриць A і B ────────────
        int localRows      = rowCounts[rank];
        int[][] localRowsA = new int[localRows][N];
        int[][] localRowsB = new int[localRows][N];

        MPI.COMM_WORLD.Scatterv(
                matA,       // джерело (читається тільки у master)
                0,          // зміщення у джерелі
                rowCounts,  // кількість рядків для кожного процесу
                offsets,    // позиції початку для кожного процесу
                MPI.OBJECT, // тип (рядки матриці — масиви)
                localRowsA, // буфер-приймач
                0,          // зміщення у приймачі
                localRows,  // скільки рядків отримує цей процес
                MPI.OBJECT, // тип
                MASTER
        );

        MPI.COMM_WORLD.Scatterv(
                matB,       0, rowCounts, offsets, MPI.OBJECT,
                localRowsB, 0, localRows,          MPI.OBJECT,
                MASTER
        );

        // ── Крок 4: кожен процес обчислює свої елементи вектора C ────────
        // C[i] = avg(A[i]) * avg(B[i]), де avg — середнє значення рядка
        double[] localC = new double[localRows];

        for (int i = 0; i < localRows; i++) {
            double sumA = 0;
            double sumB = 0;
            for (int j = 0; j < N; j++) {
                sumA += localRowsA[i][j];
                sumB += localRowsB[i][j];
            }
            double avgA = sumA / N;
            double avgB = sumB / N;
            localC[i]   = avgA * avgB;
        }

        // ── Крок 5: Gatherv — збираємо вектор C у master ─────────────────
        // Вихідний буфер виділяємо ТІЛЬКИ у master
        double[] resultC = null;
        if (rank == MASTER) {
            resultC = new double[N];
        }

        MPI.COMM_WORLD.Gatherv(
                localC,    // що надсилаємо
                0,         // зміщення у вхідному буфері
                localRows, // скільки надсилаємо
                MPI.DOUBLE,// тип
                resultC,   // куди збираємо (null у не-master — допустимо)
                0,         // зміщення у вихідному буфері
                rowCounts, // скільки очікуємо від кожного процесу
                offsets,   // позиції запису для кожного процесу
                MPI.DOUBLE,// тип
                MASTER
        );

        // ── Крок 6: master виводить результат ────────────────────────────
        if (rank == MASTER) {
            System.out.println("Matrix A:");
            printMatrix(matA);
            System.out.println("Matrix B:");
            printMatrix(matB);
            System.out.println("Result C (avg(A[i]) * avg(B[i])):");
            printArray(resultC);
        }

        MPI.Finalize();
    }

    // ── Генерація матриці N×N з випадковими цілими числами ───────────────
    static int[][] generateMatrix(int size, int minVal, int maxVal) {
        Random rng    = new Random(42);
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = minVal + rng.nextInt(maxVal - minVal + 1);
            }
        }
        return matrix;
    }

    // ── Вивід одновимірного масиву ────────────────────────────────────────
    static void printArray(double[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.printf("%.2f", array[i]);
            if (i < array.length - 1) {
                System.out.print("  ");
            }
        }
        System.out.println();
    }

    // ── Вивід матриці рядок за рядком ────────────────────────────────────
    static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int j = 0; j < row.length; j++) {
                System.out.printf("%3d", row[j]);
                if (j < row.length - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
    }
}

///////////////////////////////////////////////////////////
private static final int MASTER = 0;
private static final int N = 10;
private static final int MIN_VAL = 0;
private static final int MAX_VAL = 9;

public static void main(String[] args) throws Exception {
    MPI.Init(args);

    int numProcs = MPI.COMM_WORLD.Size();
    int rank = MPI.COMM_WORLD.Rank();

    if (numProcs < 2) {
        MPI.COMM_WORLD.Abort(1);
        MPI.Finalize();
        return;
    }

    int[][] matA = null;
    int[][] matB = null;
    if (rank == MASTER) {
        matA = generateMatrix(N, MIN_VAL, MAX_VAL);
        matB = generateMatrix(N, MIN_VAL, MAX_VAL);
    }

    int base = N / numProcs;
    int extra = N % numProcs;

    int[] rowCounts = new int[numProcs];
    int[] offsets = new int[numProcs];

    for (int i = 0; i < numProcs; i++) {
        rowCounts[i] = (i < extra) ? base + 1 : base;
        offsets[i] = (i == 0) ? 0 : offsets[i - 1] + rowCounts[i - 1];
    }

    int localRows = rowCounts[rank];
    int[][] localRowsA = new int[localRows][N];
    int[][] localRowsB = new int[localRows][N];

    MPI.COMM_WORLD.Scatterv(matA, 0, rowCounts, offsets, MPI.OBJECT, localRowsA, 0, localRows, MPI.OBJECT, MASTER);
    MPI.COMM_WORLD.Scatterv(matB, 0, rowCounts, offsets, MPI.OBJECT, localRowsB, 0, localRows, MPI.OBJECT, MASTER);

    double[] localC = new double[localRows];

    for (int i = 0; i < localRows; i++) {
        double sumA = 0;
        double sumB = 0;
        for (int j = 0; j < N; j++) {
            sumA += localRowsA[i][j];
            sumB += localRowsB[i][j];
        }
        double avgA = sumA / N;
        double avgB = sumB / N;
        localC[i] = avgA * avgB;
    }

    double[] resultC = null;
    if (rank == MASTER) {
        resultC = new double[N];
    }

    MPI.COMM_WORLD.Gatherv(localC, 0, localRows, MPI.DOUBLE, resultC, 0, rowCounts, offsets, MPI.DOUBLE, MASTER);

    MPI.Finalize();
}