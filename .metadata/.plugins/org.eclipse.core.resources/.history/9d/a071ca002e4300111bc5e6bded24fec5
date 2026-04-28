package com.joelmir.chatwifi;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.net.ServerSocket;
import java.net.Socket;

public class ChatService extends Service {

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    private ServerSocket serverSocket;
    private Thread workerThread;

    private volatile boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        stopCurrent();
        acquireLocks();

        showNotification(); // 🔥 seguro para Android 2.2

        SharedPreferences prefs = getSharedPreferences("chat", MODE_PRIVATE);

        String modo = prefs.getString("modo", "client");
        String ip = prefs.getString("ip", "");
        int port = prefs.getInt("port", 9999);

        ChatHolder.lastError = "";
        ChatHolder.status = "initializing";
        sendStatus();

        if (ChatHolder.chat != null) {
            ChatHolder.chat.close();
        }

        ChatHolder.chat = new ChatConnection();
        running = true;

        if ("server".equals(modo)) {
            startServer(port);
        } else {
            startClient(ip);
        }

        return START_STICKY;
    }

    // =========================
    // NOTIFICAÇÃO COMPATÍVEL 2.2
    // =========================
    private void showNotification() {

        try {

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            Notification notification = new Notification(
                    android.R.drawable.stat_notify_sync,
                    "Chat ativo",
                    System.currentTimeMillis()
            );

            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    0,
                    new Intent(this, MainActivity.class),
                    0
            );

            notification.setLatestEventInfo(
                    this,
                    "Chat WiFi",
                    "Serviço rodando",
                    pi
            );

            manager.notify(1, notification);

        } catch (Exception e) {
            Log.e("CHAT", "notification error", e);
        }
    }

    // =========================
    // SERVER
    // =========================
    private void startServer(final int port) {

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    serverSocket = new ServerSocket(port);

                    ChatHolder.serverIp =
                            NetworkUtil.getWifiIp(getApplicationContext());

                    ChatHolder.status = "server_running";
                    sendStatus();

                    Log.d("SERVER", "IP: " + ChatHolder.serverIp);

                    while (running) {

                        try {

                            if (serverSocket == null || serverSocket.isClosed())
                                break;

                            ChatHolder.status = "waiting_client";
                            sendStatus();

                            Socket client = serverSocket.accept();

                            if (client != null) {

                                Log.d("SERVER", "Cliente conectado");

                                ChatHolder.chat.attachSocket(client);

                                ChatHolder.status = "connected";
                                sendStatus();
                            }

                        } catch (Exception e) {
                            if (running) {
                                Log.e("SERVER", "accept error", e);
                            }
                        }
                    }

                } catch (Exception e) {

                    Log.e("SERVER", "Erro: " + e.getMessage());

                    ChatHolder.lastError = e.toString();
                    ChatHolder.status = "error_server";
                    sendStatus();
                }
            }
        });

        workerThread.start();
    }

    // =========================
    // CLIENT
    // =========================
    private void startClient(final String ip) {

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (ip == null || ip.trim().length() == 0) {
                    ChatHolder.status = "error_client";
                    sendStatus();
                    return;
                }

                int attempts = 0;

                while (running && attempts < 10) {

                    try {

                        attempts++;

                        ChatHolder.status = "connecting";
                        sendStatus();

                        boolean ok = ChatHolder.chat.connectToServer(
                                ip.trim(),
                                9999
                        );

                        if (ok && ChatHolder.chat.isConnected()) {

                            ChatHolder.status = "connected";
                            sendStatus();
                            return;
                        }

                    } catch (Exception e) {

                        ChatHolder.lastError = e.toString();
                        ChatHolder.status = "error_client";
                        sendStatus();
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (Exception ignored) {}
                }

                ChatHolder.status = "failed_connect";
                sendStatus();
            }
        });

        workerThread.start();
    }

    // =========================
    private void sendStatus() {
        try {
            Intent i = new Intent(ChatHolder.ACTION_STATUS);
            i.putExtra("status", ChatHolder.status);
            i.putExtra("error", ChatHolder.lastError);
            sendBroadcast(i);
        } catch (Exception e) {
            Log.e("CHAT", "broadcast error", e);
        }
    }

    // =========================
    private void acquireLocks() {

        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Chat:WakeLock"
            );
            wakeLock.acquire();
        } catch (Exception e) {}

        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                    .getSystemService(WIFI_SERVICE);

            wifiLock = wm.createWifiLock(
                    WifiManager.WIFI_MODE_FULL,
                    "Chat:WifiLock"
            );
            wifiLock.acquire();

        } catch (Exception e) {}
    }

    // =========================
    private void stopCurrent() {

        running = false;

        try {
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception ignored) {}

        try {
            if (workerThread != null) workerThread.interrupt();
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopCurrent();

        try {
            if (ChatHolder.chat != null) ChatHolder.chat.close();

            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();

            if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();

        } catch (Exception e) {
            Log.e("CHAT", "destroy error", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}