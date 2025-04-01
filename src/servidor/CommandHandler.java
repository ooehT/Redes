package servidor;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandHandler {
    private File currentDirectory = new File("C:\\Users\\theo1\\IdeaProjects\\Rede de computadors\\src\\client\\VaultC");
    private File serverDirectory = new File("C:\\Users\\theo1\\IdeaProjects\\Rede de computadors\\src\\servidor\\vaultServer");

    public CommandHandler(Socket clientSocket) {
        // Cria os diretórios se não existirem
        if (!currentDirectory.exists()) {
            currentDirectory.mkdir();
        }
        if (!serverDirectory.exists()) {
            serverDirectory.mkdir();
        }
    }

    public void handleCommand(String[] parts, PrintWriter out, Socket socket) {
        try {
            switch (parts[0]) {
                case "ls":
                    File[] files = currentDirectory.listFiles();
                    if (files != null && files.length > 0) {
                        for (File file : files) {
                            out.println(file.isDirectory() ? "[DIR] " + file.getName() : "[FILE] " + file.getName());
                        }
                    } else {
                        out.println("O diretório está vazio.");
                    }
                    break;
                case "cd":
                    if (parts.length < 2) {
                        out.println("Erro: Nome do diretório não especificado.");
                    } else {
                        File newDir = new File(currentDirectory, parts[1]);
                        if (newDir.exists() && newDir.isDirectory()) {
                            currentDirectory = newDir;
                            out.println("Diretório alterado para: " + currentDirectory.getAbsolutePath());
                        } else {
                            out.println("Erro: Diretório não encontrado.");
                        }
                    }
                    break;
                case "cd..":
                    File parentDir = currentDirectory.getParentFile();
                    if (parentDir != null) {
                        currentDirectory = parentDir;
                        out.println("Diretório alterado para: " + currentDirectory.getAbsolutePath());
                    } else {
                        out.println("Erro: Não há diretório pai.");
                    }
                    break;
                case "mkdir":
                    if (parts.length < 2) {
                        out.println("Erro: Nome da pasta não especificado.");
                    } else {
                        File newDir = new File(currentDirectory, parts[1]);
                        if (newDir.mkdir()) {
                            out.println("Diretório criado com sucesso: " + parts[1]);
                        } else {
                            out.println("Erro: Não foi possível criar o diretório.");
                        }
                    }
                    break;
                case "rmdir":
                    if (parts.length < 2) {
                        out.println("Erro: Nome da pasta não especificado.");
                    } else {
                        File dir = new File(currentDirectory, parts[1]);
                        if (!dir.exists()) {
                            out.println("Erro: Diretório não encontrado.");
                        } else if (!dir.isDirectory()) {
                            out.println("Erro: O caminho especificado não é um diretório.");
                        } else if (dir.list().length > 0) {
                            out.println("Erro: O diretório não está vazio.");
                        } else if (dir.delete()) {
                            out.println("Diretório removido com sucesso: " + parts[1]);
                        } else {
                            out.println("Erro: Não foi possível remover o diretório.");
                        }
                    }
                    break;
                case "put":
                    try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                    }catch (IOException e) {
                        out.println("Erro ao receber arquivo: " + e.getMessage());
                        throw e;
                    }

                    break;
                case "get":
                    try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                         DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                        String fileName = dis.readUTF();
                        File file = new File(currentDirectory, fileName);

                        if (file.exists() && file.isFile()) {
                            dos.writeUTF(file.getName());
                            dos.writeLong(file.length());
                            dos.flush();

                            try (FileInputStream fis = new FileInputStream(file);
                                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;

                                while ((bytesRead = bis.read(buffer)) != -1) {
                                    dos.write(buffer, 0, bytesRead);
                                }
                                dos.flush();
                            }

                            out.println("Arquivo enviado com sucesso: " + fileName);
                        } else {
                            out.println("Erro: Arquivo não encontrado.");
                        }
                    } catch (IOException e) {
                        out.println("Erro ao enviar arquivo: " + e.getMessage());
                    }

                    break;
                default:
                    out.println("Comando não reconhecido.");
            }
        } catch (IOException e) {
            out.println("Erro ao processar comando: " + e.getMessage());
        }
    }
}

