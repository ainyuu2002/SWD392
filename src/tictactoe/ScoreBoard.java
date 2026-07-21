package tictactoe;

import java.util.LinkedHashMap;
import java.util.Map;

/** Entity: session score (wins per player + draws). */
public class ScoreBoard {
    private final Map<String, Integer> wins = new LinkedHashMap<>();
    private int draws = 0;

    public void register(String name) { wins.putIfAbsent(name, 0); }
    public void recordWin(String name) { wins.merge(name, 1, Integer::sum); }
    public void recordDraw() { draws++; }

    public void reset() { wins.clear(); draws = 0; }

    public String standings() {
        StringBuilder sb = new StringBuilder("Score - ");
        for (Map.Entry<String, Integer> e : wins.entrySet())
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("  ");
        sb.append("(draws: ").append(draws).append(")");
        return sb.toString();
    }
}
