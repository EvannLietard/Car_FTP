import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientCommand {

    private boolean isAuthenticated = false;
    private String receivedLogin = null;
    private String currentdirectoryPath = "";
    private String Basepath;
    private ServerSocket dataServer;


    private void handleAuthentication(String command, OutputStream commandOut) throws IOException {
        if (command.startsWith("USER ")) {
            receivedLogin = command.split(" ")[1].trim();
            if (Server.USERS.containsKey(receivedLogin)) {
                sendResponse(commandOut, "331 Password required\r\n");
            } else {
                sendResponse(commandOut, "530 User not found\r\n");
            }
        } else if (command.startsWith("PASS ")) {
            String receivedPassword = command.substring(5).trim();
            if (receivedLogin != null && Server.USERS.containsKey(receivedLogin)) {
                String correctPassword = Server.USERS.get(receivedLogin);
                if (correctPassword.equals(receivedPassword)) {
                    isAuthenticated = true;
                    Basepath = "ressources/users/" + receivedLogin;
                    currentdirectoryPath = Basepath;
                    sendResponse(commandOut, "230 User logged in, proceed\r\n");
                } else {
                    sendResponse(commandOut, "530 Incorrect password\r\n");
                }
            } else {
                sendResponse(commandOut, "530 Please login with USER and PASS\r\n");
            }
        } else {
            sendResponse(commandOut, "530 Please login with USER and PASS\r\n");
        }
    }


    public void handleCommand(String command, OutputStream commandOut) throws IOException {
        if (!isAuthenticated) {
            handleAuthentication(command, commandOut);
        } else {
            processCommand(command, commandOut);
        }
    }

    public boolean processCommand(String command, OutputStream commandOut) throws IOException {
        if (command.equalsIgnoreCase("QUIT")) {
            sendResponse(commandOut, "221 Goodbye\r\n");
            isAuthenticated=false;
        } else if (command.startsWith("EPSV")) {
            handleEPSV(commandOut);
        } else if (command.startsWith("RETR")) {
            handleRETR(command,commandOut);
        } else if (command.startsWith("LIST")) {
            handleLIST(command,commandOut);
        } else if (command.startsWith("CWD")) {
            handleCWD(command, commandOut);
        } else {
            sendResponse(commandOut, "502 Command not implemented\r\n");
        }
        return true;
    }

    private void handleEPSV(OutputStream commandOut) throws IOException {
        try {
            dataServer = new ServerSocket(0); // Crée un serveur sur un port libre
            String response = String.format("229 Entering Extended Passive Mode (|||%d|)\r\n", dataServer.getLocalPort());
            sendResponse(commandOut, response);
        } catch (IOException e) {
            sendResponse(commandOut, "425 Can't open data connection.\r\n");
            System.out.println("Erreur de création du serveur de données : " + e.getMessage());
        }
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
        if(newDirectory.getCanonicalPath().startsWith(new File(Basepath).getCanonicalPath())) {
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
}
