import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {

    private Socket clientSocket;
    private ClientCommand clientCommand;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientCommand = new ClientCommand();
    }

    @Override
    public void run() {
        try (InputStream commandIn = clientSocket.getInputStream();
             OutputStream commandOut = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(commandIn))) {

            String command;
            sendResponse(commandOut, "220 Service ready\r\n");

            while ((command = reader.readLine()) != null) {
                System.out.println("Reçu: " + command);

                if (command.equalsIgnoreCase("QUIT")) {
                    sendResponse(commandOut, "221 Goodbye\r\n");
                    clientSocket.close();
                }
                clientCommand.handleCommand(command, commandOut);
            }
        } catch (IOException e) {
            System.out.println("Erreur lors du traitement du client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Erreur lors de la fermeture de la connexion client : " + e.getMessage());
            }
        }
    }

    private void sendResponse(OutputStream out, String response) throws IOException {
        out.write(response.getBytes());
    }
}
