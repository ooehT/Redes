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

    public void handleCommand(String command, DataOutputStream dos, Socket socket) {
        String[] parts = command.split(" ");
        handleCommand(parts, dos, socket);
    }

    public void handleCommand(String command, DataOutputStream dos, Socket socket, File currentDirectory) {
        this.currentDirectory = currentDirectory;
        handleCommand(command, dos, socket);
    }

    public void handleCommand(String[] parts, DataOutputStream dos, Socket socket) {
        try {
            switch (parts[0]) {
                case "ls":
                    File[] files = currentDirectory.listFiles();
                    if (files != null && files.length > 0) {
                        for (File file : files) {
                            dos.writeUTF(file.isDirectory() ? "[DIR] " + file.getName() : "[FILE] " + file.getName());
                        }
                    } else {
                        dos.writeUTF("O diretório está vazio.");
                    }
                    break;
                case "cd":
                    if (parts.length < 2) {
                        dos.writeUTF("Erro: Nome do diretório não especificado.");
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
                            dos.writeUTF("Diretório alterado para: " + currentDirectory.getAbsolutePath());
                        } else {
                            dos.writeUTF("Erro: Diretório não encontrado.");
                        }
                    }
                    break;
                case "cd..":
                    File parentDir = currentDirectory.getParentFile();
                    if (parentDir != null) {
                        currentDirectory = parentDir;
                        dos.writeUTF("Diretório alterado para: " + currentDirectory.getAbsolutePath());
                    } else {
                        dos.writeUTF("Erro: Não há diretório pai.");
                    }
                    break;
                case "mkdir":
                    if (parts.length < 2) {
                        dos.writeUTF("Erro: Nome da pasta não especificado.");
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
                            dos.writeUTF("Diretório criado com sucesso: " + parts[1]);
                        } else {
                            dos.writeUTF("Erro: Não foi possível criar o diretório.");
                        }
                    }
                    break;
                case "rmdir":
                    if (parts.length < 2) {
                        dos.writeUTF("Erro: Nome da pasta não especificado.");
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
                            dos.writeUTF("Erro: Diretório não encontrado.");
                        } else if (!dir.isDirectory()) {
                            dos.writeUTF("Erro: O caminho especificado não é um diretório.");
                        } else if (dir.list().length > 0) {
                            dos.writeUTF("Erro: O diretório não está vazio.");
                        } else if (dir.delete()) {
                            dos.writeUTF("Diretório removido com sucesso: " + parts[1]);
                        } else {
                            dos.writeUTF("Erro: Não foi possível remover o diretório.");
                        }
                    }
                    break;
                case "put":
                    if (parts.length < 2) {
                        dos.writeUTF("Erro: Nome do arquivo não especificado.");
                    }
                    else {
                        String fileName = "";
                        for (int i = 1; i < parts.length; i++) {
                            fileName = fileName.concat(parts[i]);
                            if (i < parts.length - 1) {
                                fileName = fileName.concat(" ");
                            }
                        }
                        File destinationFile = new File(currentDirectory, fileName);
                        dos.writeUTF("READY " + fileName);
                        try (DataInputStream dis = new DataInputStream(socket.getInputStream());) {

                            int fileSize = dis.readInt();

                            if (fileSize > 1024 * 1024 * 10) {
                                throw new IOException("Arquivo muito grande.");
                            }
                            else if (fileSize <= 0) {
                                throw new IOException("nBytes <=0");
                            }
                            else {
                                byte[] fileContentBytes = new byte[fileSize];
                                dis.readFully(fileContentBytes, 0, fileSize);
                            }

                            // Confirma que o upload foi bem-sucedido
                            dos.writeUTF("UPLOAD_OK");
                        } catch (IOException e) {
                            // Notifica o cliente em caso de erro
                            dos.writeUTF("UPLOAD_ERROR: " + e.getMessage());
                        }


                    }
                    break;
                case "get":
                    try (DataOutputStream ds = new DataOutputStream(socket.getOutputStream());
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

                            dos.writeUTF("Arquivo enviado com sucesso: " + fileName);
                        } else {
                            dos.writeUTF("Erro: Arquivo não encontrado.");
                        }
                    } catch (IOException e) {
                        dos.writeUTF("Erro ao enviar arquivo: " + e.getMessage());
                    }

                    break;
                default:
                    dos.writeUTF("Comando não reconhecido.");
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar comando: " + e.getMessage());
        }
    }
}

