package to.grindelf.messengertexst.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static to.grindelf.messengertexst.utils.Constants.*;

public class ChatServer {
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final SimpleDateFormat dateFormat = DATE_FORMAT;
    private static String messageHistory;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started on port " + SERVER_PORT);

            messageHistory = readMessageHistory();
            System.out.println("Message history loaded: ");
            System.out.println(messageHistory);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String message, ClientHandler sender, boolean historyBroadcast, boolean toAllClients, boolean connectionMessage) {
        if (!historyBroadcast) {
            System.out.println(message);
            saveMessageToHistory(message);
        }

        // Обновляем историю сообщений для новых клиентов
        if (!clients.isEmpty() && !historyBroadcast) {
            messageHistory = readMessageHistory();
        }

        for (ClientHandler client : clients) {
            if (historyBroadcast && client == sender) {
                client.sendMessage(message);
            } else if (toAllClients && client != sender) {
                client.sendMessage(message);
            } else if (connectionMessage) {
                client.sendMessage(message);
            }
        }
    }

    private static void saveMessageToHistory(String message) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(HISTORY_FILE, true))
        ) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readMessageHistory() {
        try {
            return Files.readString(Paths.get(HISTORY_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);
                clientName = "Client-" + new Random().nextInt(1000);

                // Отправить только историю чата новому клиенту
                synchronized (clients) {
                    broadcast(messageHistory, this, true, false, false);
                }

                // Уведомить всех о подключении клиента
                String connectMessage = dateFormat.format(new Date()) + " - SERVER - " + clientName + " joined the chat.";
                broadcast(connectMessage, this, false, true, true);

                String message;
                while ((message = in.readLine()) != null) {
                    String formattedMessage = dateFormat.format(new Date()) + " - " + clientName + " - " + message;
                    broadcast(formattedMessage, this, false, true, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleClientDisconnect();
            }
        }

        private void handleClientDisconnect() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (clients) {
                clients.remove(this);
            }
            String disconnectMessage = dateFormat.format(new Date()) + " - SERVER - " + clientName + " left the chat.";
            broadcast(disconnectMessage, this, false, true, true);
        }

        void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
