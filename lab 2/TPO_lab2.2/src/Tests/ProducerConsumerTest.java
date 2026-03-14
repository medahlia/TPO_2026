import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

public class ProducerConsumerTest {

    // всі числа отримані в правильному порядку
    @Test
    void testAllMessagesDeliveredInOrder() throws InterruptedException {
        Drop drop = new Drop();
        int size = 100;
        List<Integer> received = new ArrayList<>();

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= size; i++)
                drop.put(i);
            drop.put(0);
        });

        Thread consumer = new Thread(() -> {
            for (int msg = drop.take(); msg != 0; msg = drop.take())
                received.add(msg);
        });

        producer.start();
        consumer.start();
        producer.join(5000);
        consumer.join(5000);

        assertEquals(size, received.size(), "кількість отриманих повідомлень");
        for (int i = 0; i < size; i++)
            assertEquals(i + 1, received.get(i), "порядок числа на позиції " + i);
    }

    // тест на завершення: Consumer має зупинитись після sentinel 0
    @Test
    void testConsumerTerminatesOnZero() throws InterruptedException {
        Drop drop = new Drop();
        CountDownLatch done = new CountDownLatch(1);

        Thread consumer = new Thread(() -> {
            new Consumer(drop).run();
            done.countDown(); // лише якщо Consumer завершився
        });

        consumer.start();
        new Thread(new Producer(drop, 5)).start();

        boolean finished = done.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "Consumer має завершитись протягом 5 секунд");
    }

    // тест на deadlock
    @RepeatedTest(10)
    void testNoDeadlock() throws InterruptedException {
        Drop drop = new Drop();
        Thread p = new Thread(new Producer(drop, 50));
        Thread c = new Thread(new Consumer(drop));

        p.start();
        c.start();

        p.join(30000);
        c.join(30000);

        assertFalse(p.isAlive(), "Producer завис — можливий deadlock");
        assertFalse(c.isAlive(), "Consumer завис — можливий deadlock");
    }

    // тест при різних швидкостях: повільний Producer, швидкий Consumer
    @Test
    void testSlowProducerFastConsumer() throws InterruptedException {
        Drop drop = new Drop();
        List<Integer> received = new CopyOnWriteArrayList<>();

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                drop.put(i);
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
            drop.put(0);
        });

        Thread consumer = new Thread(() -> {
            for (int msg = drop.take(); msg != 0; msg = drop.take())
                received.add(msg);
        });

        producer.start();
        consumer.start();
        producer.join(5000);
        consumer.join(5000);

        assertEquals(20, received.size());
    }
}