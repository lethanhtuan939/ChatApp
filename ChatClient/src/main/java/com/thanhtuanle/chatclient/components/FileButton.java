package com.thanhtuanle.chatclient.components;

import com.thanhtuanle.chatclient.Main;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class FileButton extends JButton {

    JFileChooser fc;

    public FileButton() {
        fc = new JFileChooser();
        try {
            this.setIcon(getIcon());
        } catch (Exception e) {
            // ignore
        }

        addActionListener((ActionEvent e) -> {
            if (Main.chatScreen.getCurrentChatUser() != null || Main.chatScreen.getCurrentChatGroup() != null) {
                int choice = fc.showOpenDialog(Main.chatScreen.getContentPane());
                if (choice == JFileChooser.APPROVE_OPTION) {
                    try {
                        File f = fc.getSelectedFile();
                        if (Main.chatScreen.getCurrentChatUser() != null) {
                            Main.client.sendLine("/send_file");
                            Main.client.sendLine(Main.chatScreen.getCurrentChatUser());
                            Main.client.sendLine(f.getName());
                            Main.client.sendFile(f);
                        } else if (Main.chatScreen.getCurrentChatGroup() != null) {
                            Main.client.sendLine("/send_group_file");
                            Main.client.sendLine(Main.chatScreen.getCurrentChatGroup());
                            Main.client.sendLine(f.getName());
                            Main.client.sendFile(f);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(Main.chatScreen.getContentPane(), "Đã xảy ra lỗi khi gửi file!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

    public ImageIcon getIcon() {
        return new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/attach.png"))) {
            @Override
            public int getIconWidth() {
                return 24;
            }

            @Override
            public int getIconHeight() {
                return 24;
            }

            @Override
            public synchronized void paintIcon(Component c, Graphics g,
                    int x, int y) {
                g.drawImage(getImage(), x, y, 24, 24, null);
            }
        };
    }
}
