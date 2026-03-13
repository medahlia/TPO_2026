import java.awt.*;
import java.awt.geom.Ellipse2D;

public class Ball {
    private Component canvas;
    private static final int XSIZE = 20;
    private static final int YSIZE = 20;
    private int x;
    private int y;
    private int dx = 2;
    private int dy = 2;
    private Color color;

    public Ball(Component c, Color color) {
        this.canvas = c;
        if (color.equals(Color.BLUE)) {
            this.color = new Color(0, 0, 255, 25); // майже прозорий синій // 50% прозорість
        } else {
            this.color = color; // інші кольори без змін
        }
        this.x = 100;
        this.y = 100;
    }

    public void draw(Graphics2D g2) {
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(x, y, XSIZE, YSIZE));
    }

    public void move() {
        x += dx;
        y += dy;
        if (x < 0 || x + XSIZE >= this.canvas.getWidth()) dx = -dx;
        if (y < 0 || y + YSIZE >= this.canvas.getHeight()) dy = -dy;
        this.canvas.repaint();
    }

    public Color getColor() {
        return color;
    }
}
