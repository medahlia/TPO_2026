import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class BallCanvas extends JPanel {
    private ArrayList<Ball> balls = new ArrayList<>();
    private BounceFrame frame;

    // луза
    private static final int HOLE_SIZE = 40;
    private static final Color HOLE_COLOR = Color.BLUE;

    public BallCanvas(BounceFrame frame) {
        this.frame = frame;
    }

    public void add(Ball b) {
        this.balls.add(b);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // лузи
        g2.setColor(HOLE_COLOR);
        g2.fillOval(0, 0, HOLE_SIZE, HOLE_SIZE);
        g2.fillOval(getWidth() - HOLE_SIZE, 0, HOLE_SIZE, HOLE_SIZE);
        g2.fillOval(0, getHeight() - HOLE_SIZE, HOLE_SIZE, HOLE_SIZE);
        g2.fillOval(getWidth() - HOLE_SIZE, getHeight() - HOLE_SIZE, HOLE_SIZE, HOLE_SIZE);

        // кульки
        for (Ball b : balls) {
            b.draw(g2);
        }
    }

    // перевіряє, чи кульки потрапили в лузу
    public boolean isBallInHole(int x, int y, int size) {
        int radius = size / 2;

        return (distance(x, y, 0 + radius, 0 + radius) <= HOLE_SIZE / 2) ||
                (distance(x, y, getWidth() - radius, 0 + radius) <= HOLE_SIZE / 2) ||
                (distance(x, y, 0 + radius, getHeight() - radius) <= HOLE_SIZE / 2) ||
                (distance(x, y, getWidth() - radius, getHeight() - radius) <= HOLE_SIZE / 2);
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }
}
