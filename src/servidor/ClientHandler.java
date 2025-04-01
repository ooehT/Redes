package servidor;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean authenticated = false;
    private Map<String, String> users;
    private CommandHandler commandHandler; // Adicionando CommandHandler

    public ClientHandler(Socket socket, Map<String, String> users) {
        this.socket = socket;
        this.users = users;
    }

    @Override
    public void run() {

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bem-vindo ao MyFTP. Faça login (login usuario senha):");

            while (true) {
                String command = in.readLine();
                if (command == null || command.trim().isEmpty()) {
                    out.println("Comando inválido. Tente novamente.");
                    continue;
                }

                String[] parts = command.split(" ");

                if (!authenticated) {
                    handleLogin(parts, out);
                    if (authenticated) {
                        commandHandler = new CommandHandler(socket); // Inicializa CommandHandler

                    }
                    continue; // Volta para o início do loop após o login
                }

                // Após autenticação, os comandos são passados para o CommandHandler
                commandHandler.handleCommand(parts, out, socket);
            }

        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }

    private void handleLogin(String[] parts, PrintWriter out) {
        if (parts.length == 3 && "login".equals(parts[0])) {
            if (users.containsKey(parts[1]) && users.get(parts[1]).equals(parts[2])) {
                authenticated = true;
                out.println("Login bem-sucedido!");
            } else {
                out.println("Usuário ou senha incorretos.");
            }
        } else {
            out.println("Comando inválido. Faça login primeiro (login usuario senha).");
        }
    }
}
