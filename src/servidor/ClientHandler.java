package client;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean authenticated = false;
    private Map<String, String> users;


    public ClientHandler(Socket socket, Map<String, String> users) {
        this.socket = socket;
        this.users = users;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            this.in = in;
            this.out = out;

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
                    continue;
                }

                // Comandos após autenticação
                switch (parts[0]) {
                    case "put":
                        handlePutCommand(parts, out);
                        break;

                    case "get":
                        handleGetCommand(parts, out);
                        break;
                }
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

    private void handlePutCommand(String[] parts, PrintWriter out) {
        if (parts.length < 2) {
            out.println("Erro: Nome do arquivo não especificado.");
            return;
        }

        String fileName = parts[1];
        File file = new File(fileName);

        if (!file.exists() || !file.isFile()) {
            out.println("Erro: Arquivo não encontrado.");
            return;
        }

        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            // Envia o nome do arquivo
            dos.writeUTF(file.getName());
            // Envia o tamanho do arquivo
            dos.writeLong(file.length());

            // Envia o conteúdo do arquivo
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();

            out.println("Arquivo enviado com sucesso: " + fileName);
        } catch (IOException e) {
            out.println("Erro ao enviar arquivo: " + e.getMessage());
        }
    }

    private void handleGetCommand(String[] parts, PrintWriter out) {
        if (parts.length < 2) {
            out.println("Erro: Nome do arquivo não especificado.");
            return;
        }

        String fileName = parts[1];

        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Envia o nome do arquivo para o servidor
            dos.writeUTF(fileName);
            dos.flush();

            // Recebe o nome e o tamanho do arquivo do servidor
            String receivedFileName = dis.readUTF();
            long fileSize = dis.readLong();

            // Recebe o conteúdo do arquivo
            File file = new File(receivedFileName);
            try (FileOutputStream fos = new FileOutputStream(file);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;

                while (totalRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }

            out.println("Arquivo recebido com sucesso: " + receivedFileName);
        } catch (IOException e) {
            out.println("Erro ao receber arquivo: " + e.getMessage());
        }
    }
}