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
import render.*;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;
import java.util.Map;

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
        testLpcSpritesheetFlyweightCalculations();
        testCompositeLayerZOrdering();
        testLpcCharacterBuilderValidation();
        testCharacterRendererFsmSync();

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

    private static void testLpcSpritesheetFlyweightCalculations() {
        LpcSpriteSheet sheet = new LpcSpriteSheet("male_body", "assets/sprites/body.png");
        // Kiểm tra tính toán toạ độ: WALK, hướng DOWN (hàng 8 + 2 = hàng 10). Frame 3.
        // X = 3 * 64 = 192, Y = 10 * 64 = 640
        Rectangle walkBounds = sheet.getFrameBounds(LpcAnimationType.WALK, Direction.DOWN, 3);
        boolean walkOk = walkBounds.x == 192 && walkBounds.y == 640 && walkBounds.width == 64 && walkBounds.height == 64;

        // SLASH, hướng UP (hàng 12 + 0 = hàng 12). Frame 0.
        // X = 0, Y = 12 * 64 = 768
        Rectangle slashBounds = sheet.getFrameBounds(LpcAnimationType.SLASH, Direction.UP, 0);
        boolean slashOk = slashBounds.x == 0 && slashBounds.y == 768;

        assertTrue("LpcSpriteSheet Flyweight tính đúng ma trận cắt frame 64x64", walkOk && slashOk);
    }

    private static void testCompositeLayerZOrdering() {
        CompositeCharacterVisual visual = new CompositeCharacterVisual();
        LpcSpriteSheet weaponSheet = new LpcSpriteSheet("sword", "weapon.png");
        LpcSpriteSheet bodySheet = new LpcSpriteSheet("body", "body.png");
        LpcSpriteSheet hairSheet = new LpcSpriteSheet("hair", "hair.png");

        // Chèn lộn xộn: Weapon (Z=90) -> Body (Z=20) -> Hair (Z=70)
        visual.setLayer(new SpriteLayer(LayerType.WEAPON, weaponSheet));
        visual.setLayer(new SpriteLayer(LayerType.BODY, bodySheet));
        visual.setLayer(new SpriteLayer(LayerType.HAIR, hairSheet));

        List<SpriteLayer> sorted = visual.getVisibleLayersSorted();
        boolean zOrderCorrect = sorted.size() == 3
                && sorted.get(0).getLayerType() == LayerType.BODY
                && sorted.get(1).getLayerType() == LayerType.HAIR
                && sorted.get(2).getLayerType() == LayerType.WEAPON;

        assertTrue("CompositeCharacterVisual sắp xếp Z-Index chuẩn xác (Body -> Hair -> Weapon)", zOrderCorrect);
    }

    private static void testLpcCharacterBuilderValidation() {
        boolean exceptionThrown = false;
        try {
            // Thiếu Body -> phải ném IllegalStateException
            new LpcCharacterBuilder().withHair("hair.png").build();
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }

        CompositeCharacterVisual character = new LpcCharacterBuilder()
                .withBody("body.png")
                .withShirt("shirt.png")
                .withArmor("plate_armor.png")
                .withWeapon("greatsword.png")
                .build();

        boolean validComposition = character.hasLayer(LayerType.BODY)
                && character.hasLayer(LayerType.ARMOR)
                && character.hasLayer(LayerType.WEAPON)
                && character.getVisibleLayersSorted().size() == 4;

        assertTrue("LpcCharacterBuilder kiểm soát toàn vẹn dữ liệu và lắp ráp Fluent API thành công",
                exceptionThrown && validComposition);
    }

    private static void testCharacterRendererFsmSync() {
        CompositeCharacterVisual visual = new LpcCharacterBuilder().withBody("body.png").build();
        CharacterRenderer renderer = new CharacterRenderer(visual);
        Player player = new Player("p_render", "Knight", new Vector2D(0, 0));

        // 1. Ban đầu IDLE -> Frame 0 của WALK
        renderer.syncFromPlayer(player, 0.016);
        boolean idleOk = renderer.getCurrentAnimation() == LpcAnimationType.WALK && renderer.getCurrentFrameIndex() == 0;

        // 2. Chuyển sang MOVING với vận tốc sang phải (vx=100, vy=0) -> WALK, RIGHT
        player.changeState(new Player.MovingState());
        player.setVelocity(new Vector2D(100, 0));
        renderer.syncFromPlayer(player, 0.016);
        boolean moveOk = renderer.getCurrentDirection() == Direction.RIGHT && renderer.getCurrentAnimation() == LpcAnimationType.WALK;

        // 3. Chuyển sang ATTACKING -> SLASH
        player.changeState(new Player.AttackingState());
        renderer.syncFromPlayer(player, 0.016);
        boolean attackOk = renderer.getCurrentAnimation() == LpcAnimationType.SLASH;

        // 4. Tử vong DEAD -> HURT
        player.takeDamage(999);
        renderer.syncFromPlayer(player, 0.016);
        boolean deadOk = renderer.getCurrentAnimation() == LpcAnimationType.HURT;

        assertTrue("CharacterRenderer đồng bộ mượt mà State Pattern FSM sang hoạt họa LPC",
                idleOk && moveOk && attackOk && deadOk);
    }
}
