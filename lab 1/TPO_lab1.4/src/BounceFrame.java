import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BounceFrame extends JFrame {
    private BallCanvas canvas;
    public static final int WIDTH = 450;
    public static final int HEIGHT = 350;

    public BounceFrame() {
        this.setSize(WIDTH, HEIGHT);
        this.setTitle("Bounce program with pocket");
        this.canvas = new BallCanvas();

        Container content = this.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(this.canvas, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.LIGHT_GRAY);
        JButton buttonStart = new JButton("Start with join");
        JButton buttonStop = new JButton("Stop");

        buttonStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Color[] colors = {
                        Color.RED,
                        Color.BLUE,
                        Color.GREEN,
                        Color.YELLOW,
                        Color.ORANGE,
                        Color.PINK,
                        Color.CYAN,
                        Color.MAGENTA
                };

                BallThread previousThread = null;

                // створення 8 куль
                for (int i = 0; i < colors.length; i++) {
                    Ball ball = new Ball(canvas, colors[i]);
                    canvas.add(ball);

                    // створення нового потоку, який чекатиме на завершення попереднього
                    BallThread thread = new BallThread(ball, previousThread);
                    thread.start();

                    previousThread = thread;
                }
            }
        });

        buttonStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        buttonPanel.add(buttonStart);
        buttonPanel.add(buttonStop);
        content.add(buttonPanel, BorderLayout.SOUTH);
    }
}