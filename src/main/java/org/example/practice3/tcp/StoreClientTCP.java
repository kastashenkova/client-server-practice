package org.example.practice3.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.example.practice1.Decrypter;
import org.example.practice1.Message;
import org.example.practice1.MessageCipher;
import org.example.practice2.SharedQueue;
import org.example.practice2.receiver.ReceiverImpl;
import org.example.practice3.TestScenario;

public class StoreClientTCP {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static volatile boolean isConnected = false;

    public static void main(String[] args) {
        SharedQueue<byte[]> queue = new SharedQueue<>();

         ReceiverImpl receiver = new ReceiverImpl(queue);
         new Thread(receiver).start();

//        TestScenario testProducer = new TestScenario(queue);
//        new Thread(testProducer).start();

        byte[] dataToResend = null;

        while (!Thread.currentThread().isInterrupted()) {
            try (Socket socket = new Socket(HOST, PORT);
                 DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                 DataInputStream input = new DataInputStream(socket.getInputStream())) {

                System.out.println("TCP client connected");
                isConnected = true;

                Decrypter decrypter = new Decrypter(new MessageCipher());
                Thread readerThread = new Thread(() -> {
                    try {
                        while (!socket.isClosed() && isConnected) {
                            int length = input.readInt();
                            byte[] responseData = new byte[length];
                            input.readFully(responseData);
                            Message responseMsg = decrypter.decrypt(responseData);
                            System.out.println("TCP Server response: " + responseMsg.messageString());
                        }
                    } catch (Exception e) {
                        System.out.println("Reader disconnected (server down)");
                        isConnected = false;
                    }
                });
                readerThread.start();

                while (isConnected) {
                    byte[] data = (dataToResend != null) ? dataToResend : queue.consume();
                    if (data == null) continue;

                    dataToResend = data;

                    if (!isConnected) {
                        throw new IOException("Connection dropped detected by reader");
                    }

                    output.writeInt(data.length);
                    output.write(data);
                    output.flush();

                    dataToResend = null;
                }

            } catch (IOException e) {
                isConnected = false;
                System.out.println("Server unavailable. Reconnecting in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("TCP client disconnected");
            }
        }
    }
}
