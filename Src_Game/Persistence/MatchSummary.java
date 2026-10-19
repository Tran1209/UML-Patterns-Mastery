package persistence;

import java.util.List;

/**
 * Đối tượng DTO chứa dữ liệu tổng kết trận đấu.
 */
public class MatchSummary {
    private final String matchId;
    private final String winnerId;
    private final int durationSeconds;
    private final List<String> participants;

    public MatchSummary(String matchId, String winnerId, int durationSeconds, List<String> participants) {
        this.matchId = matchId;
        this.winnerId = winnerId;
        this.durationSeconds = durationSeconds;
        this.participants = participants;
    }

    public String getMatchId() { return matchId; }
    public String getWinnerId() { return winnerId; }
    public int getDurationSeconds() { return durationSeconds; }
    public List<String> getParticipants() { return participants; }
}
