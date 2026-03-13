public class BallThread extends Thread {
    private Ball b;
    private BallThread threadToJoin; // попередній потік

    public BallThread(Ball ball) {
        this.b = ball;
        this.threadToJoin = null;
    }

    public BallThread(Ball ball, BallThread threadToJoin) {
        this.b = ball;
        this.threadToJoin = threadToJoin;
    }

    @Override
    public void run() {
        try {
            // якщо є потік для join, чекаємо на його завершення
            if (threadToJoin != null) {
                threadToJoin.join();
            }

            while (!b.isFinished()) {
                b.move();
                Thread.sleep(5);
            }

            System.out.println(
                    "Thread name = " + Thread.currentThread().getName() +
                            " | Color = " + b.getColorName()
            );

        } catch (InterruptedException ex) {
            //
        }
    }
}