package tictactoe;

import java.io.IOException;

/**
 * Control (coordinator) + entry point: drives the client. The server is
 * authoritative, so the loop simply reacts to each server line; when the server
 * asks (PROMPT), it reads one line from the user via ClientConsoleUI and replies.
 *
 * Usage: java tictactoe.ClientController [host] [port]   (default localhost 5000)
 */
public class ClientController {
    private final ServerConnection server;
    private final ClientConsoleUI ui = new ClientConsoleUI();

    public ClientController(ServerConnection server) { this.server = server; }

    public void run() {
        try {
            String line;
            while ((line = server.receive()) != null) {
                handle(line);
            }
            ui.showMessage("Disconnected from server.");
        } catch (IOException e) {
            ui.showMessage("Connection closed.");
        }
    }

    private void handle(String line) throws IOException {
        int sp = line.indexOf(' ');
        String tag = sp < 0 ? line : line.substring(0, sp);
        String rest = sp < 0 ? "" : line.substring(sp + 1);

        switch (tag) {
            case Protocol.MSG:    ui.showMessage(rest); break;
            case Protocol.SYMBOL: ui.showMessage("You are playing as: " + rest); break;
            case Protocol.BOARD:  ui.renderBoard(rest); break;
            case Protocol.PROMPT: doPrompt(rest); break;
            case Protocol.BYE:    ui.showMessage(rest); server.close(); System.exit(0); break;
            default:              ui.showMessage(line);
        }
    }

    private void doPrompt(String kind) throws IOException {
        switch (kind) {
            case Protocol.P_NAME:
                ui.showMessage("Enter your name:");
                server.send(Protocol.JOIN + " " + safe(ui.readLine()));
                break;
            case Protocol.P_INVITE:
                ui.showMessage("Press Enter to invite, or type 'quit':");
                if (isQuit(ui.readLine())) server.send(Protocol.QUIT);
                else server.send(Protocol.INVITE);
                break;
            case Protocol.P_ACCEPT:
                ui.showMessage("Accept invitation? (yes/no):");
                server.send(isYes(ui.readLine()) ? Protocol.ACCEPT : Protocol.DECLINE);
                break;
            case Protocol.P_MOVE:
                ui.showMessage("Your move - enter \"row col\" (1-3), or 'quit':");
                String mv = ui.readLine();
                if (isQuit(mv)) server.send(Protocol.QUIT);
                else server.send(Protocol.MOVE + " " + (mv == null ? "" : mv.trim()));
                break;
            case Protocol.P_AGAIN:
                ui.showMessage("Play again? (yes/no):");
                server.send(Protocol.AGAIN + " " + (isYes(ui.readLine()) ? "yes" : "no"));
                break;
            default:
                break;
        }
    }

    private boolean isYes(String s)  { return s != null && s.trim().toLowerCase().startsWith("y"); }
    private boolean isQuit(String s) { return s != null && s.trim().toLowerCase().startsWith("q"); }
    private String safe(String s)    { return s == null ? "" : s.trim(); }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        try {
            ServerProxy proxy = new ServerProxy(host, port);
            new ClientController(proxy).run();
        } catch (IOException e) {
            System.out.println("Cannot connect to " + host + ":" + port + " - " + e.getMessage());
        }
    }
}
