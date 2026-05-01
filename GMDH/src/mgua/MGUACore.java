package mgua;

import Matrix.Matrix;
import Matrix.MatrixMathematics;
import Matrix.NoSquareException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MGUACore contains all mathematical / algorithmic logic of the GMDH method.
 * It is deliberately free of MPI and JavaFX dependencies so it can be
 * unit-tested independently and reused by both the sequential and parallel entry points.
 *
 * Responsibilities:
 *  - Data generation / reading
 *  - Train/test (A/B) split
 *  - Model enumeration (setOfModels)
 *  - Per-model criteria evaluation (LS, Regularity, Unbiasedness)
 *  - Optimal model selection and final coefficient refinement
 *  - Result reporting
 */
public class MGUACore {

    // -------------------------------------------------------------------------
    // Data preparation
    // -------------------------------------------------------------------------

    /**
     * Generate synthetic 7-point dataset: y = -1 - 2*x2 + noise
     */
    public static double[][] getData() {
        double[][] data = new double[7][3];
        double delta = 1.0;
        double[][] xvals = {{-2,-1},{-1,0},{0,5},{1,2},{2,4},{3,-2},{4,5}};
        for (int i = 0; i < 7; i++) {
            data[i][0] = xvals[i][0];
            data[i][1] = xvals[i][1];
            data[i][2] = -1.0 - 2.0 * data[i][1] + delta * new Random().nextGaussian();
        }
        return data;
    }

