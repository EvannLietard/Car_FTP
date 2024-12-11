import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException{
        ServerSocket serv=new ServerSocket(2024);
        Socket s2=serv.accept();

        OutputStream out= s2.getOutputStream();
        String str= "220 Service ready";
        out.write(str.getBytes());

        InputStream in= s2.getInputStream();
        Scanner scan= new Scanner(in);
        String strs = scan.nextLine();

        System.out.println("in" + str);

        s2.close();
        serv.close();
    }
}