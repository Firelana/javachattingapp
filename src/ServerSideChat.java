import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerSideChat {

    // Keeps track of all connected clients (thread-safe for broadcast loops)
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        int port = 5000;

        System.out.println("Starting server on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);

                Thread t = new Thread(handler);
                t.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Static so it can be created from main() easily
    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String msg;

                // Read messages from this client and broadcast to everyone
                while ((msg = in.readLine()) != null) {
                    broadcast(msg);
                }

            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
            } finally {
                closeEverything();
            }
        }

        private void broadcast(String message) {
            for (ClientHandler client : clients) {
                client.out.println(message);
            }
        }

        private void closeEverything() {
            clients.remove(this);

            try { in.close(); } catch (IOException ignored) {}
            out.close();

            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
