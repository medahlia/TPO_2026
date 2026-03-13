import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;

public class Ball {
    private Component canvas;
    private static final int XSIZE = 20;
    private static final int YSIZE = 20;
    private int x = 0;
    private int y = 0;
    private int dx = 2;
    private int dy = 2;
    private Color color;
    private boolean isFinished = false;

    public Ball(Component c, Color color) {
        this.canvas = c;
        this.color = color;

        int perimeterSide = new Random().nextInt(4); // 0=top, 1=bottom, 2=left, 3=right

        switch (perimeterSide) {
            case 0:
                x = new Random().nextInt(canvas.getWidth() - XSIZE);
                y = 0;
                break;
            case 1:
                x = new Random().nextInt(canvas.getWidth() - XSIZE);
                y = canvas.getHeight() - YSIZE;
                break;
            case 2:
                x = 0;
                y = new Random().nextInt(canvas.getHeight() - YSIZE);
                break;
            case 3:
                x = canvas.getWidth() - XSIZE;
                y = new Random().nextInt(canvas.getHeight() - YSIZE);
                break;
        }
    }

    public void draw(Graphics2D g2) {
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(x, y, XSIZE, YSIZE));
    }

    public void move() {
        x += dx;
        y += dy;

        // зіткнення зі стінками
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

        // перевірка потрапляння в лузу

        int pocketX = canvas.getWidth() / 2;
        int pocketY = canvas.getHeight() / 2;
        int pocketRadius = 30;

        int ballCenterX = x + XSIZE/2;
        int ballCenterY = y + YSIZE/2;

        // відстань від центру кульки до центру лузи
        double distance = Math.sqrt(
                Math.pow(ballCenterX - pocketX, 2) +
                        Math.pow(ballCenterY - pocketY, 2)
        );

        // якщо куля потрапила в лузу
        if (distance < pocketRadius) {
            isFinished = true;
        }

        this.canvas.repaint();
    }

    // фіксує, чи потрапила кулька в лузу
    public boolean isFinished() {
        return isFinished;
    }

    public String getColorName() {
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.YELLOW)) return "Yellow";
        if (color.equals(Color.ORANGE)) return "Orange";
        if (color.equals(Color.PINK)) return "Pink";
        if (color.equals(Color.CYAN)) return "Cyan";
        if (color.equals(Color.MAGENTA)) return "Magenta";
        return "Unknown";
    }
}