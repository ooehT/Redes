package servidor;

import src.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandHandler {
    private File currentDirectory = new File(".\\src\\servidor\\vaultServer");
    private File clientDirectory = new File(".\\src\\client\\VaultC");

    public CommandHandler(Socket clientSocket) {
        // Cria os diretórios se não existirem
        if (!currentDirectory.exists()) {
            currentDirectory.mkdir();
        }
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public File getClientDirectory() {
        return clientDirectory;
    }

    public void setClientDirectory(File clientDirectory) {
        this.clientDirectory = clientDirectory;
    }

    public void handleCommand(String command, PrintWriter out, Socket socket) {
        String[] parts = command.split(" ");
        handleCommand(parts, out, socket);
    }

    public void handleCommand(String command, PrintWriter out, Socket socket, File currentDirectory) {
        this.currentDirectory = currentDirectory;
        handleCommand(command, out, socket);
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
                        String dirName = "";
                        for (int i = 1; i < parts.length; i++) {
                            dirName = dirName.concat(parts[i]);
                            if (i < parts.length - 1) {
                                dirName = dirName.concat(" ");
                            }
                        }
                        File newDir = new File(currentDirectory, dirName);
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
                        String dirName = "";
                        for (int i = 1; i < parts.length; i++) {
                            dirName = dirName.concat(parts[i]);
                            if (i < parts.length - 1) {
                                dirName = dirName.concat(" ");
                            }
                        }
                        File newDir = new File(currentDirectory, dirName);
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
                        String dirName = "";
                        for (int i = 1; i < parts.length; i++) {
                            dirName = dirName.concat(parts[i]);
                            if (i < parts.length - 1) {
                                dirName = dirName.concat(" ");
                            }
                        }
                        File dir = new File(currentDirectory, dirName);
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
                    if (parts.length < 2) {
                        out.println("Erro: Nome do arquivo não especificado.");
                    }
                    else {
                        File file = new File(currentDirectory, parts[1]);
                        if (!file.exists()) {
                            out.println("Erro: Arquivo não encontrado.");
                        } else if (!file.isFile()) {
                            out.println("Erro: O caminho especificado não é um arquivo.");
                        }
                        else
                        {


                        }
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
        } catch (Exception e) {
            out.println("Erro ao processar comando: " + e.getMessage());
        }
    }
}

