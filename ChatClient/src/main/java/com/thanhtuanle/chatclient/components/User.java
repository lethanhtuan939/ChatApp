package com.thanhtuanle.chatclient.components;

import com.thanhtuanle.chatclient.ChatScreen;
import com.thanhtuanle.chatclient.Main;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.SwingConstants;

public class User extends JButton {

    public String username;
    public ArrayList<ArrayList<String>> chatLog;
    private boolean initialized_chat = false;

    public User(String username) {
        String displayName = username.length() <= 20 ? username : username.substring(0, 20) + "...";
        this.setText(displayName);
        this.setHorizontalAlignment(SwingConstants.LEFT);
        this.chatLog = new ArrayList<>();

        this.username = username;
        this.setContentAreaFilled(false);
        this.setBackground(ChatScreen.OFFLINE);
        addActionListener((ActionEvent e) -> {
            if (!initialized_chat) {
                initialized_chat = true;
                Main.chatScreen.clearMessageList();
                Main.client.sendLine("/get_chat_log_from");
                Main.client.sendLine(username);
            } else {
                if (Main.chatScreen.getCurrentChatUser() == null || !username.equals(Main.chatScreen.getCurrentChatUser())) {
                    Main.chatScreen.clearMessageList();
                    Main.chatScreen.setCurrentChatGroup(null);
                    Main.chatScreen.setCurrentChatUser(username);
                    Main.chatScreen.updateMsgList();
                }
            }
            Main.chatScreen.setCurrentChatGroup(null);
            Main.chatScreen.setCurrentChatUser(username);
            Main.chatScreen.setTitle(Main.chatScreen.getOriginalTitle() + " - Texting " + username);
        });
    }

    public boolean getInitializedStatus() {
        return initialized_chat;
    }

    public void addToChatLog(String from, String content, String id, String type) {
        chatLog.add(new ArrayList<>(Arrays.asList(from, content, id, type)));
    }

    public void removeFromChatLog(String id) {
        for (int i = 0; i < chatLog.size(); ++i) {
            if (Objects.equals(chatLog.get(i).get(2), id)) {
                chatLog.remove(i);
                break;
            }
        }
    }

    public ArrayList<ArrayList<String>> getChatLog() {
        return chatLog;
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(
                new Point(0, 0),
                Color.WHITE,
                new Point(0, getHeight()),
                getBackground()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();

        super.paintComponent(g);
    }
}
