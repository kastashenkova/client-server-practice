package org.example.practice3;

import org.example.practice1.Encrypter;
import org.example.practice1.Message;
import org.example.practice1.MessageCipher;
import org.example.practice2.SharedQueue;

public class TestScenario implements Runnable {
    private final SharedQueue<byte[]> clientQueue;
    private long messageCounter = 1;

    private final Encrypter encrypter = new Encrypter(new MessageCipher());

    public TestScenario(SharedQueue<byte[]> clientQueue) {
        this.clientQueue = clientQueue;
    }

    @Override
    public void run() {
        try {
            sendMessage(4, "Electronics"); // ADD_GROUP
            Thread.sleep(1000);

            sendMessage(5, "Electronics:Laptop"); // ADD_PRODUCT_NAME_TO_GROUP
            Thread.sleep(1000);

            sendMessage(3, "Laptop:50"); // ADD_PRODUCTS
            Thread.sleep(1000);

            sendMessage(6, "Laptop:1500.00"); // SET_PRODUCT_PRICE
            Thread.sleep(1000);

            sendMessage(1, "Laptop"); // GET_PRODUCT_QUANTITY
            Thread.sleep(1000);

            sendMessage(2, "Laptop:10"); // DEDUCT_PRODUCTS
            Thread.sleep(1000);

            sendMessage(7, "name:Laptop,limit:10"); // SEARCH_PRODUCTS

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(int commandId, String payload) throws InterruptedException {
        byte uniqueId = 1;
        int userId = 1001;

        Message msg = new Message(
                uniqueId,
                messageCounter++,
                commandId,
                userId,
                payload
        );

        byte[] encryptedData = encrypter.encrypt(msg);
        System.out.println("Sending command: " + commandId + ", data: " + payload);
        clientQueue.produce(encryptedData);
    }
}
