package tictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Boundary (user interaction): the only class that touches the console.
 * Reads the player's input and renders messages / the board.
 */
public class ClientConsoleUI {
    private final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    public String readLine() throws IOException { return stdin.readLine(); }

    public void showMessage(String text) { System.out.println(text); }

    /** Render a 9-char row-major snapshot ("X.O..X..O") as a labelled 3x3 grid. */
    public void renderBoard(String cells) {
        if (cells.length() < 9) return;
        System.out.println();
        System.out.println("       c1  c2  c3");
        for (int r = 0; r < 3; r++) {
            StringBuilder sb = new StringBuilder("  r" + (r + 1) + "  ");
            for (int c = 0; c < 3; c++) {
                char ch = cells.charAt(r * 3 + c);
                sb.append("[ ").append(ch == '.' ? ' ' : ch).append(" ]");
            }
            System.out.println(sb);
        }
        System.out.println();
    }
}
