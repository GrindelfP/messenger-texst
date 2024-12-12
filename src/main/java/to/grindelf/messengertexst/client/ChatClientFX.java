package to.grindelf.messengertexst.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import static to.grindelf.messengertexst.utils.Constants.*;

public class ChatClientFX extends Application {
    private static final String serverAddress = SERVER_ADDRESS;
    private static final int serverPort = SERVER_PORT;
    private static final SimpleDateFormat dateFormat = DATE_FORMAT;

    private PrintWriter out;
    private BufferedReader in;

    private TextArea chatArea;
    private TextField inputField;
    private Button sendButton;

    @Override
    public void start(@NotNull Stage primaryStage) {
        VBox root = new VBox(10);
        TextArea chatArea = new TextArea();
        chatArea.setPrefRowCount(100);
        chatArea.setEditable(false);
        TextField inputField = new TextField();
        inputField.setPromptText("Enter your message...");
        Button sendButton = new Button("Send");

        root.getChildren().addAll(chatArea, inputField, sendButton);

        Scene scene = new Scene(root, 600, 600);
        primaryStage.setTitle("Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Подключение к серверу в отдельном потоке
        new Thread(() -> {
            try (Socket socket = new Socket(serverAddress, serverPort);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))
            ) {

                out = new PrintWriter(socket.getOutputStream(), true);

                // Получение сообщений от сервера
                String message;
                while ((message = in.readLine()) != null) {
                    String finalMessage = message;
                    // Обновляем UI в JavaFX Application Thread
                    javafx.application.Platform.runLater(
                            () -> chatArea.appendText(finalMessage + "\n")
                    );
                }
            } catch (IOException e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(
                        () -> chatArea.appendText("Disconnected from server.\n")
                );
            }
        }).start();

        // Отправка сообщений
        sendButton.setOnAction(event -> sendMessage(inputField, chatArea));
        inputField.setOnAction(event -> sendMessage(inputField, chatArea));
    }

    private void sendMessage(@NotNull TextField inputField, TextArea chatArea) {
        String message = inputField.getText();
        if (message.isEmpty()) {
            return;
        }
        out.println(message);
        inputField.clear();
        chatArea.appendText(dateFormat.format(new Date()) + " - You - " + message + "\n");
    }

    @Override
    public void stop() throws Exception {
        if (out != null) {
            out.close();
        }
        if (in != null) {
            in.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
