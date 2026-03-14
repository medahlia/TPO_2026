import java.util.Random;

public class Producer implements Runnable {
    private Drop drop;

    int size;

    public Producer(Drop drop, int size) {
        this.drop = drop;
        this.size = size;
    }

    public void run() {
        var importantInfo = new int[size];

        Random rand = new Random();
        // генерація значень
        for (int i = 0; i < importantInfo.length; i++) {
            // int randomInt = rand.nextInt(MAX - MIN + 1) + MIN;
            importantInfo[i] = i + 1; //randomInt
        }

        Random random = new Random();
        for (int i = 0; i < importantInfo.length; i++) {
            drop.put(importantInfo[i]);
            try {
                Thread.sleep(random.nextInt(500));
            } catch (InterruptedException e) {}
        }
        drop.put(0);
    }
}