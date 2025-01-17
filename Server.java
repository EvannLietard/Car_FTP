import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Server {
    private static final String LOGIN = "Evann";
    private static final String PASSWORD = "LIETARD";
    private ServerSocket serverSocket;
    private ServerSocket dataServer;
    private Socket commandSocket;
    private boolean isAuthenticated = false;
    private String receivedLogin = null;
    private final String  BASEPATH = "ressources/users/" + LOGIN;
    private String currentdirectoryPath = BASEPATH;


    public static void main(String[] args) {
        new Server().startServer();
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(2024);
            commandSocket = serverSocket.accept();
            handleClient();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        } finally {
            closeServer();
        }
    }

    private void handleClient() throws IOException {
        OutputStream commandOut = commandSocket.getOutputStream();
        InputStream commandIn = commandSocket.getInputStream();
        Scanner scanner = new Scanner(commandIn);

        sendResponse(commandOut, "220 Service ready\r\n");

        boolean run = true;
        while (run) {
            String command = scanner.nextLine();
            System.out.println("Reçu: " + command);

            if (!isAuthenticated) {
                run = handleAuthentication(command, commandOut);
            } else {
                run = handleCommand(command, commandOut);
            }
        }
    }

    private boolean handleAuthentication(String command, OutputStream commandOut) throws IOException {
        if (command.startsWith("USER ")) {
            receivedLogin = command.split(" ")[1];
            sendResponse(commandOut, "331 Password required\r\n");
        } else if (command.startsWith("PASS ")) {
            String receivedPassword = command.substring(5).trim();
            if (LOGIN.equals(receivedLogin) && PASSWORD.equals(receivedPassword)) {
                isAuthenticated = true;
                sendResponse(commandOut, "230 User logged in, proceed\r\n");
            } else {
                sendResponse(commandOut, "530 Login incorrect\r\n");
            }
        } else {
            sendResponse(commandOut, "530 Please login with USER and PASS\r\n");
        }
        return true;
    }

    private boolean handleCommand(String command, OutputStream commandOut) throws IOException {
        if (command.equalsIgnoreCase("QUIT")) {
            sendResponse(commandOut, "221 Goodbye\r\n");
            return false;
        } else if (command.equalsIgnoreCase("EPSV")) {
            handleEPSV(commandOut);
        } else if (command.startsWith("RETR ")) {
            handleRETR(command, commandOut);
        } else if (command.startsWith("LIST")) {
            handleLIST(command, commandOut);
        } else if (command.startsWith("CWD")) {
            handleCWD(command, commandOut);
        } else {
            sendResponse(commandOut, "502 Command not implemented\r\n");
        }
        return true;
    }

    private void handleEPSV(OutputStream commandOut) throws IOException {
        dataServer = new ServerSocket(0);
        String response = String.format("229 Entering Extended Passive Mode (|||%d|)\r\n", dataServer.getLocalPort());
        sendResponse(commandOut, response);
    }

    private void handleRETR(String command, OutputStream commandOut) throws IOException {
        String filePath = currentdirectoryPath+"/"+ command.split(" ")[1];
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            sendResponse(commandOut, "150 File status okay; about to open data connection.\r\n");
            try (Socket dataSocket = dataServer.accept(); OutputStream dataOut = dataSocket.getOutputStream(); FileInputStream fileInput = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }
            }
            sendResponse(commandOut, "226 Closing data connection. File transfer successful.\r\n");
        } else {
            sendResponse(commandOut, "550 File not found or access denied.\r\n");
        }
    }

    private void handleLIST(String command, OutputStream commandOut) throws IOException {
        if (command.length() > 5) {
            currentdirectoryPath += "/" + command.split(" ")[1];
        }
        File directory = new File(currentdirectoryPath);
        if (directory.exists() && directory.isDirectory()) {
            sendResponse(commandOut, "150 Here comes the directory listing.\r\n");
            try (Socket dataSocket = dataServer.accept(); OutputStream dataOut = dataSocket.getOutputStream()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String line = LISTDetails(file);
                        dataOut.write(line.getBytes());
                    }
                }
            }
            sendResponse(commandOut, "226 Directory send okay.\r\n");
        } else {
            sendResponse(commandOut, "550 Directory not found or access denied.\r\n");
        }
    }

    private void handleCWD(String command, OutputStream commandOut) throws IOException {
        String newDirectoryPath = currentdirectoryPath+ "/" + command.split(" ")[1];
        File newDirectory = new File(newDirectoryPath);
        if(newDirectory.getCanonicalPath().startsWith(new File(BASEPATH).getCanonicalPath())) {
            if (newDirectory.exists() && newDirectory.isDirectory()) {
                currentdirectoryPath = newDirectoryPath;
                sendResponse(commandOut, "250 Directory successfully changed \r\n");
            }
            else {
                sendResponse(commandOut, "550 Failed to change directory. Directory not found.\r\n");
            }
        }else {
            sendResponse(commandOut, "550 access denied.\r\n");
        }
    }

    private String LISTDetails(File file) {
        String type = file.isDirectory() ? "<DIR>" : "-";
        long size = file.length();
        String lastModified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()));
        String permissions = (file.canRead() ? "r" : "-") + (file.canWrite() ? "w" : "-") + (file.canExecute() ? "x" : "-");
        return String.format("%s %s %-10d %s %s\r\n", permissions, type, size, lastModified, file.getName());
    }

    private void sendResponse(OutputStream out, String response) throws IOException {
        out.write(response.getBytes());
    }

    private void closeServer() {
        try {
            if (commandSocket != null) commandSocket.close();
            if (serverSocket != null) serverSocket.close();
            if (dataServer != null) dataServer.close();
        } catch (IOException e) {
            System.out.println("Erreur lors de la fermeture du serveur: " + e.getMessage());
        }
    }
}