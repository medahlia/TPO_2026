import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GradeBook {

    private ConcurrentHashMap<String, Integer>[] groups;
    private Random random = new Random();

    public GradeBook() {

        groups = new ConcurrentHashMap[3];

        for (int i = 0; i < 3; i++) {
            groups[i] = new ConcurrentHashMap<>();
        }
    }

    public void addGrade(int group, int student, String teacher) {

        String studentCode = (group + 1) + "-" + String.format("%02d", student + 1);

        int grade = random.nextInt(101);

        Integer previous = groups[group].putIfAbsent(studentCode, grade);

        if (previous == null) {

            String formattedGrade = String.format("%03d", grade);

            System.out.println(
                    "student " + studentCode +
                            " | " + formattedGrade +
                            " | " + teacher
            );
        }
    }
}