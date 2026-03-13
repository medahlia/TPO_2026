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

    private JTextField redInput;
    private JTextField blueInput;

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

        JButton addRedButton = new JButton("Додати 🔴");
        JButton addBlueButton = new JButton("Додати 🔵");
        JButton startButton = new JButton("Запуск");
        JButton clearButton = new JButton("Очистити");

        // відображення кількості кульок
        //countLabel = new JLabel(getBallCountText());
        //buttonPanel.add(countLabel);

        // текстові поля для введення кількості кульок
        redInput = new JTextField("1", 5);
        blueInput = new JTextField("10", 5);

        buttonPanel.add(new JLabel("🔴 Червоні:"));
        buttonPanel.add(redInput);

        addRedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Ball redBall = new Ball(canvas, Color.RED);
                canvas.add(redBall);
                redCount++;
            }
        });

        buttonPanel.add(new JLabel("🔵 Сині:"));
        buttonPanel.add(blueInput);

        addBlueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < 10; i++) {
                    Ball blueBall = new Ball(canvas, Color.BLUE);
                    canvas.add(blueBall);
                    blueCount++;
                }
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                int redBalls;
                int blueBalls;

                try {
                    redBalls = Integer.parseInt(redInput.getText());
                    blueBalls = Integer.parseInt(blueInput.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Введіть числа!");
                    return;
                }

                // створення червоних кульок
                for (int i = 0; i < redBalls; i++) {
                    Ball redBall = new Ball(canvas, Color.RED);
                    canvas.add(redBall);
                    canvas.repaint();
                    redCount++;

                    BallThread thread = new BallThread(redBall);
                    thread.start();
                    ballThreads.add(thread);
                }

                // створення синіх кульок
                for (int i = 0; i < blueBalls; i++) {
                    Ball blueBall = new Ball(canvas, Color.BLUE);
                    canvas.add(blueBall);
                    canvas.repaint();
                    blueCount++;

                    BallThread thread = new BallThread(blueBall);
                    thread.start();
                    ballThreads.add(thread);
                }
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // зупиняємо потоки
                for (BallThread thread : ballThreads) {
                    thread.interrupt();
                }

                ballThreads.clear();

                // очищаємо canvas
                canvas.clearBalls();

                redCount = 0;
                blueCount = 0;
            }
        });

        buttonPanel.add(addRedButton);
        buttonPanel.add(addBlueButton);
        buttonPanel.add(startButton);
        buttonPanel.add(clearButton);

        content.add(buttonPanel, BorderLayout.SOUTH);
    }
}
