package client.VaultC;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.io.*;
import java.net.Socket;

public class MyftpClient {
    private File serverd;

    public MyftpClient() {
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";
        int serverPort = 2121;
        int dataPort = 2122;
        File localDirectory = new File(".\\src\\client\\VaultC");

        try (
                Socket socket = new Socket(serverAddress, serverPort);
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        ) {

            (new Thread(() -> {
                String serverMessage;
                try {
                    while((serverMessage = dis.readUTF()) != null) {
                        System.out.println(serverMessage);
                        if (serverMessage.equals("Saindo...")) {
                            throw new IOException("Conexão encerrada pelo servidor.");
                        }
                        else if (serverMessage.startsWith("READY")) {
                            String[] parts = serverMessage.split(" ");
                            String fileName = "";
                            for (int i = 1; i < parts.length; i++) {
                                fileName = fileName.concat(parts[i]);
                                if (i < parts.length - 1) {
                                    fileName = fileName.concat(" ");
                                }
                            }
                            File uploadFile = new File(localDirectory, fileName);
                            // Envia o conteúdo do arquivo
                            try (FileInputStream fis = new FileInputStream(uploadFile);) {

                                byte[] fileContentBytes = new byte[(int) uploadFile.length()];
                                fis.read(fileContentBytes);

                                dos.writeInt(fileContentBytes.length);
                                dos.write(fileContentBytes);

                                // Aguarda a confirmação de upload
                                serverMessage = dis.readUTF();
                                if ("UPLOAD_OK".equals(serverMessage)) {
                                    System.out.println("Upload concluído com sucesso.");
                                } else {
                                    System.out.println("Erro no upload: " + serverMessage);
                                }
                            } catch (IOException e) {
                                System.err.println("Erro ao enviar arquivo: " + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

            })).start();

            String userInput;
            while((userInput = console.readLine()) != null) {
                dos.writeUTF(userInput);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}