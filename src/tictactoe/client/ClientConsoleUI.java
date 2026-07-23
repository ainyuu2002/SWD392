package tictactoe.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ClientConsoleUI {
    private final BufferedReader stdin =
            new BufferedReader(new InputStreamReader(System.in));

    public String readLine() throws IOException {
        return stdin.readLine();
    }

    public void showMessage(String text) {
        System.out.println(text);
    }

    public void renderBoard(String cells) {
        if (cells.length() < 9) return;
        System.out.println();
        System.out.println("       c1  c2  c3");
        for (int row = 0; row < 3; row++) {
            StringBuilder line = new StringBuilder("  r" + (row + 1) + "  ");
            for (int col = 0; col < 3; col++) {
                char value = cells.charAt(row * 3 + col);
                line.append("[ ").append(value == '.' ? ' ' : value).append(" ]");
            }
            System.out.println(line);
        }
        System.out.println();
    }
}
