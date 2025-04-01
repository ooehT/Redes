package servidor;

import java.io.IOException;
import java.net.Socket;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class FTPClientGUI extends JFrame {
    private JTextField serverField, portField, userField, passField;
    private JButton connectButton, disconnectButton;
    private JTextArea logArea;
    private JList<String> localFileList, remoteFileList;
    private DefaultListModel<String> localListModel, remoteListModel;
    private JButton uploadButton, downloadButton, refreshLocalButton, refreshRemoteButton, createFileL, removeFileL ;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private File currentLocalDirectory = new File(".\\src\\client\\VaultC");
    private File currentServerDirectory = new File(".\\src\\servidor\\vaultServer");

    public FTPClientGUI() {
        setTitle("MyFTP Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Painel de conexão
        JPanel connectionPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Conexão"));

        connectionPanel.add(new JLabel("Servidor:"));
        serverField = new JTextField("127.0.0.1");
        connectionPanel.add(serverField);

        connectionPanel.add(new JLabel("Porta:"));
        portField = new JTextField("2121");
        connectionPanel.add(portField);

        connectionPanel.add(new JLabel("Usuário:"));
        userField = new JTextField("admin");
        connectionPanel.add(userField);

        connectionPanel.add(new JLabel("Senha:"));
        passField = new JPasswordField("1234");
        connectionPanel.add(passField);

        connectButton = new JButton("Conectar");
        connectButton.addActionListener(e -> connectToServer());
        connectionPanel.add(connectButton);

        disconnectButton = new JButton("Desconectar");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnectFromServer());
        connectionPanel.add(disconnectButton);

        add(connectionPanel, BorderLayout.NORTH);

        // Painel principal
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Painel de arquivos locais
        JPanel localPanel = new JPanel(new BorderLayout());
        localPanel.setBorder(BorderFactory.createTitledBorder("Arquivos Locais"));

        localListModel = new DefaultListModel<>();
        localFileList = new JList<>(localListModel);
        localFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        localFileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    navigateLocalDirectory();
                }
            }
        });

        JScrollPane localScroll = new JScrollPane(localFileList);
        localPanel.add(localScroll, BorderLayout.CENTER);


        JPanel localButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


        refreshLocalButton = new JButton("Atualizar");
        refreshLocalButton.addActionListener(e -> refreshLocalFiles());
        localButtonPanel.add(refreshLocalButton); // Adiciona ao painel


        createFileL = new JButton("+");
        removeFileL = new JButton("-");
        createFileL.setEnabled(false);
        removeFileL.setEnabled(false);
        removeFileL.addActionListener(e -> removeClickFile());
        createFileL.addActionListener(e -> createNewFile());
        localButtonPanel.add(createFileL); // Adiciona diretamente ao painel
        localButtonPanel.add(removeFileL); // Adiciona diretamente ao painel


        uploadButton = new JButton("Enviar (Upload)");
        uploadButton.setEnabled(false);
        uploadButton.addActionListener(e -> uploadFile());
        localButtonPanel.add(uploadButton); // Adiciona ao painel
        localPanel.add(localButtonPanel, BorderLayout.SOUTH);

        // Painel de arquivos remotos e logs
        JPanel remotePanel = new JPanel(new BorderLayout());

        JPanel remoteFilesPanel = new JPanel(new BorderLayout());
        remoteFilesPanel.setBorder(BorderFactory.createTitledBorder("Arquivos do Servidor"));

        remoteListModel = new DefaultListModel<>();
        remoteFileList = new JList<>(remoteListModel);
        remoteFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        remoteFileList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    navigateServerDirectory();
                }
            }
        });

        JScrollPane remoteScroll = new JScrollPane(remoteFileList);
        remoteFilesPanel.add(remoteScroll, BorderLayout.CENTER);

        JPanel remoteButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshRemoteButton = new JButton("Atualizar");
        refreshRemoteButton.addActionListener(e -> refreshServerFiles());
        remoteButtonPanel.add(refreshRemoteButton);


        downloadButton = new JButton("Baixar (Download)");
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> downloadFile());
        remoteButtonPanel.add(downloadButton);

        remoteFilesPanel.add(remoteButtonPanel, BorderLayout.SOUTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs"));

        JSplitPane remoteSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, remoteFilesPanel, logScroll);
        remoteSplit.setResizeWeight(0.5);

        remotePanel.add(remoteSplit, BorderLayout.CENTER);

        mainPanel.add(localPanel);
        mainPanel.add(remotePanel);

        add(mainPanel, BorderLayout.CENTER);
        // Barra de status
        statusLabel = new JLabel("Desconectado");
        add(statusLabel, BorderLayout.SOUTH);

        // Carrega os arquivos locais e do servidor inicialmente
        refreshLocalFiles();
        refreshServerFiles();
    }

    private void refreshServerFiles() {
        remoteListModel.clear();
        File[] files = currentServerDirectory.listFiles();

        if (files != null) {
            // Adiciona diretório pai (se não for a raiz)
            if (currentServerDirectory.getParentFile() != null && currentServerDirectory.getParentFile().getName().equals("vaultServer")) {
                remoteListModel.addElement("[..]");
            }


            for (File file : files) {
                // Adiciona diretórios
                if (file.isDirectory()) {
                    remoteListModel.addElement("[DIR] " + file.getName());
                }
                // Adiciona arquivos
                else if (file.isFile()) {
                    remoteListModel.addElement(file.getName());
                }
            }

        }
    }

    private void navigateServerDirectory() {
        String selected = remoteFileList.getSelectedValue();
        if (selected == null) return;

        if (selected.equals("[..]")) {
            currentServerDirectory = currentServerDirectory.getParentFile();
        } else if (selected.startsWith("[DIR] ")) {
            String dirName = selected.substring(6);
            currentServerDirectory = new File(currentServerDirectory, dirName);
        } else {
            return; // Não faz nada para arquivos
        }

        refreshServerFiles();
    }

    private void connectToServer() {
        String server = serverField.getText();
        int port = Integer.parseInt(portField.getText());
        String username = userField.getText();
        String password = passField.getText();

        try {
            socket = new Socket(server, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread para receber mensagens do servidor
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> logArea.append(finalLine + "\n"));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> logArea.append("Erro na conexão: " + e.getMessage() + "\n"));
                } finally {
                    disconnectFromServer();
                }
            }).start();

            // Envia comando de login
            out.println("login " + username + " " + password);

            connected = true;
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            uploadButton.setEnabled(true);
            downloadButton.setEnabled(true);
            createFileL.setEnabled(true);
            removeFileL.setEnabled(true);

            statusLabel.setText("Conectado a " + server + ":" + port);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            logArea.append("Erro ao conectar: " + e.getMessage() + "\n");
        }
    }

    private void disconnectFromServer() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logArea.append("Erro ao desconectar: " + e.getMessage() + "\n");
        } finally {
            connected = false;
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            uploadButton.setEnabled(false);
            downloadButton.setEnabled(false);
            createFileL.setEnabled(false);
            removeFileL.setEnabled(false);
            statusLabel.setText("Desconectado");
            logArea.append("Desconectado do servidor\n");
        }
    }

    private void refreshLocalFiles() {
        localListModel.clear();
        File[] files = currentLocalDirectory.listFiles();

        if (files != null) {
            // Adiciona diretório pai (se não for a raiz)
            if (currentLocalDirectory.getParentFile() != null && !(currentLocalDirectory.getParentFile().getName().equals("VaultC")) ) {
                localListModel.addElement("[..]");
            }

            // Adiciona diretórios
            for (File file : files) {
                if (file.isDirectory()) {
                    localListModel.addElement("[DIR] " + file.getName());
                }
            }

            // Adiciona arquivos
            for (File file : files) {
                if (file.isFile()) {
                    localListModel.addElement(file.getName());
                }
            }
        }
    }

    private void navigateLocalDirectory() {
        String selected = localFileList.getSelectedValue();
        if (selected == null) return;

        if (selected.equals("[..]")) {
            currentLocalDirectory = currentLocalDirectory.getParentFile();
        } else if (selected.startsWith("[DIR] ")) {
            String dirName = selected.substring(6);
            currentLocalDirectory = new File(currentLocalDirectory, dirName);
        } else {
            return; // Não faz nada para arquivos
        }

        refreshLocalFiles();
    }

    private void uploadFile() {
        String selected = localFileList.getSelectedValue();
        if (selected == null || selected.startsWith("[") || !connected) {
            JOptionPane.showMessageDialog(this, "Selecione um arquivo para enviar", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File fileToUpload = new File(currentLocalDirectory, selected);
        if (!fileToUpload.exists() || !fileToUpload.isFile()) {
            JOptionPane.showMessageDialog(this, "Arquivo não encontrado", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(fileToUpload);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            // Envia comando PUT
            out.println("put");

            // Envia nome e tamanho do arquivo
            dos.writeUTF(fileToUpload.getName());
            dos.writeLong(fileToUpload.length());

            // Envia o arquivo
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();

            logArea.append("Arquivo enviado: " + fileToUpload.getName() + "\n");
            refreshServerFiles(); // Atualiza a lista de arquivos do servidor após upload

        } catch (IOException e) {
            logArea.append("Erro ao enviar arquivo: " + e.getMessage() + "\n");
        }
    }

    private void downloadFile() {
        String selected = remoteFileList.getSelectedValue();
        if (selected == null || selected.startsWith("[") || !connected) {
            JOptionPane.showMessageDialog(this, "Selecione um arquivo para baixar", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File fileToDownload = new File(currentServerDirectory, selected);
        if (!fileToDownload.exists() || !fileToDownload.isFile()) {
            JOptionPane.showMessageDialog(this, "Arquivo não encontrado no servidor", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileOutputStream fos = new FileOutputStream(new File(currentLocalDirectory, selected));
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            // Envia comando GET
            out.println("get " + selected);

            // Lê o arquivo do servidor
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.flush();

            logArea.append("Arquivo baixado: " + selected + "\n");
            refreshLocalFiles(); // Atualiza a lista de arquivos locais após download

        } catch (IOException e) {
            logArea.append("Erro ao baixar arquivo: " + e.getMessage() + "\n");
        }
    }
    private void removeClickFile(){
        String selected = localFileList.getSelectedValue();

        if(selected.startsWith("[DIR] ")){
            selected = selected.substring("[DIR] ".length());
        }
        File fileToDownload = new File(currentLocalDirectory, selected);
        if (!fileToDownload.exists()) {
            JOptionPane.showMessageDialog(this, "Arquivo não encontrado no servidor", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }


        fileToDownload.delete();
        refreshLocalFiles();
    }

    private void createNewFile(){
        String fileName = JOptionPane.showInputDialog(
                this,                           // Componente pai (janela principal)
                "Digite o nome do arquivo:",    // Mensagem
                "Criar Arquivo",                // Título
                JOptionPane.PLAIN_MESSAGE       // Tipo de mensagem (sem ícone)
        );
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        File newDir = new File(currentLocalDirectory, fileName);
        if (newDir.mkdir()) {
            logArea.append("Pasta criada: " + fileName + "\n");
            refreshLocalFiles(); // Atualiza a lista de arquivos/pastas
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível criar a pasta. Ela já existe ou o nome é inválido.",
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        logArea.append("Arquivo Criado: " + fileName + "\n");

    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FTPClientGUI gui = new FTPClientGUI();
            gui.setVisible(true);
        });
    }
}