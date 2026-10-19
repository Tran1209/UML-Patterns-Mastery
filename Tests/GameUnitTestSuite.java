package tests;

import combat.Hitbox;
import combat.IDamageStrategy;
import combat.PhysicalDamageStrategy;
import combat.SpatialGridAoI;
import core.LivingEntity;
import core.Player;
import core.Vector2D;
import network.SnapshotInterpolator;
import persistence.JsonFileMatchRepository;
import persistence.MatchSummary;

import java.io.File;
import java.util.List;

/**
 * Bộ kiểm thử tự động toàn diện (Automated Unit & Netcode Test Suite).
 * Phụ trách: Bùi Minh Hoàng (QA/QC Test Engineer) & Trần Trung Kiên.
 */
public class GameUnitTestSuite {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("🚀 KHỞI ĐỘNG BỘ KIỂM THỬ ĐỒ ÁN OOP (HUCE - 2026)");
        System.out.println("==================================================");

        testVector2DImmutability();
        testLivingEntityDamageAndDeath();
        testHitboxAABBCollision();
        testPlayerStateMachineTransitions();
        testSpatialGridPartitioning();
        testSnapshotInterpolatorLERP();
        testJsonPersistence();

        System.out.println("==================================================");
        System.out.printf("📊 KẾT QUẢ KIỂM THỬ: %d/%d TEST CASES PASSED (%.1f%%)%n",
                testsPassed, testsRun, (testsPassed * 100.0 / testsRun));
        System.out.println("==================================================");
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void testVector2DImmutability() {
        Vector2D v1 = new Vector2D(10, 20);
        Vector2D v2 = v1.add(new Vector2D(5, 5));
        assertTrue("Vector2D Immutability (v1 không bị biến đổi khi cộng)",
                v1.getX() == 10.0 && v1.getY() == 20.0 && v2.getX() == 15.0 && v2.getY() == 25.0);
    }

    private static void testLivingEntityDamageAndDeath() {
        Player player = new Player("p1", "Warrior", new Vector2D(0, 0));
        // player maxHp=100, defense=10.
        // Nhận 30 incomingDamage: effective = 30 - 5 = 25. HP còn 75.
        int dealt = player.takeDamage(30);
        assertTrue("LivingEntity takeDamage tính đúng sát thương trừ giáp", dealt == 25 && player.getCurrentHp() == 75);

        // Nhận thêm 100 sát thương -> HP về 0, chuyển trạng thái tử vong
        player.takeDamage(100);
        assertTrue("LivingEntity tử vong khi HP về 0 và chuyển sang trạng thái DEAD",
                player.getCurrentHp() == 0 && !player.isAlive() && "DEAD".equals(player.getCurrentState().getStateName()));
    }

    private static void testHitboxAABBCollision() {
        Hitbox b1 = new Hitbox(100, 100, 40, 40); // [80, 120] x [80, 120]
        Hitbox b2 = new Hitbox(115, 115, 40, 40); // Giao cắt với b1
        Hitbox b3 = new Hitbox(300, 300, 40, 40); // Cách xa, không giao cắt

        assertTrue("Hitbox AABB phát hiện giao cắt chính xác khi chồng lấn", b1.intersects(b2));
        assertTrue("Hitbox AABB phát hiện không va chạm khi ở xa", !b1.intersects(b3));
    }

    private static void testPlayerStateMachineTransitions() {
        Player player = new Player("p2", "Rogue", new Vector2D(50, 50));
        assertTrue("Trạng thái khởi tạo của nhân vật phải là IDLE", "IDLE".equals(player.getCurrentState().getStateName()));

        player.changeState(new Player.AttackingState());
        assertTrue("Khi ATTACKING, nhân vật tạm thời bị khóa di chuyển", !player.getCurrentState().canMove());

        player.changeState(new Player.DeadState());
        assertTrue("Khi DEAD, nhân vật bị khóa cả di chuyển lẫn tấn công",
                !player.getCurrentState().canMove() && !player.getCurrentState().canAttack());
    }

    private static void testSpatialGridPartitioning() {
        SpatialGridAoI grid = new SpatialGridAoI(128);
        Player nearPlayer = new Player("near", "Nearby", new Vector2D(100, 100));
        Player farPlayer = new Player("far", "FarAway", new Vector2D(1000, 1000));

        grid.insert(nearPlayer);
        grid.insert(farPlayer);

        List<LivingEntity> query = grid.queryNearby(new Vector2D(110, 110));
        assertTrue("SpatialGridAoI lọc đúng thực thể lân cận trong bán kính lân cận",
                query.contains(nearPlayer) && !query.contains(farPlayer));
    }

    private static void testSnapshotInterpolatorLERP() {
        SnapshotInterpolator interpolator = new SnapshotInterpolator(100);
        long now = 1000;
        // Gửi 2 gói tin tại t=900ms và t=1000ms
        interpolator.pushTransform("e1", new Vector2D(0, 0), now - 100);
        interpolator.pushTransform("e1", new Vector2D(100, 0), now);

        // Tại thời điểm render hiện tại = 1050ms, renderTime = 1050 - 100 = 950ms (chính giữa 900 và 1000)
        Vector2D interpolated = interpolator.interpolate("e1", now + 50);
        assertTrue("SnapshotInterpolator LERP nội suy mượt mà tại trung điểm",
                interpolated != null && Math.abs(interpolated.getX() - 50.0) < 1e-3);
    }

    private static void testJsonPersistence() {
        String testPath = "build/test_matches.json";
        JsonFileMatchRepository repo = new JsonFileMatchRepository(testPath);
        MatchSummary summary = new MatchSummary("M-101", "Warrior", 185, List.of("Warrior", "Rogue", "Mage"));

        boolean saved = repo.saveMatch(summary);
        assertTrue("JsonFileMatchRepository lưu thành công bản ghi trận đấu", saved && repo.getRecentMatches().size() == 1);

        // Dọn dẹp file rác
        new File(testPath).delete();
    }
}
