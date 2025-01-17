import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static final int PORT =2024;
    public static final Map<String, String> USERS = new HashMap<>();

    static {
        USERS.put("Evann", "LIETARD");
        USERS.put("miage", "miage");
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println("Erreur du serveur : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Server().startServer();
    }
}
