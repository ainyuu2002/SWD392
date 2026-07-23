package tictactoe.client;

import tictactoe.Protocol;

import java.io.IOException;

public final class ClientController {
    private final ServerProxy server;
    private final ClientConsoleUI ui = new ClientConsoleUI();

    public ClientController(ServerProxy server) {
        this.server = server;
    }

    public void run() {
        try {
            String line;
            while ((line = server.receive()) != null)
                if (!handle(line)) return;
            ui.showMessage("Disconnected from server.");
        } catch (IOException error) {
            ui.showMessage("Connection closed.");
        } finally {
            server.close();
        }
    }

    private boolean handle(String line) throws IOException {
        int separator = line.indexOf(' ');
        String tag = separator < 0 ? line : line.substring(0, separator);
        String rest = separator < 0 ? "" : line.substring(separator + 1);

        switch (tag) {
            case Protocol.MSG:
                ui.showMessage(rest);
                break;
            case Protocol.SYMBOL:
                ui.showMessage("You are playing as: " + rest);
                break;
            case Protocol.BOARD:
                ui.renderBoard(rest);
                break;
            case Protocol.PROMPT:
                handlePrompt(rest);
                break;
            case Protocol.BYE:
                ui.showMessage(rest);
                return false;
            default:
                ui.showMessage(line);
        }
        return true;
    }

    private void handlePrompt(String kind) throws IOException {
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
                server.send(isYes(ui.readLine())
                        ? Protocol.ACCEPT : Protocol.DECLINE);
                break;
            case Protocol.P_MOVE:
                ui.showMessage("Your move - enter \"row col\" (1-3), or 'quit':");
                String move = ui.readLine();
                if (isQuit(move)) server.send(Protocol.QUIT);
                else server.send(Protocol.MOVE + " "
                        + (move == null ? "" : move.trim()));
                break;
            case Protocol.P_AGAIN:
                ui.showMessage("Play again? (yes/no):");
                server.send(Protocol.AGAIN + " "
                        + (isYes(ui.readLine()) ? "yes" : "no"));
                break;
            default:
                ui.showMessage("Unknown prompt: " + kind);
        }
    }

    private boolean isYes(String value) {
        return value != null
                && value.trim().toLowerCase().startsWith("y");
    }

    private boolean isQuit(String value) {
        return value != null
                && value.trim().toLowerCase().startsWith("q");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        try {
            ServerProxy proxy = new ServerProxy(host, port);
            new ClientController(proxy).run();
        } catch (IOException error) {
            System.out.println("Cannot connect to " + host + ":" + port
                    + " - " + error.getMessage());
        }
    }
}
