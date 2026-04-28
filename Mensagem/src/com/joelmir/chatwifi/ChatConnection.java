package com.joelmir.chatwifi;

import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatConnection {

    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private volatile boolean connected = false;
    private volatile boolean listening = false;

    private Thread listenThread;
    private Thread serverThread;
    private Thread heartbeatThread;

    private MessageListener listener;
    private ConnectionListener connectionListener;

    public interface MessageListener {
        void onMessage(String msg);
    }

    public interface ConnectionListener {
        void onClientConnected();
    }

    public void setMessageListener(MessageListener l) {
        this.listener = l;
    }

    public void setConnectionListener(ConnectionListener l) {
        this.connectionListener = l;
    }

    // =========================
    // SERVER
    // =========================
    public boolean startServer(final int port) {

        close();

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    serverSocket = new ServerSocket(port);
                    Log.d("CHAT", "Servidor aguardando...");

                    while (true) {

                        Socket client = serverSocket.accept();

                        if (client != null) {

                            Log.d("CHAT", "Cliente conectado!");

                            attachSocket(client);

                            if (connectionListener != null) {
                                connectionListener.onClientConnected();
                            }

                            break;
                        }
                    }

                } catch (Exception e) {

                    Log.e("CHAT", "SERVER ERROR", e);

                    ChatHolder.lastError =
                            e.getClass().getSimpleName() + ": " + e.getMessage();
                }
            }
        });

        serverThread.start();
        return true;
    }

    // =========================
    // CLIENT
    // =========================
    public boolean connectToServer(String ip, int port) {

        try {

            close();

            socket = new Socket();
            socket.setReuseAddress(true);
            socket.setKeepAlive(true);

            InetSocketAddress address =
                    new InetSocketAddress(ip.trim(), port);

            socket.connect(address, 5000);

            setupConnection();

            return true;

        } catch (Exception e) {

            Log.e("CHAT", "CLIENT ERROR", e);

            ChatHolder.lastError =
                    e.getClass().getSimpleName() + ": " + e.getMessage();

            connected = false;
            return false;
        }
    }

    // =========================
    // SERVER SOCKET REAL
    // =========================
    public void attachSocket(Socket clientSocket) {
        try {
            close();

            this.socket = clientSocket;

            setupConnection();

            connected = true;
            ChatHolder.status = "connected"; // 🔥 ESSENCIAL

        } catch (Exception e) {
            Log.e("CHAT", "attachSocket error", e);
        }
    }

    // =========================
    private void setupConnection() throws Exception {

        if (socket == null || socket.isClosed()) return;

        out = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())
                ), true);

        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        connected = true;
        ChatHolder.status = "connected"; // 🔥 ADD ISSO

        startListening();
    }

    // =========================
    private void startHeartbeat() {

        heartbeatThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    while (connected) {

                        if (socket == null || socket.isClosed()) {
                            break;
                        }

                        if (out != null) {
                            out.println("PING");
                            out.flush();
                        }

                        Thread.sleep(3000);
                    }

                } catch (Exception e) {

                    Log.e("CHAT", "HEARTBEAT ERROR", e);

                    connected = false;
                    ChatHolder.status = "disconnected";
                    ChatHolder.lastError = "Servidor caiu";
                }
            }
        });

        heartbeatThread.start();
    }

    // =========================
    private void startListening() {

        listening = true;

        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    String line;

                    while (listening &&
                            connected &&
                            socket != null &&
                            !socket.isClosed() &&
                            (line = in.readLine()) != null) {

                        if (listener != null) {
                            listener.onMessage(line);
                        }
                    }

                } catch (Exception e) {

                    Log.e("CHAT", "LISTEN ERROR", e);

                    connected = false;

                    ChatHolder.lastError =
                            e.getClass().getSimpleName() + ": " + e.getMessage();
                }
            }
        });

        listenThread.start();
    }

    // =========================
    public synchronized void sendMessage(String msg) {

        try {

            if (out != null && connected && socket != null && !socket.isClosed()) {
                out.println(msg);
                out.flush();
            } else {
                Log.e("CHAT", "SEND BLOCKED - not connected");
            }

        } catch (Exception e) {

            Log.e("CHAT", "SEND ERROR", e);

            ChatHolder.lastError =
                    e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // =========================
    public synchronized void close() {

        connected = false;
        listening = false;

        try {
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}

        socket = null;
        serverSocket = null;
        out = null;
        in = null;
    }

    public boolean isConnected() {
        return socket != null
                && !socket.isClosed()
                && connected
                && out != null;
    }
}