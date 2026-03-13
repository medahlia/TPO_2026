import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class BounceFrame extends JFrame {
    private BallCanvas canvas;
    private int redCount = 0;
    private int blueCount = 0;
    private JLabel countLabel;
    private ArrayList<BallThread> ballThreads = new ArrayList<>();

    public static final int WIDTH = 1000;
    public static final int HEIGHT = 350;

    public BounceFrame() {
        this.setSize(WIDTH, HEIGHT);
        this.setTitle("Ball Priority Experiment");
        this.canvas = new BallCanvas();
        Container content = this.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(this.canvas, BorderLayout.CENTER);

        // кнопки
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.LIGHT_GRAY);

        JButton addRedButton = new JButton("Додати червону кульку");
        JButton addBlueButton = new JButton("Додати сині кульки (10)");
        JButton startButton = new JButton("Запуск");

        // відображення кількості кульок
        countLabel = new JLabel(getBallCountText());
        buttonPanel.add(countLabel);

        addRedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Ball redBall = new Ball(canvas, Color.RED);
                canvas.add(redBall);
                redCount++;
                updateBallCount();
            }
        });

        addBlueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < 10; i++) {
                    Ball blueBall = new Ball(canvas, Color.BLUE);
                    canvas.add(blueBall);
                    blueCount++;
                }
                updateBallCount();
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Запуск всіх кульок
                for (Ball b : canvas.getBalls()) {
                    BallThread thread = new BallThread(b);
                    thread.start();
                    ballThreads.add(thread);
                }
            }
        });

        buttonPanel.add(addRedButton);
        buttonPanel.add(addBlueButton);
        buttonPanel.add(startButton);

        content.add(buttonPanel, BorderLayout.SOUTH);
    }

    private String getBallCountText() {
        return "🔴 Червоні: " + redCount + " | 🔵 Сині: " + blueCount;
    }

    private void updateBallCount() {
        countLabel.setText(getBallCountText());
    }
}
