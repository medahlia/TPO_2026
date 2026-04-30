import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class Client {

    private static final int NUM_RUNS = 10;
    private static final int[] SIZES = {100, 500, 1000, 1500, 2000};

    private final String host;
    private final int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ── Один запит: відправити розміри (і матриці якщо mode=2),
    //    отримати результат. Повертає масив часів [send, recv, total] ────────
    public long[] sendRequest(int mode, int n, int m, int p) throws IOException {

        Socket socket = new Socket(host, port);
        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        int[][] matA = null;
        int[][] matB = null;
        if (mode == 2) {
            matA = generateMatrix(n, m);
            matB = generateMatrix(m, p);
        }

        // ── Відправляємо заголовок (і матриці якщо mode=2) ────────────
        long totalStart = System.nanoTime();

        out.writeInt(mode);
        out.writeInt(n);
        out.writeInt(m);
        out.writeInt(p);
        out.flush();

        if (mode == 2) {
            sendMatrix(matA, out);
            sendMatrix(matB, out);
        }

        long sendTime = System.nanoTime() - totalStart;

        // ── Отримуємо результат ────────────────────────────────────────
        long recvStart = System.nanoTime();
        receiveMatrix(in, n, p);
        long recvTime = System.nanoTime() - recvStart;

        long totalTime = System.nanoTime() - totalStart;

        in.close();
        out.close();
        socket.close();

        return new long[]{sendTime, recvTime, totalTime};
    }

    // ── Генерація матриці n×m з числами [0,9] ─────────────────────────────
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

    // ── Відправка матриці через ByteBuffer ────────────────────────────────
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

    // ── Читання результуючої матриці n×m ──────────────────────────────────
    private int[][] receiveMatrix(DataInputStream in, int n, int m) throws IOException {
        int[][] mat = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                mat[i][j] = in.readInt();
            }
        }
        return mat;
    }

    // ── Запуск тестування обох режимів для всіх розмірів ──────────────────
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Використання: java MatrixClient <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        Client client = new Client(host, port);

        for (int mode = 1; mode <= 2; mode++) {
            System.out.println();
            if (mode == 1) {
                System.out.println("Mode 1 — matrices generated on SERVER");
            } else {
                System.out.println("Mode 2 — matrices generated on CLIENT");
            }
            System.out.println("+----------+----------------+----------------+----------------+");
            System.out.println("| Size     | Send (ms)      | Receive (ms)   | Total (ms)     |");
            System.out.println("+----------+----------------+----------------+----------------+");

            for (int size : SIZES) {
                long totalSend = 0;
                long totalRecv = 0;
                long totalAll = 0;

                for (int run = 0; run < NUM_RUNS; run++) {
                    long[] times = client.sendRequest(mode, size, size, size);
                    totalSend += times[0];
                    totalRecv += times[1];
                    totalAll += times[2];
                }

                System.out.printf(
                        "| %-8d | %-14.1f | %-14.1f | %-14.1f |%n",
                        size,
                        totalSend / (double) NUM_RUNS / 1e6,
                        totalRecv / (double) NUM_RUNS / 1e6,
                        totalAll  / (double) NUM_RUNS / 1e6
                );
            }
            System.out.println("+----------+----------------+----------------+----------------+");
        }
    }
}