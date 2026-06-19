package org.example.practice3.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.example.practice2.Scaling;

public class StoreServerUDP {
    private final int port;
    private final Scaling pipeline;

    public StoreServerUDP(int port, Scaling pipeline) {
        this.port = port;
        this.pipeline = pipeline;
    }

    public void start() {
        Set<SocketAddress> activeClients = ConcurrentHashMap.newKeySet();

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP server started on port " + port);

            new Thread(() -> {
                try {
                    while (true) {
                        byte[] responseData = pipeline.getSendQueue().consume();
                        for (SocketAddress client : activeClients) {
                            try {
                                DatagramPacket respPacket = new DatagramPacket(
                                        responseData, responseData.length, client);
                                socket.send(respPacket);
                            } catch (Exception e) {
                                System.err.println("Failed to send UDP response: " + e.getMessage());
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            while (true) {
                byte[] buffer = new byte[65507];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                SocketAddress clientAddress = packet.getSocketAddress();
                activeClients.add(clientAddress);

                byte[] ack = "ACK_UDP".getBytes();
                DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, clientAddress);
                socket.send(ackPacket);

                byte[] actualData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, actualData, 0, packet.getLength());
                pipeline.getRawQueue().produce(actualData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while processing UDP packets", e);
        }
    }
}
