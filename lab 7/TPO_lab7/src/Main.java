import mpi.*;
import java.util.Random;

public class Main {

    static final int MASTER = 0;

    // =========================================================
    // ── Точка входу ──────────────────────────────────────────
    // =========================================================
    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int rank     = MPI.COMM_WORLD.Rank();
        int numProcs = MPI.COMM_WORLD.Size();

        if (numProcs < 2) {
            if (rank == MASTER) System.out.println("Need at least 2 processes");
            MPI.COMM_WORLD.Abort(1);
            MPI.Finalize();
            return;
        }

        int[] sizes    = {500, 1000, 1500, 2000};
        int iterations = 10;

        if (rank == MASTER) {
            System.out.printf("Processes: %d%n%n", numProcs);
            System.out.printf("%-8s | %-14s | %-14s | %-14s | %-14s%n",
                    "Size", "Sequential", "OneToAll", "AllToOne", "AllToAll");
            //System.out.println("-".repeat(74));
        }

        for (int n : sizes) {
            // Матриці генеруються у всіх процесах однаково (seed фіксований)
            double[] matA = randomMatrix(n * n, n);
            double[] matB = randomMatrix(n * n, n + 1);

            double seqTime = 0, oneToAllTime = 0, allToOneTime = 0, allToAllTime = 0;

            for (int iter = 0; iter < iterations; iter++) {

                // ── Sequential (тільки master) ────────────────────────
                if (rank == MASTER) {
                    long t = System.currentTimeMillis();
                    multiplySequential(matA, matB, n);
                    seqTime += System.currentTimeMillis() - t;
                }

                // ── One-To-All: Scatterv + Bcast → Gatherv ────────────
                MPI.COMM_WORLD.Barrier();
                {
                    long t = System.currentTimeMillis();
                    multiplyOneToAll(matA, matB, n, rank, numProcs);
                    if (rank == MASTER) oneToAllTime += System.currentTimeMillis() - t;
                }

                // ── All-To-One: Scatterv + Bcast → Reduce ─────────────
                MPI.COMM_WORLD.Barrier();
                {
                    long t = System.currentTimeMillis();
                    multiplyAllToOne(matA, matB, n, rank, numProcs);
                    if (rank == MASTER) allToOneTime += System.currentTimeMillis() - t;
                }

                // ── All-To-All: Scatterv + Allgatherv ─────────────────
                MPI.COMM_WORLD.Barrier();
                {
                    long t = System.currentTimeMillis();
                    multiplyAllToAll(matA, matB, n, rank, numProcs);
                    if (rank == MASTER) allToAllTime += System.currentTimeMillis() - t;
                }
            }

            if (rank == MASTER) {
                System.out.printf("%-8d | %-14.1f | %-14.1f | %-14.1f | %-14.1f%n",
                        n,
                        seqTime      / iterations,
                        oneToAllTime / iterations,
                        allToOneTime / iterations,
                        allToAllTime / iterations);
            }
        }

