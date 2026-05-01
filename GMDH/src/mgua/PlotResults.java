package mgua;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * PlotResults — JavaFX-додаток для відображення результатів МГУА.
 *
 * Читає файл RESULTS_MPI.txt (створений GMDHParallel після MPI-запуску)
 * і малює два графіки:
 *   1. Реальні vs модельні значення y (лінійний графік).
 *   2. Scatter plot (реальні по X, модельні по Y) — ідеальна модель = діагональ.
 *
 * Запуск:
 *   - Після того як ./run_mpi.sh завершився і RESULTS_MPI.txt створено,
 *     запусти PlotResults напряму з IntelliJ (звичайний Run).
 *   - Або через термінал (потрібен JavaFX у classpath):
 *       java --module-path /Users/dasha/javafx-sdk-21.0.11/lib \
 *            --add-modules javafx.controls \
 *            -cp out mgua.PlotResults
 */
public class PlotResults extends Application {

    // Шлях до файлу результатів (відносно кореня проєкту)
    private static final String RESULTS_FILE = "RESULTS_MPI.txt";

    // -------------------------------------------------------------------------
    // Точка входу
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        // --- Читаємо дані з файлу ---
        ParsedResults data = parseResultsFile(RESULTS_FILE);

        if (data.real.isEmpty()) {
            showError(stage, "Файл " + RESULTS_FILE + " не містить секції --- POINTS ---.\n"
                    + "Спочатку запусти: ./run_mpi.sh");
            return;
        }

        int n = data.real.size();

        // =====================================================================
        // Графік 1 — Лінійний: реальні vs модельні по індексу спостереження
        // =====================================================================
        NumberAxis xAxis1 = new NumberAxis(0, n, Math.max(1, n / 10));
        xAxis1.setLabel("Спостереження (індекс)");
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel(data.yName + " (нормалізовано)");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis1, yAxis1);
        lineChart.setTitle("МГУА: реальні vs модельні значення");
        lineChart.setCreateSymbols(false);   // без кружечків — швидше при n=2000
        lineChart.setAnimated(false);

        XYChart.Series<Number, Number> realSeries  = new XYChart.Series<>();
        XYChart.Series<Number, Number> modelSeries = new XYChart.Series<>();
        realSeries.setName("Реальні (" + data.yName + ")");
        modelSeries.setName("Модель МГУА");

        for (int i = 0; i < n; i++) {
            realSeries.getData().add(new XYChart.Data<>(i, data.real.get(i)));
            modelSeries.getData().add(new XYChart.Data<>(i, data.model.get(i)));
        }
        lineChart.getData().addAll(realSeries, modelSeries);
        lineChart.setPrefHeight(350);

        // =====================================================================
        // Графік 2 — Scatter: реальні (X) vs модельні (Y)
        //   Ідеальна модель = точки лежать на діагоналі y=x
        // =====================================================================
        NumberAxis xAxis2 = new NumberAxis();
        xAxis2.setLabel("Реальні значення");
        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Модельні значення");

        ScatterChart<Number, Number> scatter = new ScatterChart<>(xAxis2, yAxis2);
        scatter.setTitle("Scatter: реальні vs модельні (ідеал — діагональ)");
        scatter.setAnimated(false);

        XYChart.Series<Number, Number> scatterSeries = new XYChart.Series<>();
        scatterSeries.setName("Спостереження");
        for (int i = 0; i < n; i++) {
            scatterSeries.getData().add(
                    new XYChart.Data<>(data.real.get(i), data.model.get(i)));
        }

        // Діагональ y=x для орієнтиру
        XYChart.Series<Number, Number> diagSeries = new XYChart.Series<>();
        diagSeries.setName("Ідеальна модель (y=x)");
        double minVal = data.real.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxVal = data.real.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        diagSeries.getData().add(new XYChart.Data<>(minVal, minVal));
        diagSeries.getData().add(new XYChart.Data<>(maxVal, maxVal));

        scatter.getData().addAll(scatterSeries, diagSeries);
        scatter.setPrefHeight(350);

        // =====================================================================
        // Мітка з формулою та критерієм
        // =====================================================================
        Label infoLabel = new Label(data.formula + "\n" + data.criterion);
        infoLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; "
                + "-fx-padding: 8px; -fx-background-color: #f4f4f4; "
                + "-fx-border-color: #cccccc; -fx-border-radius: 4;");

        // =====================================================================
        // Компонування
        // =====================================================================
        VBox root = new VBox(10, infoLabel, lineChart, scatter);
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 900, 820);
        stage.setTitle("Результати МГУА — " + data.yName);
        stage.setScene(scene);
        stage.show();
    }

    // -------------------------------------------------------------------------
    // Парсинг RESULTS_MPI.txt
    // -------------------------------------------------------------------------

    private static class ParsedResults {
        String yName    = "y";
        String formula  = "";
        String criterion = "";
        List<Double> real  = new ArrayList<>();
        List<Double> model = new ArrayList<>();
    }

    private static ParsedResults parseResultsFile(String path) {
        ParsedResults r = new ParsedResults();
        boolean inPoints = false;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Секція POINTS починається після "--- POINTS ---"
                if (line.equals("--- POINTS ---")) {
                    inPoints = true;
                    continue;
                }

                if (inPoints) {
                    if (line.startsWith("Y_NAME ")) {
                        r.yName = line.substring(7).trim();
                    } else if (line.startsWith("POINT ")) {
                        // POINT <idx> <real> <model>
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 4) {
                            r.real.add(Double.parseDouble(parts[2]));
                            r.model.add(Double.parseDouble(parts[3]));
                        }
                    }
                } else {
                    // Збираємо формулу та критерій з верхньої частини файлу
                    if (line.startsWith("f(x)") || line.startsWith(" f(x)")) {
                        r.formula = line.trim();
                    }
                    if (line.startsWith("Criterion") || line.startsWith(" Criterion")) {
                        r.criterion = line.trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PlotResults] Не вдалось прочитати " + path + ": " + e.getMessage());
        }
        return r;
    }

    // -------------------------------------------------------------------------
    // Помилка — показати у вікні
    // -------------------------------------------------------------------------

    private static void showError(Stage stage, String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: red; -fx-padding: 20px;");
        Scene scene = new Scene(new VBox(lbl), 500, 120);
        stage.setTitle("PlotResults — Помилка");
        stage.setScene(scene);
        stage.show();
    }
}