package mgua;

import java.io.Serializable;

/**
 * Holds the evaluation result of a single GMDH model candidate.
 * Must be Serializable so it can be sent via MPI object messages.
 *
 * Layout of flat double[] for MPI_Send (avoids Java serialization overhead):
 *   [0]            = model index in global models array
 *   [1]            = criterionLS   (least squares)
 *   [2]            = criterionReg  (regularity)
 *   [3]            = criterionUnb  (unbiasedness)
 *   [4 .. 4+q]     = model bitmask (int cast)
 */
public class ModelResult implements Serializable, Comparable<ModelResult> {

    private static final long serialVersionUID = 1L;

    public final int   modelIndex;
    public final int[] model;
    public final double criterionLS;
    public final double criterionReg;
    public final double criterionUnb;

    public ModelResult(int modelIndex, int[] model,
                       double criterionLS, double criterionReg, double criterionUnb) {
        this.modelIndex  = modelIndex;
        this.model       = model;
        this.criterionLS  = criterionLS;
        this.criterionReg = criterionReg;
        this.criterionUnb = criterionUnb;
    }

    /** Convert to flat double[] for low-level MPI_Send. */
    public double[] toFlat() {
        double[] flat = new double[4 + model.length];
        flat[0] = modelIndex;
        flat[1] = criterionLS;
        flat[2] = criterionReg;
        flat[3] = criterionUnb;
        for (int i = 0; i < model.length; i++)
            flat[4 + i] = model[i];
        return flat;
    }

    /** Reconstruct from flat double[] produced by toFlat(). */
    public static ModelResult fromFlat(double[] flat) {
        int modelIndex = (int) flat[0];
        double ls  = flat[1];
        double reg = flat[2];
        double unb = flat[3];
        int[] model = new int[flat.length - 4];
        for (int i = 0; i < model.length; i++)
            model[i] = (int) flat[4 + i];
        return new ModelResult(modelIndex, model, ls, reg, unb);
    }

    /** Natural ordering by unbiasedness criterion (ascending). */
    @Override
    public int compareTo(ModelResult other) {
        return Double.compare(this.criterionUnb, other.criterionUnb);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Model[");
        for (int v : model) sb.append(v);
        sb.append(String.format("] LS=%.6f Reg=%.6f Unb=%.6f", criterionLS, criterionReg, criterionUnb));
        return sb.toString();
    }
}