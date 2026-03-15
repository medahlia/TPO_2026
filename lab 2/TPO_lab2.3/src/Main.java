public class Main {

    public static void main(String[] args) throws InterruptedException {

        int weeks = 4;

        for (int week = 1; week <= weeks; week++) {

            System.out.println("\nWeek " + week);

            GradeBook gradeBook = new GradeBook();

            Thread lecturer = new Thread(
                    new TeacherThread(gradeBook, "lecturer", true, -1)
            );

            Thread assistant1 = new Thread(
                    new TeacherThread(gradeBook, "assistant 1", false, 0)
            );

            Thread assistant2 = new Thread(
                    new TeacherThread(gradeBook, "assistant 2", false, 1)
            );

            Thread assistant3 = new Thread(
                    new TeacherThread(gradeBook, "assistant 3", false, 2)
            );

            lecturer.start();
            assistant1.start();
            assistant2.start();
            assistant3.start();

            lecturer.join();
            assistant1.join();
            assistant2.join();
            assistant3.join();

            //gradeBook.printGrades();
        }
    }
}