package servidor;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean authenticated = false;
    private Map<String, String> users;
    private String username;
    private CommandHandler commandHandler; // Adicionando CommandHandler

    public ClientHandler(Socket socket, Map<String, String> users) {
        this.socket = socket;
        this.users = users;
        this.commandHandler = new CommandHandler(socket);
    }

    @Override
    public void run() {

        try {
            // Configurando timeout no socket para evitar espera indefinida-
            socket.setSoTimeout(60000); // 60 segundos

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF("Bem-vindo ao MyFTP. Faça login (login usuario senha):");

            while (true) {
                    // Tenta ler do socket
                String command = dis.readUTF();

                // Valida o comando
                if (command == null || command.trim().isEmpty()) {
                    dos.writeUTF("Comando inválido. Tente novamente.");
                    continue;
                }

                String[] parts = command.split(" ");

                if (!authenticated) {
                    handleLogin(parts, dos);
                    continue;
                }

                // Passa os comandos autenticos para o handler
                commandHandler.handleCommand(parts, dos, socket);

            }

        } catch (SocketException e) {
            // Timeout: informe ao cliente e encerre ou continue o loop
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        } finally {
            try {
                if (socket != null) {
                    dos.writeUTF("Saindo...");
                    dos.flush();
                    dos.close();
                    dis.close();
                    socket.close();
                    System.out.println("Cliente desconectado.");
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }

    private void handleLogin(String[] parts, DataOutputStream dos) {
        try {
            if (parts.length == 3 && "login".equals(parts[0])) {
                if (users.containsKey(parts[1]) && users.get(parts[1]).equals(parts[2])) {
                    authenticated = true;
                    username = parts[1];
                    commandHandler.setClientDirectory(new File(commandHandler.getClientDirectory(), username));
                    dos.writeUTF("Login bem-sucedido! Bem vindo " + username);
                } else {
                    dos.writeUTF("Usuário ou senha incorretos.");
                }
            } else {
                dos.writeUTF("Comando inválido. Faça login primeiro (login usuario senha).");
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar login: " + e.getMessage());
        }
    }
}
