import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GradeBook {
    private ConcurrentHashMap<String, Integer>[] groups;
    private Lock[] groupLocks;
    private Random random = new Random();

    public GradeBook() {
        groups = new ConcurrentHashMap[3];
        groupLocks = new ReentrantLock[3];

        for (int i = 0; i < 3; i++) {
            groups[i] = new ConcurrentHashMap<>();
            groupLocks[i] = new ReentrantLock();
        }
    }

    public void addGrade(int group, int student, String teacher) {
        String studentCode = (group + 1) + "-" + String.format("%02d", student + 1);

        groupLocks[group].lock();
        try {
            // ReentrantLock
            // записуємо оцінку лише, якщо ще не існує
            if (!groups[group].containsKey(studentCode)) {
                int grade = random.nextInt(101);
                groups[group].put(studentCode, grade);

                System.out.println(
                        "student " + studentCode +
                                " | " + String.format("%03d", grade) +
                                " | " + teacher
                );
            }
        } finally {
            groupLocks[group].unlock();
        }
    }

    public void printGrades() {
        System.out.println("\n=== Підсумок журналу ===");
        for (int i = 0; i < groups.length; i++) {
            System.out.println("Група " + (i + 1) + ":");
            groups[i].entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(e -> System.out.println("  Студент " + e.getKey() + " -> " + e.getValue()));
        }
    }
}