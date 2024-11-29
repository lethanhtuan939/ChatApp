package com.thanhtuanle.chatserver;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

public class ConnectionHandler implements Runnable {

    private Client client;
    private PrintWriter out;
    private BufferedReader in;
    private DataInputStream fin;
    private DataOutputStream fout;
    private boolean done;

    public ConnectionHandler(Socket socket) {
        this.client = new Client(socket);
    }

    @Override
    public void run() {
        try {
            done = false;
            this.out = new PrintWriter(client.getSocket().getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(client.getSocket().getInputStream()));
            this.fin = new DataInputStream(client.getSocket().getInputStream());
            this.fout = new DataOutputStream(client.getSocket().getOutputStream());
            String msg;
            while (!done && (msg = this.receiveLine()) != null) {
                switch (msg) {
                    case "/quit" -> {
                        if (client.getUsername() != null) {
                            Server.broadcast("/user_offline " + client.getUsername(), client.getUsername());
                            this.client.setUsername(null);
                        }
                        this.shutdown();
                    }
                    case "/login" -> {
                        String username = this.receiveLine().strip();
                        String password = this.receiveLine().strip();
                        try {
                            boolean isOnline = false;
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && username.equals(ch.client.getUsername())) {
                                    this.sendLine("/login_error User already logged in!");
                                    isOnline = true;
                                    break;
                                }
                            }
                            if (isOnline) {
                                break;
                            }
                            boolean success = Database.login(username, password);
                            if (!success) {
                                this.sendLine("/login_error Username or password is incorrect!");
                            } else {
                                client.setUsername(username);
                                this.sendLine("/login_success " + username);
                                Server.broadcast("/user_online " + username);
                                Server.setQuitLog(username, "connected");
                                Server.setTableLog(username, this.client.getSocket().getInetAddress().getHostAddress());
                            }
                        } catch (NoSuchAlgorithmException e) {
                            this.sendLine("/login_error Server error!");
                        }
                    }
                    case "/logout" -> {
                        Server.broadcast("/user_offline " + client.getUsername(), client.getUsername());
                        Server.setQuitLog(client.getUsername(), "exited");
                        this.sendLine("/logout_success");
                        client.setUsername(null);
                    }
                    case "/register" -> {
                        String username = this.receiveLine().strip();
                        String password = this.receiveLine().strip();
                        try {
                            boolean success = Database.register(username, password);
                            if (!success) {
                                this.sendLine("/register_error Username already existed!");
                            } else {
                                this.sendLine("/register_success Register successfully, go to login!");
                                Server.broadcast("/new_user " + username);
                            }
                        } catch (NoSuchAlgorithmException e) {
                            this.sendLine("/register_error Username already existed!");
                        }
                    }
                    case "/chat" -> {
                        String to = this.receiveLine().strip();
                        String content = this.receiveLine().strip();
                        String type = MessageType.TEXT.getValue();
                        String id = Database.saveChat(client.getUsername(), to, content, type);
                        if (id != null) {
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && ch.client.getUsername().equals(to) && !ch.client.getUsername().equals(this.client.getUsername())) {
                                    ch.sendLine("/message_from");
                                    ch.sendLine(this.client.getUsername());
                                    ch.sendLine(content);
                                    ch.sendLine(id);
                                    ch.sendLine(type);
                                    break;
                                }
                            }
                            sendLine("/chat_success");
                            sendLine(to);
                            sendLine(content);
                            sendLine(id);
                            sendLine(type);
                        } else {
                            sendLine("/error_chat Cann't send a message!");
                        }
                    }
                    case "/get_users" -> {
                        String self = this.receiveLine().strip();
                        ArrayList<String> userList = Database.getAllUsers();
                        this.sendLine("/user_list");
                        this.sendLine(Integer.toString(userList.size()));
                        for (String username : userList) {
                            this.sendLine(username);
                        }
                    }
                    case "/get_online_users" -> {
                        ArrayList<String> online_users = new ArrayList<>();
                        for (ConnectionHandler ch : Server.connections) {
                            if (ch.client != null && ch.client.getUsername() != null) {
                                online_users.add(ch.client.getUsername());
                            }
                        }
                        this.sendLine("/online_user_list");
                        this.sendLine(Integer.toString(online_users.size()));
                        for (String user : online_users) {
                            this.sendLine(user);
                        }
                    }
                    case "/get_chat_log_from" -> {
                        String username = this.receiveLine().strip();
                        ArrayList<ArrayList<String>> logs = Database.getChatLog(client.getUsername(), username);
                        if (logs != null && !logs.isEmpty()) {
                            this.sendLine("/chat_log");
                            this.sendLine(username);
                            this.sendLine(Integer.toString(logs.size()));
                            for (ArrayList<String> log : logs) {
                                this.sendLine(log.get(0)); // from
                                this.sendLine(log.get(1)); // content
                                this.sendLine(log.get(2)); // id
                                this.sendLine(log.get(3)); // type
                            }
                        }
                    }
                    case "/remove_message" -> {
                        String id = this.receiveLine().strip();
                        String chatUser = this.receiveLine().strip();
                        if (Database.removeMessage(this.client.getUsername(), id)) {
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && ch.client.getUsername().equals(chatUser)) {
                                    ch.sendLine("/remove_message");
                                    ch.sendLine(this.client.getUsername());
                                    ch.sendLine(id);
                                    break;
                                }
                            }
                            this.sendLine("/remove_message");
                            this.sendLine(chatUser);
                            this.sendLine(id);
                        }
                    }
                    case "/remove_group_message" -> {
                        String id = this.receiveLine().strip();
                        String group_id = this.receiveLine().strip();
                        ArrayList<String> usersInGroup = Database.getUsersFromGroup(group_id);
                        if (Database.removeMessage(this.client.getUsername(), id)) {
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && usersInGroup.contains(ch.client.getUsername())) {
                                    ch.sendLine("/remove_group_message");
                                    ch.sendLine(group_id);
                                    ch.sendLine(id);
                                }
                            }
                        }
                    }
                    case "/remove_file" -> {
                        String id = this.receiveLine().strip();
                        String chatUser = this.receiveLine().strip();
                        String filenameToDelete = null;
                        if ((filenameToDelete = Database.getFileNameFromMessage(this.client.getUsername(), id)) != null) {
                            File fileToDelete = new File(filenameToDelete);
                            if ((!fileToDelete.exists() || fileToDelete.isDirectory() || fileToDelete.delete()) && Database.removeFile(this.client.getUsername(), id)) {
                                for (ConnectionHandler ch : Server.connections) {
                                    if (ch.client != null && ch.client.getUsername().equals(chatUser)) {
                                        ch.sendLine("/remove_message");
                                        ch.sendLine(this.client.getUsername());
                                        ch.sendLine(id);
                                        break;
                                    }
                                }
                                this.sendLine("/remove_message");
                                this.sendLine(chatUser);
                                this.sendLine(id);
                            }
                        }
                    }
                    case "/remove_group_file" -> {
                        String id = this.receiveLine().strip();
                        String group_id = this.receiveLine().strip();
                        String filenameToDelete = null;

                        if ((filenameToDelete = Database.getFileNameFromMessage(this.client.getUsername(), id)) != null) {
                            File fileToDelete = new File(filenameToDelete);
                            if ((!fileToDelete.exists() || fileToDelete.isDirectory() || fileToDelete.delete()) && Database.removeFile(this.client.getUsername(), id)) {
                                ArrayList<String> usersInGroup = Database.getUsersFromGroup(group_id);
                                for (ConnectionHandler ch : Server.connections) {
                                    if (ch.client != null && usersInGroup.contains(ch.client.getUsername())) {
                                        ch.sendLine("/remove_group_message");
                                        ch.sendLine(group_id);
                                        ch.sendLine(id);
                                    }
                                }
//                                this.sendLine("/remove_message");
//                                this.sendLine(chatUser);
//                                this.sendLine(id);
                            }
                        }
                    }
                    case "/send_file" -> {
                        String to = this.receiveLine().strip();
                        String orgFileName = this.receiveLine().strip();
                        String ext = FilenameUtils.getExtension(orgFileName);
                        String filename = UUID.randomUUID().toString() + '.' + ext;
                        String filepath = "assets/" + filename;
                        receiveFile(filepath);

                        String content = filepath + "|" + orgFileName;
                        String type = MessageType.FILE.getValue();
                        String id = Database.saveChat(client.getUsername(), to, content, type);
                        if (id != null) {
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && ch.client.getUsername().equals(to)) {
                                    ch.sendLine("/message_from");
                                    ch.sendLine(this.client.getUsername());
                                    ch.sendLine(content);
                                    ch.sendLine(id);
                                    ch.sendLine(type);
                                    break;
                                }
                            }
                            sendLine("/chat_success");
                            sendLine(to);
                            sendLine(content);
                            sendLine(id);
                            sendLine(type);
                        } else {
                            sendLine("/error_chat Cann't send the file!");
                        }
                    }
                    case "/send_group_file" -> {
                        String to = this.receiveLine().strip();
                        String orgFileName = this.receiveLine().strip();
                        String ext = FilenameUtils.getExtension(orgFileName);
                        String filename = UUID.randomUUID().toString() + '.' + ext;
                        String filepath = "assets/" + filename;
                        receiveFile(filepath);

                        String content = filepath + "|" + orgFileName;
                        String type = MessageType.GROUP_FILE.getValue();
                        String id = Database.saveChat(client.getUsername(), to, content, type);
                        ArrayList<String> usersInGroup = Database.getUsersFromGroup(to);
                        if (id != null) {
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && usersInGroup.contains(ch.client.getUsername())) {
                                    ch.sendLine("/message_from");
                                    ch.sendLine(to);
                                    ch.sendLine(content);
                                    ch.sendLine(id);
                                    ch.sendLine(type);
                                    ch.sendLine(this.client.getUsername());
                                }
                            }
                        } else {
                            sendLine("/error_chat Cann't send a file!");
                        }
                    }
                    case "/download_file" -> {
                        String serverFileName = this.receiveLine().strip();
                        String orgFileName = this.receiveLine().strip();
                        File fileToSend = new File(serverFileName);
                        if (fileToSend.exists() && !fileToSend.isDirectory()) {
                            this.sendLine("/download_file");
                            this.sendLine(orgFileName);
                            this.sendFile(fileToSend);
                        } else {
                            this.sendLine("/download_error");
                            this.sendLine("The File don't exist on server!");
                        }
                    }
                    case "/create_group" -> {
                        String name = this.receiveLine().strip();
                        int n = Integer.parseInt(this.receiveLine().strip());
                        ArrayList<String> userList = new ArrayList<>();
                        for (int i = 0; i < n; ++i) {
                            userList.add(this.receiveLine().strip());
                        }
                        userList.add(this.client.getUsername());
                        String id = Database.createGroup(name, userList);
                        if (id != null) {
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && userList.contains(ch.client.getUsername())) {
                                    ch.sendLine("/new_group");
                                    ch.sendLine(id);
                                    ch.sendLine(name);
                                    ch.sendLine(Integer.toString(userList.size()));
                                    for (String user : userList) {
                                        ch.sendLine(user);
                                    }
                                }
                            }
                        }
                    }
                    case "/get_groups" -> {
                        String username = in.readLine().strip();
                        ArrayList<ArrayList<String>> groups = Database.getAllGroup(username);
                        this.sendLine("/group_list");
                        this.sendLine(Integer.toString(groups.size()));
                        for (ArrayList<String> group : groups) {
                            this.sendLine(group.get(0)); // id
                            this.sendLine(group.get(1)); // name
                            int size = group.size() - 2;
                            this.sendLine(Integer.toString(size));
                            for (int i = 0; i < size; ++i) {
                                this.sendLine(group.get(2 + i)); // usernames
                            }
                        }
                    }
                    case "/group_chat" -> {
                        String to = this.receiveLine().strip();
                        String content = this.receiveLine().strip();
                        String type = MessageType.GROUP_TEXT.getValue();
                        String id = Database.saveChat(client.getUsername(), to, content, type);
                        if (id != null) {
                            ArrayList<String> usersInGroup = Database.getUsersFromGroup(to);
                            for (ConnectionHandler ch : Server.connections) {
                                if (ch.client != null && usersInGroup.contains(ch.client.getUsername())) {
                                    ch.sendLine("/message_from");
                                    ch.sendLine(to);
                                    ch.sendLine(content);
                                    ch.sendLine(id);
                                    ch.sendLine(type);
                                    ch.sendLine(this.client.getUsername());
                                }
                            }
                        } else {
                            sendLine("/error_chat Cann't sned the message!");
                        }
                    }
                    case "/get_group_chat_log_from" -> {
                        String id = in.readLine().strip();
                        ArrayList<ArrayList<String>> logs = Database.getGroupChatLog(id);
                        if (logs != null && !logs.isEmpty()) {
                            this.sendLine("/group_chat_log");
                            this.sendLine(id);
                            this.sendLine(Integer.toString(logs.size()));
                            for (ArrayList<String> log : logs) {
                                this.sendLine(log.get(0)); // from
                                this.sendLine(log.get(1)); // content
                                this.sendLine(log.get(2)); // id
                                this.sendLine(log.get(3)); // type
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            this.shutdown();
        }

    }

    public void sendLine(String msg) {
        this.out.println(msg);
    }

    public void send(String msg) {
        this.out.print(msg);
    }

    public String receiveLine() throws IOException {
        return this.in.readLine();
    }

    public void receiveFile(String filename) throws Exception {
        int bytes = 0;
        File file = new File(filename);
        file.getParentFile().mkdirs();
        file.createNewFile();
        try (FileOutputStream fileOutputStream = new FileOutputStream(file, false)) {
            long size = fin.readLong(); // read file size
            byte[] buffer = new byte[4 * 1024];
            while (size > 0
                    && (bytes = fin.read(
                            buffer, 0,
                            (int) Math.min(buffer.length, size)))
                    != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes;
            }
//        System.out.println("File is Received");
        } // read file size
    }

    public void sendFile(File file) throws Exception {
        int bytes = 0;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            file.length();
            fout.writeLong(file.length());

            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer))
                    != -1) {
                fout.write(buffer, 0, bytes);
                fout.flush();
            }
            out.flush();
            fout.flush();
        }
    }

    public void shutdown() {
        try {
            done = true;
            this.client.setUsername(null);
            in.close();
            fin.close();
            fout.close();
            out.close();
            if (!client.getSocket().isClosed()) {
                client.getSocket().close();
            }
            this.client = null;
            Server.connections.remove(this);
        } catch (IOException e) {
            // ignore
        }
    }

    public Client getClient() {
        return client;
    }
}
