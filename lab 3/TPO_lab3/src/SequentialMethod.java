public class SequentialMethod {

    public static Matrix multiply(Matrix a, Matrix b) {

        if (a.getCols() != b.getRows()) {
            throw new IllegalArgumentException("Matrices cannot be multiplied");
        }

        int rows = a.getRows();
        int cols = b.getCols();
        int common = a.getCols();

        Matrix result = new Matrix(rows, cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int sum = 0;
                for (int k = 0; k < common; k++) {
                    sum += a.getValue(i, k) * b.getValue(k, j);
                }
                result.setValue(i, j, sum);
            }
        }

        return result;
    }
}
