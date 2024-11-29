package com.thanhtuanle.chatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class Server implements Runnable {

    private final int port;
    public static ArrayList<ConnectionHandler> connections;
    private ExecutorService pool;
    private boolean isDone;
    private ServerSocket server;
    private static JTextArea txtLog;
    private static JTable tblLog;
    private static DefaultTableModel tableModel;
    private String username;
    private static SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public Server(int port, JTextArea txtLog, JTable tblLog) {
        this.isDone = false;
        this.port = port;
        connections = new ArrayList<>();
        this.txtLog = txtLog;
        this.tblLog = tblLog;
        this.tableModel = (DefaultTableModel) tblLog.getModel();
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            // start server
            server = new ServerSocket(this.port);
            txtLog.append(formatter.format(new Date()) + " - Server started!\n");
            pool = Executors.newCachedThreadPool();
            while (!isDone) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void shutdown() {
        try {
            isDone = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void broadcast(String msg) {
        for (ConnectionHandler ch : connections) {
            if (ch != null && ch.getClient().getUsername() != null) {
                ch.sendLine(msg);
            }
        }
    }

    public static void broadcast(String msg, String except) {
        for (ConnectionHandler ch : connections) {
            if (ch != null && ch.getClient().getUsername() != null && !Objects.equals(ch.getClient().getUsername(), except)) {
                ch.sendLine(msg);
            }
        }
    }

    public static void setQuitLog(String username, String type) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        txtLog.append(formatter.format(date) + " - " + username + " " + type + "\n");
        SwingUtilities.invokeLater(() -> txtLog.updateUI());
    }

    public static void setTableLog(String message, String clientIP) {
        tableModel.addRow(new Object[]{tableModel.getRowCount() + 1, clientIP, message, formatter.format(new Date()),});
    }
}
