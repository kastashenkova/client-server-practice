package org.example.practice3.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.example.practice2.SharedQueue;
import org.example.practice2.receiver.ReceiverImpl;

public class StoreClientTCP {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) {

        SharedQueue<byte[]> queue = new SharedQueue<>();

        ReceiverImpl receiver = new ReceiverImpl(queue);
        new Thread(receiver).start();

        while (!Thread.currentThread().isInterrupted()) {
            try (
                    Socket socket = new Socket(HOST, PORT);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    DataInputStream input = new DataInputStream(socket.getInputStream())
            ) {
                socket.setSoTimeout(3000);
                System.out.println("TCP client connected");
                byte[] dataToResend = null;
                while (true) {
                    byte[] data = (dataToResend != null) ? dataToResend : queue.consume();
                    if (data == null) {
                        continue;
                    }

                    dataToResend = data;
                    output.writeInt(data.length);
                    output.write(data);
                    output.flush();

                    try {
                        String ack = input.readUTF();
                        System.out.println("TCP ACK received: " + ack);
                        dataToResend = null;
                    } catch (SocketTimeoutException e) {
                        System.err.println("ACK delayed, retrying...");
                        continue;
                    }
                }

            } catch (IOException e) {
                System.out.println("Server unavailable. Reconnecting in 5 seconds...");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
