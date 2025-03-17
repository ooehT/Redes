package servidor;

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
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Bem-vindo ao MyFTP. Faça login (login usuario senha):");

            while (true) {
                String command = in.readLine();
                if (command == null) break;

                String[] parts = command.split(" ");
                if (!authenticated) {
                    if (parts.length == 3 && parts[0].equals("login")) {
                        if (users.containsKey(parts[1]) && users.get(parts[1]).equals(parts[2])) {
                            authenticated = true;
                            out.println("Login bem-sucedido!");
                        } else {
                            out.println("Usuário ou senha incorretos.");
                        }
                    } else {
                        out.println("Comando inválido. Faça login primeiro.");
                    }
                    continue;
                }

                CommandHandler.handleCommand(parts, out, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
