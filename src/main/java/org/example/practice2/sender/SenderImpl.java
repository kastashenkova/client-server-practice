package org.example.practice2.sender;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.example.practice2.SharedQueue;
import org.example.practice3.SocketWrapper;

public class SenderImpl implements Sender, Runnable {

    private final SharedQueue<byte[]> inputQueue;
    private volatile boolean active = true;
    private final List<SocketWrapper> activeConnections = new CopyOnWriteArrayList<>();

    public SenderImpl(SharedQueue<byte[]> inputQueue) {
        this.inputQueue = inputQueue;
    }

    public void addConnection(SocketWrapper wrapper) {
        activeConnections.add(wrapper);
    }

    public void removeConnection(SocketWrapper wrapper) {
        activeConnections.remove(wrapper);
    }

    @Override
    public void sendMessage(byte[] message, InetAddress target) {

        for (SocketWrapper wrapper : activeConnections) {
            try {
                boolean ok = wrapper.sendPackage(message);
                if (!ok) {
                    safeRemove(wrapper);
                }
            } catch (Exception e) {
                safeRemove(wrapper);
            }
        }
    }

    private void safeRemove(SocketWrapper wrapper) {
        try {
            wrapper.close();
        } catch (Exception ignored) {}

        activeConnections.remove(wrapper);
    }

    @Override
    public void run() {
        while (active && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] data = inputQueue.consume();

                if (data == null) continue;

                System.out.println("[SEND] " + data.length + " bytes");

                sendMessage(data, null);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                System.err.println("Sender error: " + e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        active = false;
        for (SocketWrapper wrapper : activeConnections) {
            try {
                wrapper.close();
            } catch (Exception ignored) {}
        }
        activeConnections.clear();
    }

    public boolean isEmpty() {
        return activeConnections.isEmpty();
    }
}
