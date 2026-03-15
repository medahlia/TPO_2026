import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GradeBookTest {
    private GradeBook gradeBook;

    // Random із фіксованим seed, результати завжди однакові
    @BeforeEach
    void setUp() {
        gradeBook = new GradeBook(new Random(42));
    }

    @Test
    void concurrency_eachStudentGradedExactlyOnce() throws InterruptedException {
        // лектор і асистент одночасно намагаються виставити оцінку студенту 0 групи 0
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            GradeBook book = new GradeBook(new Random());

            Thread t1 = new Thread(() -> book.addGrade(0, 0, "lecturer"));
            Thread t2 = new Thread(() -> book.addGrade(0, 0, "assistant"));

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            assertEquals(1, book.getGroupSize(0),
                    "Після двох паралельних записів група повинна мати рівно 1 студента");
        }
    }

    // перевіряє, що всі 30 студентів отримали оцінку і жоден студент не пропущений.
    @Test
    void concurrency_allStudentsGradedByLecturerAndAssistants() throws InterruptedException {
        Thread lecturer   = new Thread(new TeacherThread(gradeBook, "lecturer",    true,  -1));
        Thread assistant1 = new Thread(new TeacherThread(gradeBook, "assistant 1", false,  0));
        Thread assistant2 = new Thread(new TeacherThread(gradeBook, "assistant 2", false,  1));
        Thread assistant3 = new Thread(new TeacherThread(gradeBook, "assistant 3", false,  2));

        lecturer.start();
        assistant1.start();
        assistant2.start();
        assistant3.start();

        lecturer.join();
        assistant1.join();
        assistant2.join();
        assistant3.join();

        // кожна група повинна мати 30 студентів
        assertEquals(30, gradeBook.getGroupSize(0), "Група 1 повинна мати 30 студентів");
        assertEquals(30, gradeBook.getGroupSize(1), "Група 2 повинна мати 30 студентів");
        assertEquals(30, gradeBook.getGroupSize(2), "Група 3 повинна мати 30 студентів");
    }


    @Test
    void concurrency_noGradeIsNull_afterFullRun() throws InterruptedException {
        Thread lecturer   = new Thread(new TeacherThread(gradeBook, "lecturer",    true,  -1));
        Thread assistant1 = new Thread(new TeacherThread(gradeBook, "assistant 1", false,  0));
        Thread assistant2 = new Thread(new TeacherThread(gradeBook, "assistant 2", false,  1));
        Thread assistant3 = new Thread(new TeacherThread(gradeBook, "assistant 3", false,  2));

        lecturer.start();
        assistant1.start();
        assistant2.start();
        assistant3.start();

        lecturer.join();
        assistant1.join();
        assistant2.join();
        assistant3.join();

        // жоден студент не повинен мати null-оцінку
        for (int g = 0; g < 3; g++) {
            for (int s = 0; s < 30; s++) {
                assertNotNull(gradeBook.getGrade(g, s),
                        "Студент " + s + " групи " + g + " не має оцінки");
            }
        }
    }

    @Test
    void concurrency_manyThreadsCompeteForSameGroup() throws InterruptedException {
        int threadCount = 10;
        List<Thread> threads = new ArrayList<>();

        // 10 потоків одночасно пишуть в групу 0
        for (int i = 0; i < threadCount; i++) {
            final int t = i;
            threads.add(new Thread(() -> {
                for (int s = 0; s < 30; s++) {
                    gradeBook.addGrade(0, s, "teacher-" + t);
                }
            }));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        assertEquals(30, gradeBook.getGroupSize(0),
                "При конкурентному записі 10 потоків група повинна містити рівно 30 унікальних студентів");
    }
}