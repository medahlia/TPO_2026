package mgua;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class PlotResults extends Application {

    private static final String FILE = "RESULTS_MPI.txt";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {

        Data d = read(FILE);

        if (d.x.isEmpty()) {
            System.out.println("Немає даних для графіка");
            return;
        }

        // =========================
        // 📏 МАСШТАБ (щоб не було зжаття)
        // =========================
        double minX = d.x.stream().mapToDouble(v -> v).min().orElse(0);
        double maxX = d.x.stream().mapToDouble(v -> v).max().orElse(1);

        double minY = Math.min(
                d.real.stream().mapToDouble(v -> v).min().orElse(0),
                d.model.stream().mapToDouble(v -> v).min().orElse(0)
        );

        double maxY = Math.max(
                d.real.stream().mapToDouble(v -> v).max().orElse(1),
                d.model.stream().mapToDouble(v -> v).max().orElse(1)
        );

        double padX = (maxX - minX) * 0.1;
        double padY = (maxY - minY) * 0.1;

        NumberAxis xAxis = new NumberAxis(minX - padX, maxX + padX, (maxX - minX) / 10);
        NumberAxis yAxis = new NumberAxis(minY - padY, maxY + padY, (maxY - minY) / 10);

        xAxis.setLabel("X");
        yAxis.setLabel("Y");

        // =========================
        // 📈 ГРАФІК
        // =========================
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Model");

        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(true);

        // --- Серії ---
        XYChart.Series<Number, Number> real = new XYChart.Series<>();
        real.setName("Input XY");

        XYChart.Series<Number, Number> model = new XYChart.Series<>();
        model.setName("Model XY");

        for (int i = 0; i < d.x.size(); i++) {
            real.getData().add(new XYChart.Data<>(d.x.get(i), d.real.get(i)));
            model.getData().add(new XYChart.Data<>(d.x.get(i), d.model.get(i)));
        }

        chart.getData().addAll(real, model);

        // =========================
        // 🎨 СТИЛЬ (гарний вигляд)
        // =========================
        chart.setPrefSize(1100, 700);

        Scene scene = new Scene(chart, 1100, 700);

        stage.setTitle("Model");
        stage.setScene(scene);
        stage.show();

        // фон
        chart.lookup(".chart-plot-background")
                .setStyle("-fx-background-color: #f5f5f5;");

        // товсті лінії
        real.getNode().setStyle("-fx-stroke-width: 3px;");
        model.getNode().setStyle("-fx-stroke-width: 3px;");
    }

    // =========================
    // 📦 DATA
    // =========================
    static class Data {
        List<Double> x = new ArrayList<>();
        List<Double> real = new ArrayList<>();
        List<Double> model = new ArrayList<>();
    }

    // =========================
    // 📂 FILE PARSER
    // =========================
    private static Data read(String path) {

        Data d = new Data();
        boolean points = false;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            String line;

            while ((line = br.readLine()) != null) {

                line = line.trim();

                if (line.equals("--- POINTS ---")) {
                    points = true;
                    continue;
                }

                if (points && line.startsWith("POINT")) {

                    // FORMAT:
                    // POINT idx X real model
                    String[] p = line.split("\\s+");

                    double x = Double.parseDouble(p[2]);
                    double real = Double.parseDouble(p[3]);
                    double model = Double.parseDouble(p[4]);

                    d.x.add(x);
                    d.real.add(real);
                    d.model.add(model);
                }
            }

        } catch (Exception e) {
            System.out.println("Помилка читання: " + e.getMessage());
        }

        return d;
    }
}