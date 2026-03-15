import java.util.Random;


public class GradeBook {
    private int[][] groups;
    private Random random = new Random();

    public GradeBook(int groupsCount, int studentsPerGroup) {
        groups = new int[groupsCount][studentsPerGroup];
    }

    public synchronized void addGrade(int group, int student, String teacher) {

        int grade = random.nextInt(101);
        groups[group][student] = grade;

        System.out.println(
                teacher + " поставив " + grade + "балів " +
                        " студенту " + student +
                        " групи " + group
        );
    }
}
