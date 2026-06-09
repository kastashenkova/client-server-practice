package practice2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.practice1.Encrypter;
import org.example.practice1.Message;
import org.example.practice1.MessageCipher;
import org.example.practice2.Scaling;
import org.example.practice2.SharedQueue;
import org.example.practice2.sender.Sender;
import org.example.practice2.sender.SenderImpl;
import org.example.practice2.warehouse.WarehouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultithreadMessageSendingTest {
    private Scaling scaling;

    @BeforeEach
    void setUp() {
        scaling = new Scaling(2, 2, 4, 3, 5);
        scaling.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        scaling.stop();
    }

    @Test
    void concurrentAddStock_shouldNotLoseUpdates() throws Exception {
        WarehouseService warehouse = scaling.getWarehouseService();
        int threadCount = 20;
        int addPerThread = 10;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                warehouse.addProducts("rice", addPerThread);
                latch.countDown();
            });
        }

        pool.shutdown();

        pool.awaitTermination(5, TimeUnit.SECONDS);
        latch.await(5, TimeUnit.SECONDS);

        assertEquals(threadCount * addPerThread, warehouse.getStock("rice"));
    }

    @Test
    void concurrentDeductStock_shouldNotGoNegative() throws Exception {
        WarehouseService warehouse = scaling.getWarehouseService();
        warehouse.addProducts("buckwheat", 50);

        int threadCount = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                warehouse.deductProducts("buckwheat", 5);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(warehouse.getStock("buckwheat") >= 0,
                "Stock must never be negative");
    }

    @Test
    void fullPipeline_concurrentMessages_processedWithoutErrors() throws Exception {
        Encrypter encrypter = new Encrypter(new MessageCipher());
        SharedQueue<byte[]> rawQueue = scaling.getRawQueue();

        int threadCount = 10;
        int messagesPerThread = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            futures.add(pool.submit(() -> {
                for (int m = 0; m < messagesPerThread; m++) {
                    Message msg = new Message(
                            (byte) 0x01, System.nanoTime(),
                            3,
                            1,
                            "pasta:10");
                    try {
                        rawQueue.produce(encrypter.encrypt(msg));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        WarehouseService warehouse = scaling.getWarehouseService();

        int expected = threadCount * messagesPerThread * 10; // 500
        int current = 0;
        int maxRetries = 100; // 5 sec

        while (maxRetries > 0) {
            current = warehouse.getStock("pasta");
            if (current == expected) {
                break;
            }
            Thread.sleep(50);
            maxRetries--;
        }
        assertEquals(expected, warehouse.getStock("pasta"),
                "All ADD_PRODUCTS commands must be processed exactly once");
    }

    // race condition test
    @Test
    void concurrentAddAndDeduct_stockRemainsConsistent() throws Exception {
        WarehouseService warehouse = scaling.getWarehouseService();
        warehouse.addProducts("oats", 1000);

        int threadCount = 50;
        AtomicInteger netChange = new AtomicInteger(0);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            boolean add = (i % 2 == 0);
            pool.submit(() -> {
                if (add) {
                    warehouse.addProducts("oats", 5);
                    netChange.addAndGet(5);
                } else {
                    warehouse.deductProducts("oats", 3);
                    netChange.addAndGet(-3);
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        int expected = Math.max(0, 1000 + netChange.get());
        assertEquals(expected, warehouse.getStock("oats"));
    }

    @Test
    void sender_shouldConsumeAndProcessMessages() throws Exception {
        SharedQueue<byte[]> sendQueue = new SharedQueue<>();
        Sender sender = new SenderImpl(sendQueue);

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        Thread senderThread = new Thread(sender);

        try {
            senderThread.start();

            sendQueue.produce(new byte[]{1, 2, 3}); // 3 bytes
            sendQueue.produce(new byte[]{4, 5, 6, 7, 8}); // 5 bytes

            int maxRetries = 40;
            while (maxRetries > 0) {
                String consoleOutput = outContent.toString();
                if (consoleOutput.contains("[SEND] 3 bytes") && consoleOutput.contains("[SEND] 5 bytes")) {
                    break;
                }
                Thread.sleep(50);
                maxRetries--;
            }

            String consoleOutput = outContent.toString();
            assertTrue(consoleOutput.contains("[SEND] 3 bytes"));
            assertTrue(consoleOutput.contains("[SEND] 5 bytes"));

        } finally {
            System.setOut(originalOut);
            sender.stop();
            senderThread.interrupt();
            senderThread.join(1000);
        }
    }
}
