package org.example.practice5;

import org.example.practice2.Scaling;
import org.example.practice3.tcp.StoreServerTCP;
import org.example.practice3.udp.StoreServerUDP;
import org.example.practice4.Database;

public class Main {
    public static void main(String[] args) {
        try {
            // one pipeline for TCP + UDP + HTTP
            Scaling pipeline = new Scaling(2, 2, 4, 3, 5);
            pipeline.start();

            Database sharedDatabase = pipeline.getWarehouseService().getDatabase();

            CustomHttpServer httpServer = new CustomHttpServer(8282, sharedDatabase);
            httpServer.start();

            new Thread(() -> {
                StoreServerUDP udpServer = new StoreServerUDP(8081, pipeline);
                udpServer.start();
            }).start();

            StoreServerTCP tcpServer = new StoreServerTCP(8080, pipeline);
            tcpServer.start();

        } catch (Exception e) {
            throw new RuntimeException("Error starting server", e);
        }
    }
}
