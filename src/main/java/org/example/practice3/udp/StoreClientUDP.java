package org.example.practice3.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import org.example.practice2.SharedQueue;
import org.example.practice2.receiver.ReceiverImpl;

public class StoreClientUDP {

    private static final String HOST = "localhost";
    private static final int PORT = 8081;

    private static final int TIMEOUT = 3000;
    private static final int RETRIES = 5;

    public static void main(String[] args) {

        SharedQueue<byte[]> queue = new SharedQueue<>();

        ReceiverImpl receiver = new ReceiverImpl(queue);
        new Thread(receiver).start();

        try (DatagramSocket socket = new DatagramSocket()) {

            socket.setSoTimeout(TIMEOUT);

            InetAddress address = InetAddress.getByName(HOST);

            while (true) {

                byte[] data = queue.consume();

                if (data == null) {
                    continue;
                }

                boolean delivered = false;
                int retries = 0;

                while (!delivered && retries < RETRIES) {

                    try {

                        DatagramPacket packet =
                                new DatagramPacket(
                                        data,
                                        data.length,
                                        address,
                                        PORT
                                );

                        socket.send(packet);

                        byte[] ackBuffer = new byte[32];

                        DatagramPacket ackPacket =
                                new DatagramPacket(
                                        ackBuffer,
                                        ackBuffer.length
                                );

                        socket.receive(ackPacket);

                        String ack = new String(
                                ackPacket.getData(),
                                0,
                                ackPacket.getLength()
                        );

                        if ("ACK_UDP".equals(ack)) {

                            delivered = true;

                            System.out.println(
                                    "UDP package delivered"
                            );
                        }

                    } catch (SocketTimeoutException e) {

                        retries++;

                        System.out.println(
                                "Retry " + retries
                        );
                    }
                }

                if (!delivered) {

                    System.out.println(
                            "UDP package dropped after "
                                    + RETRIES
                                    + " retries"
                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while processing UDP packet", e);
        }
    }
}