    /**
     * Read whitespace-delimited data from a text file.
     */
    public static double[][] readData(String name, int rows, int cols) {
        double[][] data = new double[rows][cols];
        try (BufferedReader in = new BufferedReader(new FileReader(name))) {
            int i = 0;
            String s = in.readLine();
            while (s != null && i < rows) {
                StringTokenizer token = new StringTokenizer(s);
                int j = 0;
                while (token.hasMoreTokens() && j < cols)
                    data[i][j++] = Double.parseDouble(token.nextToken());
                i++;
                s = in.readLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(MGUACore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }

    /**
     * Split raw data into training (A) and validation (B) halves.
     * Even-indexed rows → A, odd-indexed rows → B.
     *
     * @return [mX, mY, mXB, mYB] — four matrices ready for model fitting
     */
    public static Matrix[] splitData(double[][] data) {
        int n = data.length;
        int m = data[0].length - 1;

        int nA = n / 2 + n % 2;
        int nB = n - nA;

        double[][] xA = new double[nA][m];
        double[] yA   = new double[nA];
        double[][] xB = new double[nB][m];
        double[] yB   = new double[nB];

        for (int j = 0; j < m; j++) {
            int ii = 0;
            while (2 * ii < n)     { xA[ii][j] = data[2*ii][j];   ii++; }
            ii = 0;
            while (2*ii+1 < n)     { xB[ii][j] = data[2*ii+1][j]; ii++; }
        }
        int ii = 0;
        while (2*ii < n)   { yA[ii] = data[2*ii][m];   ii++; }
        ii = 0;
        while (2*ii+1 < n) { yB[ii] = data[2*ii+1][m]; ii++; }

        // Build design matrices with prepended intercept column
        double[][] X  = buildDesignMatrix(xA, nA, m);
        double[][] XB = buildDesignMatrix(xB, nB, m);

        double[][] Y  = colVector(yA);
        double[][] YB = colVector(yB);

        return new Matrix[]{new Matrix(X), new Matrix(Y),
                new Matrix(XB), new Matrix(YB)};
    }

    private static double[][] buildDesignMatrix(double[][] x, int rows, int m) {
        double[][] X = new double[rows][m+1];
        for (int j = 0; j < rows; j++) {
            X[j][0] = 1.0;
            System.arraycopy(x[j], 0, X[j], 1, m);
        }
        return X;
    }

    private static double[][] colVector(double[] y) {
        double[][] Y = new double[y.length][1];
        for (int j = 0; j < y.length; j++) Y[j][0] = y[j];
        return Y;
    }

    // -------------------------------------------------------------------------
    // Model enumeration
    // -------------------------------------------------------------------------

    /**
     * Enumerate all 2^q - 1 non-trivial binary feature subsets for q predictors.
     * Each model is an int[] of length (q+1): index 0 = intercept flag.
     */
    public static int[][] setOfModels(int q) {
        int min = pow2(q) + 1;
        int max = 0;
        for (int i = 0; i <= q; i++) max += pow2(i);

        int[][] models = new int[pow2(q) - 1][q + 1];
        int i = 0;
        for (int j = min; j <= max; j++)
            models[i++] = convertIntToBinary(q, j);
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

    public static int pow2(int n) {
        if (n < 0) return -1;
        int a = 1;
        for (int j = 1; j <= n; j++) a *= 2;
        return a;
    }

    // -------------------------------------------------------------------------
    // Per-model criteria  (all public so workers can call them directly)
    // -------------------------------------------------------------------------

    /** Least-squares criterion on training set A (normalized). */
    public static double getCriterionLS(int[] model, Matrix mx, Matrix my) throws NoSquareException {
        return getCriterionLSRaw(model, mx, my) / getSquaresOfY(my);
    }

    private static double getCriterionLSRaw(int[] model, Matrix mx, Matrix my) throws NoSquareException {
        Matrix XofModel = subMatrix(model, mx);
        Matrix mB = regressParam(XofModel, my);
        if (mB == null) return Double.MAX_VALUE;
        double c = 0;
        Matrix mYmod = MatrixMathematics.multiply(XofModel, mB);
        for (int j = 0; j < my.getNrows(); j++) {
            double diff = my.getValueAt(j,0) - mYmod.getValueAt(j,0);
            c += diff * diff;
        }
        return c;
    }

    /** Regularity criterion: training params evaluated on validation set B (normalized). */
    public static double getCriterionReg(int[] model, Matrix mx, Matrix my,
                                         Matrix mxB, Matrix myB) throws NoSquareException {
        Matrix XofModel = subMatrix(model, mx);
        Matrix mB = regressParam(XofModel, my);
        if (mB == null) return Double.MAX_VALUE;
        double c = 0;
        Matrix mYmodOnB = MatrixMathematics.multiply(subMatrix(model, mxB), mB);
        for (int j = 0; j < myB.getNrows(); j++) {
            double diff = myB.getValueAt(j,0) - mYmodOnB.getValueAt(j,0);
            c += diff * diff;
        }
        return c / getSquaresOfY(myB);
    }

    /** Unbiasedness (зсуву) criterion (normalized). */
    public static double getCriterionUnb(int[] model, Matrix mx, Matrix my,
                                         Matrix mxB, Matrix myB) throws NoSquareException {
        Matrix mBforA = regressParam(subMatrix(model, mx),  my);
        Matrix mBforB = regressParam(subMatrix(model, mxB), myB);
        if (mBforA == null || mBforB == null) return Double.MAX_VALUE;

        Matrix YmodAA = MatrixMathematics.multiply(subMatrix(model, mx),  mBforA);
        Matrix YmodAB = MatrixMathematics.multiply(subMatrix(model, mxB), mBforA);
        Matrix YmodBA = MatrixMathematics.multiply(subMatrix(model, mx),  mBforB);
        Matrix YmodBB = MatrixMathematics.multiply(subMatrix(model, mxB), mBforB);

        double c = 0;
        for (int j = 0; j < my.getNrows(); j++) {
            double d = YmodAA.getValueAt(j,0) - YmodBA.getValueAt(j,0);
            c += d * d;
        }
        for (int j = 0; j < myB.getNrows(); j++) {
            double d = YmodAB.getValueAt(j,0) - YmodBB.getValueAt(j,0);
            c += d * d;
        }
        return c / (getSquaresOfY(my) + getSquaresOfY(myB));
    }

    // -------------------------------------------------------------------------
    // Regression helper
    // -------------------------------------------------------------------------

    /** OLS: b = (X'X)^{-1} X'y.  Returns null if X'X is singular. */
    public static Matrix regressParam(Matrix x, Matrix y) throws NoSquareException {
        Matrix XtX = MatrixMathematics.multiply(MatrixMathematics.transpose(x), x);
        if (MatrixMathematics.determinant(XtX) == 0) {
            System.out.println("[WARN] Singular matrix, skipping model.");
            return null;
        }
        Matrix inv = MatrixMathematics.inverse(XtX);
        return MatrixMathematics.multiply(inv, MatrixMathematics.multiply(
                MatrixMathematics.transpose(x), y));
    }

    /** Extract columns from X according to binary model mask. */
    public static Matrix subMatrix(int[] model, Matrix X) {
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

    // -------------------------------------------------------------------------
    // Post-selection: refine on full dataset A∪B and write results
    // -------------------------------------------------------------------------

    /**
     * Given the optimal model mask, refit on the combined A∪B data,
     * print the formula, and return [XAB, YAB, Ymod].
     */
    public static Matrix[] refineAndReport(int[] modelOpt, double minCriterion,
                                           Matrix mX, Matrix mY,
                                           Matrix mXB, Matrix mYB,
                                           int m) throws NoSquareException, IOException {
        int nA = mX.getNrows();
        int nB = mXB.getNrows();

        double[][] XAB = new double[nA + nB][m + 1];
        double[][] YAB = new double[nA + nB][1];

        for (int j = 0; j < nA; j++) {
            for (int i = 0; i <= m; i++) XAB[j][i] = mX.getValueAt(j, i);
            YAB[j][0] = mY.getValueAt(j, 0);
        }
        for (int j = 0; j < nB; j++) {
            for (int i = 0; i <= m; i++) XAB[nA+j][i] = mXB.getValueAt(j, i);
            YAB[nA+j][0] = mYB.getValueAt(j, 0);
        }

        Matrix matXAB = new Matrix(XAB);
        Matrix matYAB = new Matrix(YAB);

        Matrix mB = regressParam(subMatrix(modelOpt, matXAB), matYAB);

        String[] nameX = new String[m + 1];
        nameX[0] = "";
        for (int j = 1; j <= m; j++) nameX[j] = "x" + j;

        PrintWriter output = new PrintWriter(new FileWriter("RESULTS_MPI.txt"));
        System.out.print("\n f(x) = ");
        output.print("\n f(x) = ");
        int k = 0;
        for (int j = 0; j <= m; j++) {
            if (modelOpt[j] == 1) {
                double coeff = mB.getValues()[k][0];
                if (j == 0 && coeff < 0) { System.out.print(" - "); output.print(" - "); }
                System.out.printf("%5.6f %s", Math.abs(coeff), nameX[j]);
                output.printf("%5.6f %s", Math.abs(coeff), nameX[j]);
                k++;
                if (k < mB.getNrows()) {
                    double next = mB.getValues()[k][0];
                    System.out.print(next >= 0 ? " + " : " - ");
                    output.print(next >= 0 ? " + " : " - ");
                }
            }
        }
        System.out.printf("\n Criterion = %.9f\n", minCriterion);
        output.printf("\n Criterion = %.9f\n", minCriterion);
        if (minCriterion < 0.05) {
            System.out.println(" Congratulations! You have a quality model.");
            output.println(" Congratulations! You have a quality model.");
        }
        output.close();

        Matrix mYmod = MatrixMathematics.multiply(subMatrix(modelOpt, matXAB), mB);
        return new Matrix[]{matXAB, matYAB, mYmod};
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    public static double getSquaresOfY(Matrix my) {
        double c = 0;
        for (int j = 0; j < my.getNrows(); j++) {
            double v = my.getValueAt(j, 0);
            c += v * v;
        }
        return c;
    }

    public static void print(double[][] x) {
        for (double[] row : x) {
            for (double v : row) System.out.print(v + "\t");
            System.out.println();
        }
    }
}