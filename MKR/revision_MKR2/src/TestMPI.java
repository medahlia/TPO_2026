import mpi.*;

public class TestMPI {
    public static void main(String[] args) throws Exception {

        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();

        System.out.println("Hello from process " + rank);

        MPI.Finalize();
    }
}