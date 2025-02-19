package com.thanhtuanle.chatclient.components;

import com.thanhtuanle.chatclient.ChatScreen;
import com.thanhtuanle.chatclient.Main;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Group extends JButton {

    private String id;
    private String name;
    private ArrayList<String> userList;
    public ArrayList<ArrayList<String>> chatLog;
    private boolean initialized_chat = false;

    public Group(ArrayList<String> groupInfo) {
//        String wrapped  = WordUtils.wrap(groupInfo.get(1), 15, "\n", true);
        String displayName = groupInfo.get(1).length() <= 20 ? groupInfo.get(1) : groupInfo.get(1).substring(0, 20) + "...";

        this.setText(displayName);
        this.setHorizontalAlignment(SwingConstants.LEFT);
        this.chatLog = new ArrayList<>();
        this.id = groupInfo.get(0);
        this.name = groupInfo.get(1);
        int size = groupInfo.size() - 2;
        this.userList = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            this.userList.add(groupInfo.get(2 + i)); // usernames
        }
        this.setContentAreaFilled(false);
        this.setBackground(ChatScreen.GROUP);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!initialized_chat) {
                    initialized_chat = true;
                    Main.chatScreen.clearMessageList();
                    Main.client.sendLine("/get_group_chat_log_from");
                    Main.client.sendLine(id);
                } else {
                    if (Main.chatScreen.getCurrentChatGroup() == null || !Main.chatScreen.getCurrentChatGroup().equals(id)) {
                        Main.chatScreen.clearMessageList();
                        Main.chatScreen.setCurrentChatGroup(id);
                        Main.chatScreen.setCurrentChatUser(null);
                        Main.chatScreen.updateMsgList();
                    }
                }
                Main.chatScreen.setCurrentChatGroup(id);
                Main.chatScreen.setCurrentChatUser(null);
                Main.chatScreen.setTitle(Main.chatScreen.getOriginalTitle() + " - Texting " + name);
            }
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

    public ArrayList<String> getUserList() {
        return userList;
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
