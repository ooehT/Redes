package servidor;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean authenticated = false;
    private Map<String, String> users;
    private String username;
    private CommandHandler commandHandler; // Adicionando CommandHandler

    public ClientHandler(Socket socket, Map<String, String> users) {
        this.socket = socket;
        this.users = users;
    }

    @Override
    public void run() {

        try {
            // Configurando timeout no socket para evitar espera indefinida-
            socket.setSoTimeout(60000); // 60 segundos

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bem-vindo ao MyFTP. Faça login (login usuario senha):");

            while (true) {
                    // Tenta ler do socket
                String command = in.readLine();

                // Valida o comando
                if (command == null || command.trim().isEmpty()) {
                    out.println("Comando inválido. Tente novamente.");
                    continue;
                }

                String[] parts = command.split(" ");

                if (!authenticated) {
                    handleLogin(parts, out);
                    if (authenticated) {
                        commandHandler = new CommandHandler(socket);
                        commandHandler.setClientDirectory(new File(commandHandler.getClientDirectory(), username));
                    }
                    continue;
                }

                // Passa os comandos autenticos para o handler
                commandHandler.handleCommand(parts, out, socket);

            }

        } catch (SocketException e) {
            // Timeout: informe ao cliente e encerre ou continue o loop
            System.err.println("Timeout atingido. Encerrando cliente.");
            out.println("Tempo de inatividade excedido. Encerrando conexão.");
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    out.println("Saindo...");
                    out.flush();
                    out.close();
                    in.close();
                    socket.close();
                    System.out.println("Cliente desconectado.");
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }

    private void handleLogin(String[] parts, PrintWriter out) {
        if (parts.length == 3 && "login".equals(parts[0])) {
            if (users.containsKey(parts[1]) && users.get(parts[1]).equals(parts[2])) {
                authenticated = true;
                username = parts[1];
                out.println("Login bem-sucedido! Bem vindo " + username + ".");
            } else {
                out.println("Usuário ou senha incorretos.");
            }
        } else {
            out.println("Comando inválido. Faça login primeiro (login usuario senha).");
        }
    }
}
