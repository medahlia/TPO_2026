public class TeacherThread  implements Runnable {
    private GradeBook gradeBook;
    private String name;
    private boolean lecturer;
    private int group;

    public TeacherThread(GradeBook gradeBook, String name, boolean lecturer, int group) {
        this.gradeBook = gradeBook;
        this.name = name;
        this.lecturer = lecturer;
        this.group = group;
    }

    @Override
    public void run() {
        for (int week = 1; week <= 5; week++) {
            if (lecturer) {
                for (int g = 0; g < 3; g++) {
                    for (int student = 0; student < 5; student++) {
                        gradeBook.addGrade(g, student, name);

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                for (int student = 0; student < 5; student++) {
                    gradeBook.addGrade(group, student, name);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
