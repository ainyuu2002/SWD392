package tictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Boundary (proxy): the client side of the TCP socket. Sends command lines and
 * receives server messages. The single seam between the client and the server.
 */
public class ServerProxy implements ServerConnection {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    public ServerProxy(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    @Override
    public void send(String message) { out.println(message); }

    /** Blocking read of the next server line, or null when the connection closes. */
    @Override
    public String receive() throws IOException { return in.readLine(); }

    @Override
    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
