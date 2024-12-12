package to.grindelf.messengertexst.server;

import org.jetbrains.annotations.NotNull;

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
    private static final int PORT = SERVER_PORT;
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final SimpleDateFormat dateFormat = DATE_FORMAT;
    private static final String chatHistory = HISTORY_FILE;
    private static final String messageHistory = readMessageHistory();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
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

    static void broadcast(String message, ClientHandler sender, boolean historyBroadcast) {
        if (!historyBroadcast) {
            System.out.println(message);
            saveMessageToHistory(message);
        }
        for (ClientHandler client : clients) {
            if (historyBroadcast) {
                client.sendMessage(message);
            } else if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    private static void saveMessageToHistory(String message) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(chatHistory, true))
        ) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static @NotNull String readMessageHistory() {
        try {
            return Files.readString(Paths.get(chatHistory));
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
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);
                clientName = "Client-" + new Random().nextInt(1000);

                synchronized (clients) {
                    broadcast(
                            messageHistory,
                            this,
                            true
                    );
                }

                broadcast(
                        dateFormat.format(
                                new Date()) + " - SERVER - " + clientName + " joined the chat.",
                        this,
                        false
                );

                String message;

                while ((message = in.readLine()) != null) {
                    broadcast(
                            dateFormat.format(
                                    new Date()) + " - " + clientName + " - " + message,
                            this,
                            false
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clients) {
                    clients.remove(this);
                }
                broadcast(
                        dateFormat.format(
                                new Date()) + " - SERVER - " + clientName + " left the chat.",
                        this,
                        false
                );
            }
        }

        void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
