import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;

public class Ball {
    private Component canvas;
    private BounceFrame frame;
    private static final int XSIZE = 20;
    private static final int YSIZE = 20;
    private int x;
    private int y;
    private int dx = 2;
    private int dy = 2;
    private boolean alive = true;

    public Ball(Component c, BounceFrame frame) {
        this.canvas = c;
        this.frame = frame;

        if (Math.random() < 0.5) {
            x = new Random().nextInt(this.canvas.getWidth() - XSIZE);
            y = 0;
        } else {
            x = 0;
            y = new Random().nextInt(this.canvas.getHeight() - YSIZE);
        }
    }

    public void draw(Graphics2D g2) {
        if (alive) {
            g2.setColor(Color.darkGray);
            g2.fill(new Ellipse2D.Double(x, y, XSIZE, YSIZE));
        }
    }

    public void move() {
        if (!alive) return;

        x += dx;
        y += dy;

        if (x < 0) {
            x = 0;
            dx = -dx;
        }
        if (x + XSIZE >= this.canvas.getWidth()) {
            x = this.canvas.getWidth() - XSIZE;
            dx = -dx;
        }
        if (y < 0) {
            y = 0;
            dy = -dy;
        }
        if (y + YSIZE >= this.canvas.getHeight()) {
            y = this.canvas.getHeight() - YSIZE;
            dy = -dy;
        }

        // перевірка попадання у лузу
        BallCanvas ballCanvas = (BallCanvas) canvas;
        int ballCenterX = x + XSIZE / 2;
        int ballCenterY = y + YSIZE / 2;
        if (ballCanvas.isBallInHole(ballCenterX, ballCenterY, XSIZE)) { // isBallInHole перевіряє, чи потрапила кулька у лузу
            frame.updateScore();
            alive = false;
        }

        this.canvas.repaint();
    }

    public boolean isAlive() {
        return alive;
    }
}
