package org.example.practice3.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class StoreServerUDP {

    private static final int PORT = 8081;

    public static void main(String[] args) {

        try (DatagramSocket socket =
                     new DatagramSocket(PORT)) {

            System.out.println(
                    "UDP server started on port "
                            + PORT
            );

            while (true) {
                byte[] buffer = new byte[65507];
                DatagramPacket packet =
                        new DatagramPacket(
                                buffer,
                                buffer.length
                        );
                socket.receive(packet);
                System.out.println(
                        "UDP server received package: "
                                + packet.getLength()
                                + " bytes"
                );
                byte[] ack = "ACK_UDP".getBytes();
                DatagramPacket ackPacket =
                        new DatagramPacket(
                                ack,
                                ack.length,
                                packet.getAddress(),
                                packet.getPort()
                        );

                socket.send(ackPacket);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while trying to send UDP packet", e);
        }
    }
}
