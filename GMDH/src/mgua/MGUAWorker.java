package mgua;

import Matrix.Matrix;
import Matrix.NoSquareException;
import mpi.*;

/**
 * MGUAWorker encapsulates the work performed by every non-root MPI process.
 *
 * Protocol (tags defined in MPITags):
 *
 *  1. Receive broadcast matrices mX, mY, mXB, mYB from root (rank 0).
 *  2. Receive own slice of model indices {start, end} via point-to-point.
 *  3. For each model in [start, end):
 *       - compute LS, Regularity, Unbiasedness criteria
 *       - pack into ModelResult.toFlat()
 *  4. Send results array back to root.
 *
 * The worker is stateless; all state is passed in via MPI messages.
 */
public class MGUAWorker {

    private final int rank;
    private final int size;

    public MGUAWorker(int rank, int size) {
        this.rank = rank;
        this.size = size;
    }

    /**
     * Entry point called after MPI.Init().
     * Blocks until root sends SHUTDOWN signal (modelCount == 0).
     */
    public void run() throws Exception {
        System.out.printf("[Worker %d] Started, waiting for data.\n", rank);

        // ----------------------------------------------------------------
        // Step 1 – receive shared matrices broadcast from root
        // ----------------------------------------------------------------
        Matrix mX  = receiveBroadcastMatrix(10);
        Matrix mY  = receiveBroadcastMatrix(20);
        Matrix mXB = receiveBroadcastMatrix(30);
        Matrix mYB = receiveBroadcastMatrix(40);

        System.out.printf("[Worker %d] Matrices received: X(%dx%d) Y(%dx%d).\n",
                rank, mX.getNrows(), mX.getNcols(), mY.getNrows(), mY.getNcols());

        // ----------------------------------------------------------------
        // Step 2 – receive all model definitions
        // ----------------------------------------------------------------
        int[] modelCountBuf = new int[1];
        MPI.COMM_WORLD.Recv(modelCountBuf, 0, 1, MPI.INT, 0, 50);
        int totalModels = modelCountBuf[0];

        if (totalModels == 0) {
            System.out.printf("[Worker %d] Shutdown signal received.\n", rank);
            return;
        }

        int modelLen = mX.getNcols(); // length of each binary model vector
        double[] allModelsFlat = new double[totalModels * modelLen];
        MPI.COMM_WORLD.Recv(allModelsFlat, 0, allModelsFlat.length,
                MPI.DOUBLE, 0, 60);

        // ----------------------------------------------------------------
        // Step 3 – receive slice assignment {start, end}
        // ----------------------------------------------------------------
        int[] sliceBuf = new int[2];
        MPI.COMM_WORLD.Recv(sliceBuf, 0, 2, MPI.INT, 0, 70);
        int sliceStart = sliceBuf[0];
        int sliceEnd   = sliceBuf[1];
        int sliceSize  = sliceEnd - sliceStart;

        System.out.printf("[Worker %d] Computing models [%d, %d).\n",
                rank, sliceStart, sliceEnd);

        // ----------------------------------------------------------------
        // Step 4 – evaluate each assigned model
        // ----------------------------------------------------------------
        // Each result = ModelResult.toFlat() which has length (4 + modelLen)
        int resultWidth = 4 + modelLen;
        double[] resultsFlat = new double[sliceSize * resultWidth];

        for (int i = 0; i < sliceSize; i++) {
            int globalIdx = sliceStart + i;

            // Reconstruct model mask from flat storage
            int[] model = new int[modelLen];
            for (int j = 0; j < modelLen; j++)
                model[j] = (int) allModelsFlat[globalIdx * modelLen + j];

            double ls  = Double.MAX_VALUE;
            double reg = Double.MAX_VALUE;
            double unb = Double.MAX_VALUE;
            try {
                ls  = MGUACore.getCriterionLS(model, mX, mY);
                reg = MGUACore.getCriterionReg(model, mX, mY, mXB, mYB);
                unb = MGUACore.getCriterionUnb(model, mX, mY, mXB, mYB);
            } catch (NoSquareException e) {
                System.out.printf("[Worker %d] NoSquareException for model %d, using MAX_VALUE.\n",
                        rank, globalIdx);
            }

            ModelResult mr = new ModelResult(globalIdx, model, ls, reg, unb);
            double[] flat  = mr.toFlat();
            System.arraycopy(flat, 0, resultsFlat, i * resultWidth, resultWidth);

            System.out.printf("[Worker %d] Model %d done: LS=%.4f Reg=%.4f Unb=%.4f\n",
                    rank, globalIdx, ls, reg, unb);
        }

        // ----------------------------------------------------------------
        // Step 5 – send results back to root
        // ----------------------------------------------------------------
        // First send the count so root knows buffer size
        MPI.COMM_WORLD.Send(new int[]{sliceSize}, 0, 1,
                MPI.INT, 0, 80);
        MPI.COMM_WORLD.Send(resultsFlat, 0, resultsFlat.length,
                MPI.DOUBLE, 0, 90);

        System.out.printf("[Worker %d] Sent %d results to root.\n", rank, sliceSize);
    }

    // -------------------------------------------------------------------------
    // Helper: receive a matrix that root sent as flat double[]
    // -------------------------------------------------------------------------
    private Matrix receiveBroadcastMatrix(int tag) throws Exception {
        // Root sends header [nrows, ncols] first
        int[] header = new int[2];
        MPI.COMM_WORLD.Recv(header, 0, 2, MPI.INT, 0, tag);
        int nrows = header[0];
        int ncols = header[1];

        double[] flat = new double[nrows * ncols];
        MPI.COMM_WORLD.Recv(flat, 0, flat.length, MPI.DOUBLE, 0, tag + 1);

        double[][] data = new double[nrows][ncols];
        for (int i = 0; i < nrows; i++)
            for (int j = 0; j < ncols; j++)
                data[i][j] = flat[i * ncols + j];
        return new Matrix(data);
    }
}