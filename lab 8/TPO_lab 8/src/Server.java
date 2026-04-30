import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.*;

public class Server {

    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private final ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println();
        System.out.println("+--------+--------------+--------------+---------------+-------------+");
        System.out.println("| Mode   | Size         | Receive (ms) | Compute (ms)  | Send (ms)   |");
        System.out.println("+--------+--------------+--------------+---------------+-------------+");
    }

    // ── Головний цикл: кожен клієнт обробляється в окремому потоці ────────
    public void start() throws IOException {
        while (true) {
            Socket client = serverSocket.accept();
            Thread tread = new Thread(new ClientHandler(client));
            tread.start();
        }
    }

    // ── Обробник одного клієнта ────────────────────────────────────────────
    private class ClientHandler implements Runnable {

        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                // ── Читаємо заголовок запиту ───────────────────────────
                // mode=1: матриці генерує сервер
                // mode=2: матриці надсилає клієнт
                int mode = in.readInt();
                int n = in.readInt(); // рядки A
                int m = in.readInt(); // стовпці A = рядки B
                int p = in.readInt(); // стовпці B

                long recvStart = System.nanoTime();
                int[][] matA;
                int[][] matB;

                if (mode == 1) {
                    // Дані на сервері — генеруємо самі
                    matA = generateMatrix(n, m);
                    matB = generateMatrix(m, p);
                } else {
                    // Дані на клієнті — отримуємо по мережі
                    matA = receiveMatrix(in, n, m);
                    matB = receiveMatrix(in, m, p);
                }
                long recvTime = System.nanoTime() - recvStart;

                // ── Паралельне множення ────────────────────────────────
                long computeStart = System.nanoTime();
                int[][] matC = parallelMultiply(matA, matB);
                long computeTime = System.nanoTime() - computeStart;

                // ── Відправляємо результат ─────────────────────────────
                long sendStart = System.nanoTime();
                sendMatrix(matC, out);
                long sendTime = System.nanoTime() - sendStart;

                System.out.printf(
                        "| %-6d | %-12s | %-12.1f | %-13.1f | %-11.1f |%n",
                        mode,
                        n + "x" + m + "x" + p,
                        recvTime    / 1e6,
                        computeTime / 1e6,
                        sendTime    / 1e6
                );

                in.close();
                out.close();
                socket.close();

            } catch (IOException e) {
                System.err.println("Помилка клієнта: " + e.getMessage());
            }
        }
    }

    // ── Генерація матриці n×m з випадковими числами [0,9] ─────────────────
    private int[][] generateMatrix(int n, int m) {
        Random rng = new Random();
        int[][] mat = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mat[i][j] = rng.nextInt(10);
            }
        }
        return mat;
    }

    // ── Читання матриці n×m з потоку (int за int) ──────────────────────────
    private int[][] receiveMatrix(DataInputStream in, int n, int m) throws IOException {
        int[][] mat = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mat[i][j] = in.readInt();
            }
        }
        return mat;
    }

    // ── Відправка матриці через ByteBuffer (ефективніше ніж writeInt по одному) ─
    private void sendMatrix(int[][] mat, DataOutputStream out) throws IOException {
        int rows = mat.length;
        int cols = mat[0].length;
        ByteBuffer buf = ByteBuffer.allocate(rows * cols * 4);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                buf.putInt(mat[i][j]);
            }
        }
        out.write(buf.array());
        out.flush();
    }

    // ── Паралельне множення: кожен рядок C — окрема задача пулу ───────────
    // Алгоритм: рядковий Stripe
    // C[i][j] = sum( A[i][k] * B[k][j] ) для k = 0..m-1
    private int[][] parallelMultiply(int[][] matA, int[][] matB)
            throws IOException {
        int rows = matA.length;
        int cols = matB[0].length;
        int inner = matB.length;
        int[][] matC = new int[rows][cols];

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < rows; i++) {
            final int row = i;
            pool.submit(new RowTask(matA, matB, matC, row, cols, inner));
        }

        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return matC;
    }

    // ── Задача для обчислення одного рядка результуючої матриці ───────────
    private static class RowTask implements Runnable {
        private final int[][] matA;
        private final int[][] matB;
        private final int[][] matC;
        private final int row;
        private final int cols;
        private final int inner;

        RowTask(int[][] matA, int[][] matB, int[][] matC, int row, int cols, int inner) {
            this.matA = matA;
            this.matB = matB;
            this.matC = matC;
            this.row = row;
            this.cols = cols;
            this.inner = inner;
        }

        public void run() {
            for (int j = 0; j < cols; j++) {
                int sum = 0;
                for (int k = 0; k < inner; k++) {
                    sum += matA[row][k] * matB[k][j];
                }
                matC[row][j] = sum;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Використання: java MatrixServer <port>");
            return;
        }
        new Server(Integer.parseInt(args[0])).start();
    }
}