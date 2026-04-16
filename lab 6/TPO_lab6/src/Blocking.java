import mpi.*;
import java.util.Random;

public class Blocking {

    static final int MASTER = 0;
    static final int TAG_TO_WORK = 1;
    static final int TAG_TO_MAST = 2;

    public static void main(String[] args) {
        MPI.Init(args);

        int numProcs = MPI.COMM_WORLD.Size();
        int rank = MPI.COMM_WORLD.Rank();

        if (numProcs < 2) {
            System.out.println("Need at least 2 processes");
            MPI.COMM_WORLD.Abort(1);
            MPI.Finalize();
            return;
        }

        int numWorkers = numProcs - 1;
        int[] sizes = {100, 500, 1000, 1500, 2000};
        int iterations = 10;

        if (rank == MASTER) {
            System.out.printf("Blocking MPI — %d processes%n%n", numProcs);

            for (int n : sizes) {
                double totalTime = 0.0;

                for (int iter = 0; iter < iterations; iter++) {
                    double[] matA = randomMatrix(n * n);
                    double[] matB = randomMatrix(n * n);
                    double[] matC = new double[n * n];

                    int base = n / numWorkers;
                    int extra = n % numWorkers;
                    int offset = 0;

                    long start = System.currentTimeMillis();

                    // ── Розсилаємо шматки A і повну B кожному worker-у ───
                    for (int dest = 1; dest <= numWorkers; dest++) {
                        int rows = (dest <= extra) ? base + 1 : base;

                        double[] subA = new double[rows * n];
                        System.arraycopy(matA, offset * n, subA, 0, rows * n);

                        MPI.COMM_WORLD.Send(new int[]{offset}, 0, 1,        MPI.INT,    dest, TAG_TO_WORK);
                        MPI.COMM_WORLD.Send(new int[]{rows},   0, 1,        MPI.INT,    dest, TAG_TO_WORK);
                        MPI.COMM_WORLD.Send(subA,              0, rows * n, MPI.DOUBLE, dest, TAG_TO_WORK);
                        MPI.COMM_WORLD.Send(matB,              0, n * n,    MPI.DOUBLE, dest, TAG_TO_WORK);

                        offset += rows;
                    }

                    // ── Збираємо результати від кожного worker-а ─────────
                    for (int src = 1; src <= numWorkers; src++) {
                        int[] recvOffset = new int[1];
                        int[] recvRows   = new int[1];

                        MPI.COMM_WORLD.Recv(recvOffset, 0, 1, MPI.INT, src, TAG_TO_MAST);
                        MPI.COMM_WORLD.Recv(recvRows,   0, 1, MPI.INT, src, TAG_TO_MAST);

                        int rows = recvRows[0];
                        int off = recvOffset[0];
                        double[] subC = new double[rows * n];

                        MPI.COMM_WORLD.Recv(subC, 0, rows * n, MPI.DOUBLE, src, TAG_TO_MAST);
                        System.arraycopy(subC, 0, matC, off * n, rows * n);
                    }

                    totalTime += (System.currentTimeMillis() - start) / 1000.0;
                }

                System.out.printf("Size: %dx%d  Avg time: %.3f s%n",
                        n, n, totalTime / iterations);
            }

        } else {
            // ── Worker: обробляємо sizes.length * iterations раундів ──────
            for (int round = 0; round < sizes.length * iterations; round++) {
                int n = sizes[round / iterations];

                int[] recvOffset = new int[1];
                int[] recvRows = new int[1];

                MPI.COMM_WORLD.Recv(recvOffset, 0, 1, MPI.INT, MASTER, TAG_TO_WORK);
                MPI.COMM_WORLD.Recv(recvRows,   0, 1, MPI.INT, MASTER, TAG_TO_WORK);

                int offset = recvOffset[0];
                int rows = recvRows[0];

                double[] subA = new double[rows * n];
                double[] matB = new double[n * n];

                MPI.COMM_WORLD.Recv(subA, 0, rows * n, MPI.DOUBLE, MASTER, TAG_TO_WORK);
                MPI.COMM_WORLD.Recv(matB, 0, n * n,    MPI.DOUBLE, MASTER, TAG_TO_WORK);

                // ── Множення: subA (rows×n) × matB (n×n) = subC (rows×n) ─
                double[] subC = new double[rows * n];
                for (int i = 0; i < rows; i++) {
                    for (int k = 0; k < n; k++) {
                        double sum = 0.0;
                        for (int j = 0; j < n; j++) {
                            sum += subA[i * n + j] * matB[j * n + k];
                        }
                        subC[i * n + k] = sum;
                    }
                }

                MPI.COMM_WORLD.Send(new int[]{offset}, 0, 1,        MPI.INT,    MASTER, TAG_TO_MAST);
                MPI.COMM_WORLD.Send(new int[]{rows},   0, 1,        MPI.INT,    MASTER, TAG_TO_MAST);
                MPI.COMM_WORLD.Send(subC,              0, rows * n, MPI.DOUBLE, MASTER, TAG_TO_MAST);
            }
        }
        MPI.Finalize();
    }

    static double[] randomMatrix(int size) {
        Random rng = new Random();
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = -100 + 200 * rng.nextDouble();
        }
        return data;
    }
}