public class Drop {
    // Message sent from producer
    // to consumer.
    private int number;
    // True if consumer should wait
    // for producer to send message,

    // false if producer should wait for
    // consumer to retrieve message.
    private boolean empty = true;

    public synchronized int take() {
        // Wait until message is
        // available.
        while (empty) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        // Toggle status.
        empty = true;
        // Notify producer that
        // status has changed.
        notifyAll();
        return number;
    }

    public synchronized void put(int number) {
        // Wait until message has
        // been retrieved. (отримано)
        while (!empty) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        // Toggle status.
        empty = false;
        // Store message.
        this.number = number;
        // Notify consumer that status
        // has changed.
        notifyAll();
    }
}