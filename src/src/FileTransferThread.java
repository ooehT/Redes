package src;

import java.io.*;
import java.net.Socket;

public class FileTransferThread extends Thread {

    private Socket socket;
    private File file;

    public FileTransferThread(Socket socket, File file) {
        this.socket = socket;
        this.file = file;
    }

    @Override
    public void run() {
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                OutputStream outputStream = socket.getOutputStream();
        ) {
            byte[] buffer = new byte[8192]; // Buffer para transferência de dados (pode ajustar o tamanho)
            int bytesRead;

            // Transfere os dados do arquivo para o socket
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush(); // Garante que todos os dados sejam enviados
            System.out.println("Arquivo enviado com sucesso: " + file.getName());
        } catch (IOException e) {
            System.err.println("Erro ao transferir o arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void receiveFile(Socket clientSocket, File outputFile) {
        try (
                InputStream inputStream = clientSocket.getInputStream(); // Fluxo de entrada do cliente
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile); // Fluxo para escrever o arquivo
        ) {
            byte[] buffer = new byte[8192]; // Buffer de leitura (mesmo tamanho usado no envio)
            int bytesRead;

            // Lê os dados do socket e escreve no arquivo
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            fileOutputStream.flush(); // Garante que todos os dados foram escritos no arquivo

        } catch (IOException e) {
            System.err.println("Erro ao receber o arquivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int serverPort = 2121;

        File fileToSend = new File(".\\client\\VaultC\\aaa.txt");

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            // Inicia a thread de transferência de arquivos
            new FileTransferThread(socket, fileToSend).start();
        } catch (IOException e) {
            System.err.println("Erro ao conectar com o servidor: " + e.getMessage());
        }
    }
}

