package tictactoe.server;

import tictactoe.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public final class GameServer {
    private final int port;
    private final GameSession gameSession = new GameSession();

    public GameServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Tic-Tac-Toe server listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    PlayerSession playerSession = new PlayerSession(socket, gameSession);
                    if (gameSession.register(playerSession)) {
                        new Thread(playerSession).start();
                    } else {
                        playerSession.send(Protocol.BYE
                                + " Room is full. Try again later.");
                        playerSession.close();
                    }
                } catch (IOException error) {
                    socket.close();
                }
            }
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        try {
            new GameServer(port).start();
        } catch (IOException error) {
            System.err.println("Server error: " + error.getMessage());
        }
    }
}
