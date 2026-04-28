package com.joelmir.chatwifi;

import java.util.ArrayList;
import java.util.List;

public class ChatHolder {

    public static ChatConnection chat;

    public static String lastError = "";
    public static String mode = "client";
    public static boolean isServer = false;
    public static String serverIp = "";

    // 🔥 estado REAL da conexão
    public static volatile boolean isConnected = false;

    public static boolean isStarted = false;

    public static String status = "idle";

    public static final String ACTION_STATUS =
            "com.joelmir.chatwifi.STATUS_UPDATE";

    // =========================
    // HISTÓRICO
    // =========================
    public static final List<String> messages =
            new ArrayList<String>();

    public static synchronized void addMessage(String msg) {

        if (msg == null) return;

        messages.add(msg);

        if (messages.size() > 200) {
            messages.remove(0);
        }
    }

    public static synchronized void clearMessages() {
        messages.clear();
    }

    // =========================
    // RESET TOTAL DO SISTEMA
    // =========================
    public static synchronized void reset() {

        if (chat != null) {
            try {
                chat.close();
            } catch (Exception ignored) {}
        }

        chat = null;

        mode = "client";
        isServer = false;
        isStarted = false;

        status = "idle";
        serverIp = "";
        lastError = "";

        isConnected = false; // 🔥 FIX CRÍTICO

        messages.clear();
    }

    // =========================
    // ATUALIZAÇÃO SEGURA DE CONEXÃO
    // =========================
    public static void setConnected(boolean value) {
        isConnected = value;

        if (!value) {
            status = "disconnected";
        }
    }
}