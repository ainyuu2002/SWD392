package tictactoe;

/** Entity: a player's identity and current symbol within a game. */
public class PlayerInfo {
    private final String name;
    private Mark symbol = Mark.EMPTY;

    public PlayerInfo(String name) {
        this.name = name;
    }

    public String name() { return name; }
    public Mark symbol() { return symbol; }
    public void setSymbol(Mark symbol) { this.symbol = symbol; }
}
