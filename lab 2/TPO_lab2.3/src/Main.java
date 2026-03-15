//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        GradeBook gradeBook = new GradeBook(3, 5);

        Thread lecturer = new Thread(
                new TeacherThread(gradeBook, "Lecturer", true, -1)
        );

        Thread assistant1 = new Thread(
                new TeacherThread(gradeBook, "Assistant 1", false, 0)
        );

        Thread assistant2 = new Thread(
                new TeacherThread(gradeBook, "Assistant 2", false, 1)
        );

        Thread assistant3 = new Thread(
                new TeacherThread(gradeBook, "Assistant 3", false, 2)
        );

        lecturer.start();
        assistant1.start();
        assistant2.start();
        assistant3.start();

    }
}