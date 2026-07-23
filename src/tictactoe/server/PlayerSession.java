package tictactoe.server;

import tictactoe.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class PlayerSession implements Runnable {
    private final Socket socket;
    private final GameSession gameSession;
    private final BufferedReader in;
    private final PrintWriter out;

    public PlayerSession(Socket socket, GameSession gameSession) throws IOException {
        this.socket = socket;
        this.gameSession = gameSession;
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public synchronized void send(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            send(Protocol.MSG + " Welcome to Tic-Tac-Toe.");
            send(Protocol.PROMPT + " " + Protocol.P_NAME);
            String line;
            while ((line = in.readLine()) != null)
                gameSession.onCommand(this, line);
        } catch (IOException ignored) {
            // A failed read is handled as a disconnect below.
        } finally {
            gameSession.onDisconnect(this);
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing an already closed connection is harmless.
        }
    }
}
