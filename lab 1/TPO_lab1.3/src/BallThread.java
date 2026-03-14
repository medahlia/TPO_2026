import java.awt.*;

public class BallThread extends Thread {
    private Ball ball;

    public BallThread(Ball ball) {
        this.ball = ball;
        if (ball.getColor() == Color.RED) {
            this.setPriority(Thread.MAX_PRIORITY); // пріоритет 10
        } else {
            this.setPriority(Thread.MIN_PRIORITY); // пріоритет 1
        }
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i < 10000; i++) {

                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                ball.move();
                Thread.sleep(5);
            }
        } catch (InterruptedException ex) {

        }
    }
}
