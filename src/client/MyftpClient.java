package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MyftpClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Scanner scanner = new Scanner(System.in);
            System.out.println("Conectado ao servidor MyFTP!");

            while (true) {
                System.out.print("Comando (put, get, ls, cd, mkdir, rmdir, exit): ");
                String command = scanner.nextLine();
                out.println(command);

                if (command.startsWith("put ")) {
                    enviarArquivo(socket, command.substring(4));
                } else if (command.startsWith("get ")) {
                    receberArquivo(socket, command.substring(4));
                } else if (command.equals("exit")) {
                    break;
                } else {
                    String resposta;
                    while ((resposta = in.readLine()) != null) {
                        System.out.println(resposta);
                        if (!in.ready()) break;
                    }
                }
            }

            System.out.println("Cliente desconectado.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void enviarArquivo(Socket socket, String fileName) {
        try {
            File file = new File("cliente/" + fileName);
            if (!file.exists()) {
                System.out.println("Erro: Arquivo não encontrado.");
                return;
            }

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("put " + fileName);

            FileInputStream fis = new FileInputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, bytesRead);
            }

            bos.flush();
            fis.close();
            System.out.println("Arquivo enviado com sucesso: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receberArquivo(Socket socket, String fileName) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("get " + fileName);

            FileOutputStream fos = new FileOutputStream("cliente/" + fileName);
            BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            System.out.println("Arquivo recebido com sucesso: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
