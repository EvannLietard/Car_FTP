import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private static final String LOGIN = "Evann";
    private static final String PASSWORD = "LIETARD";
    public static void main(String[] args){
        try {
            ServerSocket serv=new ServerSocket(2024);

                Socket s2=serv.accept();
                OutputStream out= s2.getOutputStream();
                String str= "220 Service ready \r\n";
                out.write(str.getBytes());
                InputStream in= s2.getInputStream();
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
                        out.write("331 Password required\r\n".getBytes());
                    } else if (command.startsWith("PASS ")) {
                        String receivedPassword = command.substring(5).trim();
                        if (LOGIN.equals(receivedLogin) && PASSWORD.equals(receivedPassword)) {
                            isAuthenticated = true;
                            out.write("230 User logged in, proceed\r\n".getBytes());
                        } else {
                            out.write("530 Login incorrect\r\n".getBytes());
                        }
                    } else {
                        out.write("530 Please login with USER and PASS\r\n".getBytes());
                    }
                } else {
                    if (command.equalsIgnoreCase("QUIT")) {
                        s2.close();
                        serv.close();
                        run=false;
                    } else {
                        out.write("502 Command not implemented\r\n".getBytes());
                    }
                }
            }

        }
        catch (IOException e){
            System.out.println("Erreur");
        }

    }
}