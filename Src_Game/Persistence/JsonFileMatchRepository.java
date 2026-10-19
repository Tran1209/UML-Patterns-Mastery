package persistence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hiện thực lưu trữ lịch sử trận đấu ra file văn bản JSON.
 * Áp dụng nguyên lý Protected Variations của Craig Larman và Data Mapper của Martin Fowler.
 */
public class JsonFileMatchRepository implements IMatchRepository {
    private final String filePath;
    private final List<MatchSummary> inMemoryCache;

    public JsonFileMatchRepository(String filePath) {
        this.filePath = filePath;
        this.inMemoryCache = new ArrayList<>();
    }

    @Override
    public synchronized boolean saveMatch(MatchSummary summary) {
        inMemoryCache.add(summary);
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        // Tạo chuỗi JSON thủ công mà không phụ thuộc thư viện bên ngoài
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append(String.format("  \"matchId\": \"%s\",\n", summary.getMatchId()));
        jsonBuilder.append(String.format("  \"winnerId\": \"%s\",\n", summary.getWinnerId()));
        jsonBuilder.append(String.format("  \"durationSeconds\": %d,\n", summary.getDurationSeconds()));
        jsonBuilder.append("  \"participants\": [");
        for (int i = 0; i < summary.getParticipants().size(); i++) {
            jsonBuilder.append(String.format("\"%s\"", summary.getParticipants().get(i)));
            if (i < summary.getParticipants().size() - 1) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append("]\n");
        jsonBuilder.append("}\n");

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(jsonBuilder.toString());
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi lưu file trận đấu: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<MatchSummary> getRecentMatches() {
        return new ArrayList<>(inMemoryCache);
    }
}
