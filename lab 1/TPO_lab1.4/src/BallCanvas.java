import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

public class BallCanvas extends JPanel {
    private ArrayList<Ball> balls = new ArrayList<>();
    private static final int POCKET_RADIUS = 30;

    public void add(Ball b) {
        this.balls.add(b);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.DARK_GRAY);
        int pocketX = getWidth() / 2 - POCKET_RADIUS;
        int pocketY = getHeight() / 2 - POCKET_RADIUS;
        g2.fill(new Ellipse2D.Double(pocketX, pocketY,
                POCKET_RADIUS * 2, POCKET_RADIUS * 2));


        for (int i = 0; i < balls.size(); i++) {
            Ball b = balls.get(i);
            b.draw(g2);
        }
    }
}