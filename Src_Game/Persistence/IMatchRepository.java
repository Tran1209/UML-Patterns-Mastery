package persistence;

import java.util.List;

/**
 * Giao diện Repository lưu trữ theo Martin Fowler (PoEAA).
 * Tách biệt hoàn toàn tầng Domain khỏi cơ chế lưu trữ vật lý.
 */
public interface IMatchRepository {
    boolean saveMatch(MatchSummary summary);
    List<MatchSummary> getRecentMatches();
}
