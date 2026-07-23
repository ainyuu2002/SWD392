package tictactoe.game;

public final class Player {
    private final String name;
    private Mark mark = Mark.EMPTY;
    private int wins;

    public Player(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public Mark mark() {
        return mark;
    }

    public int wins() {
        return wins;
    }

    public void setMark(Mark mark) {
        this.mark = mark;
    }

    public void recordWin() {
        wins++;
    }
}
