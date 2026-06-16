package org.example.practice3.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import org.example.practice1.Decrypter;
import org.example.practice1.Message;
import org.example.practice1.MessageCipher;
import org.example.practice2.SharedQueue;
import org.example.practice2.receiver.ReceiverImpl;
import org.example.practice3.TestScenario;

public class StoreClientUDP {
    private static final String HOST = "localhost";
    private static final int PORT = 8081;
    private static final int TIMEOUT = 3000;
    private static final int RETRIES = 5;

    private static volatile boolean ackReceived = false;

    public static void main(String[] args) {
        SharedQueue<byte[]> queue = new SharedQueue<>();

         ReceiverImpl receiver = new ReceiverImpl(queue);
         new Thread(receiver).start();

//        TestScenario testProducer = new TestScenario(queue);
//        new Thread(testProducer).start();

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress address = InetAddress.getByName(HOST);

            Decrypter decrypter = new Decrypter(new MessageCipher());

            new Thread(() -> {
                byte[] buffer = new byte[65507];
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());

                        if ("ACK_UDP".equals(message)) {
                            ackReceived = true;
                        } else {
                            byte[] responseData = new byte[packet.getLength()];
                            System.arraycopy(packet.getData(), 0, responseData, 0, packet.getLength());
                            Message responseMsg = decrypter.decrypt(responseData);
                            System.out.println("UDP Server response: " + responseMsg.messageString());
                        }
                    } catch (SocketTimeoutException _) {
                    } catch (Exception e) {
                        break;
                    }
                }
            }).start();

            while (true) {
                byte[] data = queue.consume();
                if (data == null) continue;

                ackReceived = false;
                boolean delivered = false;
                int retries = 0;

                while (!delivered && retries < RETRIES) {
                    DatagramPacket packet = new DatagramPacket(data, data.length, address, PORT);
                    socket.send(packet);

                    long startTime = System.currentTimeMillis();
                    while (System.currentTimeMillis() - startTime < TIMEOUT) {
                        if (ackReceived) {
                            delivered = true;
                            break;
                        }
                        Thread.sleep(50);
                    }

                    if (!delivered) {
                        retries++;
                        System.out.println("Retry " + retries);
                    }
                }

                if (delivered) {
                    System.out.println("UDP package delivered.");
                } else {
                    System.out.println("UDP package dropped after " + RETRIES + " retries.");
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while processing UDP packet", e);
        }
    }
}
