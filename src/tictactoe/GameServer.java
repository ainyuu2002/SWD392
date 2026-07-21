package tictactoe;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Control (coordinator) + entry point: opens the ServerSocket, accepts clients,
 * and spawns a PlayerSession task per connection. Stays running across sessions.
 *
 * Usage: java tictactoe.GameServer [port]   (default port 5000)
 */
public class GameServer {
    private final int port;
    private final RoomManager room = new RoomManager();

    public GameServer(int port) { this.port = port; }

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Tic-Tac-Toe server listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                try {
                    PlayerSession session = new PlayerSession(socket, room);
                    if (room.register(session)) {
                        new Thread(session).start();
                        System.out.println("[server] client connected: " + socket.getRemoteSocketAddress());
                    } else {
                        session.send(Protocol.BYE + " Room is full. Try again later.");
                        session.close();
                    }
                } catch (IOException e) {
                    socket.close();
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        try {
            new GameServer(port).start();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
