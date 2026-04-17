import mpi.*;
import java.util.Random;

public class NonBlocking {

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
        int[] sizes = {500, 1000, 1500, 2000};
        int iterations = 10;

        if (rank == MASTER) {
            System.out.printf("Non-Blocking MPI — %d processes%n%n", numProcs);

            for (int n : sizes) {
                double totalTime = 0.0;

                for (int iter = 0; iter < iterations; iter++) {
                    double[] matA = randomMatrix(n * n);
                    double[] matB = randomMatrix(n * n);
                    double[] matC = new double[n * n];

                    int base = n / numWorkers;
                    int extra = n % numWorkers;
                    int offset = 0;

                    // Зберігаємо subA окремо — буфер Isend має жити до Waitall
                    double[][] subAs = new double[numWorkers][];

                    long start = System.currentTimeMillis();

                    // ── Небасуюча розсилка: 4 повідомлення на кожен worker ─
                    Request[] sendReqs = new Request[numWorkers * 4];

                    for (int dest = 1; dest <= numWorkers; dest++) {
                        int rows = (dest <= extra) ? base + 1 : base;
                        int idx  = (dest - 1) * 4;

                        int[] offArr = new int[]{offset};
                        int[] rowArr = new int[]{rows};
                        double[] subA = new double[rows * n];
                        System.arraycopy(matA, offset * n, subA, 0, rows * n);
                        subAs[dest - 1] = subA; // утримуємо буфер живим

                        sendReqs[idx]     = MPI.COMM_WORLD.Isend(offArr, 0, 1,        MPI.INT,    dest, TAG_TO_WORK);
                        sendReqs[idx + 1] = MPI.COMM_WORLD.Isend(rowArr, 0, 1,        MPI.INT,    dest, TAG_TO_WORK);
                        sendReqs[idx + 2] = MPI.COMM_WORLD.Isend(subA,   0, rows * n, MPI.DOUBLE, dest, TAG_TO_WORK);
                        sendReqs[idx + 3] = MPI.COMM_WORLD.Isend(matB,   0, n * n,    MPI.DOUBLE, dest, TAG_TO_WORK);

                        offset += rows;
                    }

                    Request.Waitall(sendReqs); // чекаємо завершення всіх відправлень

                    // ── Небасуючий прийом offset і rows від усіх worker-ів ─
                    int[][] recvOffsets = new int[numWorkers][1];
                    int[][] recvRows = new int[numWorkers][1];
                    Request[] metaReqs = new Request[numWorkers * 2];

                    for (int src = 1; src <= numWorkers; src++) {
                        int idx = (src - 1) * 2;
                        recvOffsets[src - 1] = new int[1];
                        recvRows[src - 1] = new int[1];
                        metaReqs[idx]     = MPI.COMM_WORLD.Irecv(recvOffsets[src - 1], 0, 1, MPI.INT, src, TAG_TO_MAST);
                        metaReqs[idx + 1] = MPI.COMM_WORLD.Irecv(recvRows[src - 1],    0, 1, MPI.INT, src, TAG_TO_MAST);
                    }

                    Request.Waitall(metaReqs); // чекаємо всі метадані

                    // ── Приймаємо субматриці C (розмір відомий після метаданих) ─
                    for (int src = 1; src <= numWorkers; src++) {
                        int off = recvOffsets[src - 1][0];
                        int rows = recvRows[src - 1][0];
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

                // ── Небасуючий прийом метаданих ───────────────────────────
                int[] recvOffset = new int[1];
                int[] recvRows = new int[1];
                Request[] metaReqs = new Request[2];
                metaReqs[0] = MPI.COMM_WORLD.Irecv(recvOffset, 0, 1, MPI.INT, MASTER, TAG_TO_WORK);
                metaReqs[1] = MPI.COMM_WORLD.Irecv(recvRows,   0, 1, MPI.INT, MASTER, TAG_TO_WORK);
                Request.Waitall(metaReqs);

                int offset = recvOffset[0];
                int rows = recvRows[0];

                // ── Небасуючий прийом даних ───────────────────────────────
                double[] subA = new double[rows * n];
                double[] matB = new double[n * n];
                Request[] dataReqs = new Request[2];
                dataReqs[0] = MPI.COMM_WORLD.Irecv(subA, 0, rows * n, MPI.DOUBLE, MASTER, TAG_TO_WORK);
                dataReqs[1] = MPI.COMM_WORLD.Irecv(matB, 0, n * n,    MPI.DOUBLE, MASTER, TAG_TO_WORK);
                Request.Waitall(dataReqs);

                // ── Множення ─────────────────────────────────────────────
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

                // ── Небасуюча відправка результатів ──────────────────────
                Request[] sendReqs = new Request[3];
                sendReqs[0] = MPI.COMM_WORLD.Isend(new int[]{offset}, 0, 1,        MPI.INT,    MASTER, TAG_TO_MAST);
                sendReqs[1] = MPI.COMM_WORLD.Isend(new int[]{rows},   0, 1,        MPI.INT,    MASTER, TAG_TO_MAST);
                sendReqs[2] = MPI.COMM_WORLD.Isend(subC,              0, rows * n, MPI.DOUBLE, MASTER, TAG_TO_MAST);
                Request.Waitall(sendReqs);
            }
        }

        MPI.Finalize();
    }

    static double[] randomMatrix(int size) {
        Random rng    = new Random();
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = -100 + 200 * rng.nextDouble();
        }
        return data;
    }
}