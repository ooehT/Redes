package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MyFTPServer {
    private static final int PORT = 2121;
    private static Map<String, String> users = new HashMap<>();

    public static void main(String[] args) {
        // Usuários cadastrados
        users.put("admin", "1234");
        users.put("theo", "1234");
        users.put("marcelo", "1234");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, users);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
