package org.example.practice2.receiver;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.example.practice1.Encrypter;
import org.example.practice1.Message;
import org.example.practice1.MessageCipher;
import org.example.practice2.SharedQueue;
import org.example.practice2.warehouse.CommandType;

public class ReceiverImpl implements Receiver {
    private static final String[] PRODUCTS = {"rice", "buckwheat", "pasta", "beans", "oats"};
    private static final String[] GROUPS = {"cereals", "legumes", "grains"};

    private final SharedQueue<byte[]> outputQueue;
    private final Encrypter encrypter;
    private final Random random = new Random();
    private final AtomicLong counter = new AtomicLong(0);
    private volatile boolean active = true;

    public ReceiverImpl(SharedQueue<byte[]> outputQueue) {
        this.outputQueue = outputQueue;
        this.encrypter = new Encrypter(new MessageCipher());
    }

    @Override
    public void receiveMessage() {
        CommandType type = randomType();
        String payload   = buildPayload(type);

        Message msg = new Message(
                (byte) 0x01,
                counter.incrementAndGet(),
                type.getCode(),
                random.nextInt(100),
                payload
        );

        try {
            outputQueue.produce(encrypter.encrypt(msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private CommandType randomType() {
        CommandType[] types = CommandType.values();
        return types[random.nextInt(types.length)];
    }

    private String buildPayload(CommandType type) {
        String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
        String group = GROUPS[random.nextInt(GROUPS.length)];
        int quantity = random.nextInt(50) + 1;
        double price = Math.round((random.nextDouble() * 100 + 1) * 100.0) / 100.0;

        return switch (type) {
            case GET_PRODUCT_QUANTITY -> product;
            case DEDUCT_PRODUCTS, ADD_PRODUCTS -> product + ":" + quantity;
            case ADD_GROUP -> group;
            case ADD_PRODUCT_NAME_TO_GROUP -> group + ":" + product;
            case SET_PRODUCT_PRICE -> product + ":" + price;
            case SEARCH_PRODUCTS -> "name:" + product;
            case DELETE_PRODUCT -> String.valueOf(random.nextInt(20) + 1);
        };
    }

    @Override
    public void run() {
        while (active && !Thread.currentThread().isInterrupted()) {
            receiveMessage();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void stop() {
        active = false;
    }
}