        MPI.Finalize();
    }

    // =========================================================
    // ── Sequential ───────────────────────────────────────────
    // =========================================================
    static double[] multiplySequential(double[] A, double[] B, int n) {
        double[] C = new double[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += A[i * n + k] * B[k * n + j];
                }
                C[i * n + j] = sum;
            }
        }
        return C;
    }

    // =========================================================
    // ── One-To-All: Scatterv + Bcast → Gatherv ───────────────
    // Один процес (master) розподіляє дані → всі отримують
    // Один процес (master) збирає результати ← від усіх
    // =========================================================
    static double[] multiplyOneToAll(
            double[] matA, double[] matB, int n,
            int rank, int numProcs) throws Exception {

        // ── Розподіл рядків між процесами ─────────────────────
        int[] rowCounts = rowCounts(n, numProcs);
        int[] offsets   = offsets(rowCounts);

        // sendcounts і displs у елементах double (не рядках)
        int[] sendCounts = new int[numProcs];
        int[] displs     = new int[numProcs];
        for (int i = 0; i < numProcs; i++) {
            sendCounts[i] = rowCounts[i] * n;
            displs[i]     = offsets[i]   * n;
        }

        // ── Scatterv: master розсилає шматки A кожному процесу ─
        int localRows = rowCounts[rank];
        double[] localA = new double[localRows * n];

        MPI.COMM_WORLD.Scatterv(
                matA, 0, sendCounts, displs, MPI.DOUBLE,
                localA, 0, localRows * n,    MPI.DOUBLE,
                MASTER
        );

        // ── Bcast: master надсилає повну матрицю B всім ─────────
        // (One-To-All: один відправник, всі отримувачі)
        MPI.COMM_WORLD.Bcast(matB, 0, n * n, MPI.DOUBLE, MASTER);

        // ── Локальне множення ────────────────────────────────────
        double[] localC = multiplyBlock(localA, matB, localRows, n);

        // ── Gatherv: master збирає результати від усіх ──────────
        // (All-To-One за напрямком: всі надсилають → один отримує)
        int[] recvCounts = new int[numProcs];
        for (int i = 0; i < numProcs; i++) {
            recvCounts[i] = rowCounts[i] * n;
        }

        double[] resultC = (rank == MASTER) ? new double[n * n] : null;

        MPI.COMM_WORLD.Gatherv(
                localC, 0, localRows * n, MPI.DOUBLE,
                resultC, 0, recvCounts, displs, MPI.DOUBLE,
                MASTER
        );

        return resultC;
    }

    // =========================================================
    // ── All-To-One: Scatterv + Bcast → Reduce ────────────────
    // Відрізняється від OneToAll способом збирання результату:
    // замість Gatherv використовується Reduce(SUM) —
    // кожен процес надсилає повну матрицю C з нулями
    // на позиціях "чужих" рядків, Reduce підсумовує їх.
    // Це справжній All-To-One: всі процеси беруть участь
    // у формуванні єдиного результату через редукцію.
    // =========================================================
    static double[] multiplyAllToOne(
            double[] matA, double[] matB, int n,
            int rank, int numProcs) throws Exception {

        int[] rowCounts = rowCounts(n, numProcs);
        int[] offsets   = offsets(rowCounts);

        int[] sendCounts = new int[numProcs];
        int[] displs     = new int[numProcs];
        for (int i = 0; i < numProcs; i++) {
            sendCounts[i] = rowCounts[i] * n;
            displs[i]     = offsets[i]   * n;
        }

        int localRows = rowCounts[rank];
        double[] localA = new double[localRows * n];

        MPI.COMM_WORLD.Scatterv(
                matA, 0, sendCounts, displs, MPI.DOUBLE,
                localA, 0, localRows * n,    MPI.DOUBLE,
                MASTER
        );

        MPI.COMM_WORLD.Bcast(matB, 0, n * n, MPI.DOUBLE, MASTER);

        // ── Локальне множення: заповнюємо тільки "свої" рядки ───
        // Решта залишаються нулями — Reduce(SUM) їх проігнорує
        double[] localC = new double[n * n];
        double[] block  = multiplyBlock(localA, matB, localRows, n);
        System.arraycopy(block, 0, localC, offsets[rank] * n, localRows * n);

        // ── Reduce(SUM): всі надсилають свій localC → master ────
        // (All-To-One: всі процеси беруть участь, один отримує)
        double[] resultC = new double[n * n];

        MPI.COMM_WORLD.Reduce(
                localC, 0,
                resultC, 0,
                n * n,
                MPI.DOUBLE,
                MPI.SUM,
                MASTER
        );

        return resultC;
    }

    // =========================================================
    // ── All-To-All: Scatterv + Allgatherv ────────────────────
    // Відрізняється від OneToAll способом збирання результату:
    // замість Gatherv (результат тільки у master)
    // використовується Allgatherv — КОЖЕН процес отримує
    // повну результуючу матрицю C.
    // Це справжній All-To-All: всі надсилають і всі отримують.
    // =========================================================
    static double[] multiplyAllToAll(
            double[] matA, double[] matB, int n,
            int rank, int numProcs) throws Exception {

        int[] rowCounts = rowCounts(n, numProcs);
        int[] offsets   = offsets(rowCounts);

        int[] sendCounts = new int[numProcs];
        int[] displs     = new int[numProcs];
        for (int i = 0; i < numProcs; i++) {
            sendCounts[i] = rowCounts[i] * n;
            displs[i]     = offsets[i]   * n;
        }

        int localRows = rowCounts[rank];
        double[] localA = new double[localRows * n];

        MPI.COMM_WORLD.Scatterv(
                matA, 0, sendCounts, displs, MPI.DOUBLE,
                localA, 0, localRows * n,    MPI.DOUBLE,
                MASTER
        );

        MPI.COMM_WORLD.Bcast(matB, 0, n * n, MPI.DOUBLE, MASTER);

        double[] localC = multiplyBlock(localA, matB, localRows, n);

        // ── Allgatherv: кожен надсилає свій localC,
        //    кожен отримує повну матрицю C ──────────────────────
        // (All-To-All: всі беруть участь, всі отримують результат)
        int[] recvCounts = new int[numProcs];
        for (int i = 0; i < numProcs; i++) {
            recvCounts[i] = rowCounts[i] * n;
        }

        double[] resultC = new double[n * n]; // є у КОЖНОГО процесу

        MPI.COMM_WORLD.Allgatherv(
                localC,  0, localRows * n, MPI.DOUBLE,
                resultC, 0, recvCounts, displs, MPI.DOUBLE
        );

        return resultC;
    }

    // =========================================================
    // ── Допоміжні методи ─────────────────────────────────────
    // =========================================================

    // Множення блоку: localA (rows×n) × B (n×n) = localC (rows×n)
    static double[] multiplyBlock(double[] localA, double[] B, int rows, int n) {
        double[] localC = new double[rows * n];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += localA[i * n + k] * B[k * n + j];
                }
                localC[i * n + j] = sum;
            }
        }
        return localC;
    }

    // Кількість рядків для кожного процесу (рівномірний поділ)
    static int[] rowCounts(int n, int numProcs) {
        int base  = n / numProcs;
        int extra = n % numProcs;
        int[] counts = new int[numProcs];
        for (int i = 0; i < numProcs; i++) {
            counts[i] = (i < extra) ? base + 1 : base;
        }
        return counts;
    }

    // Зміщення (prefix sum від rowCounts)
    static int[] offsets(int[] rowCounts) {
        int[] offsets = new int[rowCounts.length];
        for (int i = 1; i < rowCounts.length; i++) {
            offsets[i] = offsets[i - 1] + rowCounts[i - 1];
        }
        return offsets;
    }

    // Генерація матриці з фіксованим seed для відтворюваності
    static double[] randomMatrix(int size, long seed) {
        Random rng    = new Random(seed);
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            data[i] = -100 + 200 * rng.nextDouble();
        }
        return data;
    }
}