package tictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Boundary (proxy): the server side of one client's TCP socket. Runs as its own
 * task (thread): reads command lines and forwards them to the RoomManager;
 * implements ClientEndpoint so control objects can message and close it.
 */
public class PlayerSession implements ClientEndpoint, Runnable {
    private final Socket socket;
    private final RoomManager room;
    private final BufferedReader in;
    private final PrintWriter out;

    public PlayerSession(Socket socket, RoomManager room) throws IOException {
        this.socket = socket;
        this.room = room;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    @Override
    public synchronized void send(String message) { out.println(message); }

    @Override
    public void run() {
        try {
            send(Protocol.MSG + " Welcome to Tic-Tac-Toe.");
            send(Protocol.PROMPT + " " + Protocol.P_NAME);
            String line;
            while ((line = in.readLine()) != null) {
                room.onCommand(this, line);
            }
        } catch (IOException ignored) {
            // client dropped
        } finally {
            room.onDisconnect(this);
        }
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
