public class TeacherThread implements Runnable {

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
        if (lecturer) {
            for (int g = 0; g < 3; g++) {
                for (int s = 0; s < 30; s++) {
                    gradeBook.addGrade(g, s, name);
                }
            }
        } else {
            for (int s = 0; s < 30; s++) {
                gradeBook.addGrade(group, s, name);
            }
        }
    }
}