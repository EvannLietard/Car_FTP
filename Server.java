import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private static final String LOGIN = "Evann";
    private static final String PASSWORD = "LIETARD";
    private static ServerSocket dataServer;
    public static void main(String[] args){
        try {
                ServerSocket serv=new ServerSocket(2024);
                Socket commandSocket=serv.accept();
                OutputStream commandOut= commandSocket.getOutputStream();
                String str= "220 Service ready \r\n";
                commandOut.write(str.getBytes());
                InputStream in= commandSocket.getInputStream();
                Scanner scan = new Scanner(in);
            boolean isAuthenticated = false;
            String receivedLogin=null;
            boolean run=true;
            while (run) {
                String command = scan.nextLine();
                System.out.println("Reçu: " + command);
                if (!isAuthenticated) {
                    if (command.startsWith("USER ")) {
                        receivedLogin = command.substring(5).trim();
                        commandOut.write("331 Password required\r\n".getBytes());
                    } else if (command.startsWith("PASS ")) {
                        String receivedPassword = command.substring(5).trim();
                        if (LOGIN.equals(receivedLogin) && PASSWORD.equals(receivedPassword)) {
                            isAuthenticated = true;
                            commandOut.write("230 User logged in, proceed\r\n".getBytes());
                        } else {
                            commandOut.write("530 Login incorrect\r\n".getBytes());
                        }
                    } else {
                        commandOut.write("530 Please login with USER and PASS\r\n".getBytes());
                    }
                } else {
                    if (command.equalsIgnoreCase("QUIT")) {
                        commandSocket.close();
                        serv.close();
                        run=false;
                    } else if (command.equalsIgnoreCase("EPSV")) {
                        dataServer = new ServerSocket(0);
                        String response= String.format("229 Entering Extended Passive Mode (|||%d|)\r\n",dataServer.getLocalPort());
                        commandOut.write(response.getBytes());
                    } else if (command.startsWith("RETR ")) {
                        String filePath = "ressources/users/" + LOGIN + "/" + command.substring(5).trim();
                        System.out.println(filePath);
                        File file = new File(filePath);
                        if (file.exists() && file.isFile()) {
                            commandOut.write("150 File status okay; about to open data connection.\r\n".getBytes());
                            Socket dataSocket = dataServer.accept();
                            OutputStream dataOut = dataSocket.getOutputStream();

                            try (FileInputStream fileInput = new FileInputStream(file)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = fileInput.read(buffer)) != -1) {
                                    dataOut.write(buffer, 0, bytesRead);
                                }
                            }
                            dataSocket.close();
                            dataServer.close();
                            commandOut.write("226 Closing data connection. File transfer successful.\r\n".getBytes());
                            }
                        else {
                            commandOut.write("550 File not found or access denied.\r\n".getBytes());
                        }
                    } else {
                        commandOut.write("502 Command not implemented\r\n".getBytes());
                    }
                }
            }

        }
        catch (IOException e){
            System.out.println("Erreur");
        }

    }
}