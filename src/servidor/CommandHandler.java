package servidor;

import java.io.*;
import java.net.Socket;
import java.util.StringTokenizer;

public class CommandHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public CommandHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Comando recebido: " + command);
                handleCommand(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleCommand(String command) {
        StringTokenizer tokenizer = new StringTokenizer(command);
        String action = tokenizer.nextToken();

        switch (action) {
            case "ls":
                listarArquivos();
                break;
            case "cd":
                mudarDiretorio(tokenizer.nextToken());
                break;
            case "mkdir":
                criarDiretorio(tokenizer.nextToken());
                break;
            case "rmdir":
                removerDiretorio(tokenizer.nextToken());
                break;
            case "put":
                receberArquivo(tokenizer.nextToken());
                break;
            case "get":
                enviarArquivo(tokenizer.nextToken());
                break;
            default:
                out.println("Comando inválido!");
        }
    }

    private void listarArquivos() {
        File dir = new File("servidor/");
        String[] arquivos = dir.list();
        if (arquivos != null) {
            for (String arquivo : arquivos) {
                out.println(arquivo);
            }
        } else {
            out.println("Erro ao listar arquivos.");
        }
    }

    private void mudarDiretorio(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            System.setProperty("user.dir", dir.getAbsolutePath());
            out.println("Diretório alterado para: " + dir.getAbsolutePath());
        } else {
            out.println("Diretório não encontrado.");
        }
    }

    private void criarDiretorio(String dirName) {
        File dir = new File("servidor/" + dirName);
        if (dir.mkdir()) {
            out.println("Diretório criado: " + dirName);
        } else {
            out.println("Erro ao criar diretório.");
        }
    }

    private void removerDiretorio(String dirName) {
        File dir = new File("servidor/" + dirName);
        if (dir.isDirectory() && dir.list().length == 0 && dir.delete()) {
            out.println("Diretório removido: " + dirName);
        } else {
            out.println("Erro ao remover diretório.");
        }
    }

    private void receberArquivo(String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream("servidor/" + fileName);
            BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            out.println("Arquivo recebido: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarArquivo(String fileName) {
        try {
            File file = new File("servidor/" + fileName);
            if (!file.exists()) {
                out.println("Arquivo não encontrado.");
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(clientSocket.getOutputStream());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, bytesRead);
            }

            bos.flush();
            fis.close();
            out.println("Arquivo enviado: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
