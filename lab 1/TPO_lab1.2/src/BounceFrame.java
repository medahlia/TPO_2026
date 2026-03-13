import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BounceFrame extends JFrame {

    private BallCanvas canvas;
    private JTextField scoreField;
    private int score = 0;

    public static final int WIDTH = 450;
    public static final int HEIGHT = 350;

    public BounceFrame() {
        this.setSize(WIDTH, HEIGHT);
        this.setTitle("Bounce program");
        this.canvas = new BallCanvas(this);
        System.out.println("In Frame Thread name = " + Thread.currentThread().getName());

        Container content = this.getContentPane();
        content.setLayout(new BorderLayout());

        content.add(this.canvas, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.LIGHT_GRAY);

        JButton buttonStart = new JButton("Start");
        JButton buttonStop = new JButton("Stop");

        scoreField = new JTextField("Score: 0", 10);
        scoreField.setEditable(false);

        buttonStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Ball b = new Ball(canvas, BounceFrame.this);
                canvas.add(b);
                BallThread thread = new BallThread(b);
                thread.start();
                System.out.println("Thread name = " + thread.getName());
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
        buttonPanel.add(scoreField);

        content.add(buttonPanel, BorderLayout.SOUTH);
    }

    public synchronized void updateScore() {
        score++;
        scoreField.setText("Score: " + score);
    }
}
