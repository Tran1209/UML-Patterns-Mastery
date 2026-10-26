package render;

/**
 * Mẫu thiết kế Builder Pattern (GoF) hỗ trợ xây dựng ngoại hình nhân vật LPC linh hoạt.
 * Tránh lỗi Telescoping Constructor khi một nhân vật có hàng chục tùy chọn đồ họa.
 */
public class LpcCharacterBuilder {
    private final CompositeCharacterVisual visual;

    public LpcCharacterBuilder() {
        this.visual = new CompositeCharacterVisual();
    }

    public LpcCharacterBuilder withBody(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("body", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.BODY, sheet));
        return this;
    }

    public LpcCharacterBuilder withEyes(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("eyes", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.EYES, sheet));
        return this;
    }

    public LpcCharacterBuilder withHair(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("hair", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.HAIR, sheet));
        return this;
    }

    public LpcCharacterBuilder withPants(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("pants", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.PANTS, sheet));
        return this;
    }

    public LpcCharacterBuilder withShirt(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("shirt", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.SHIRT, sheet));
        return this;
    }

    public LpcCharacterBuilder withArmor(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("armor", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.ARMOR, sheet));
        return this;
    }

    public LpcCharacterBuilder withHelmet(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("helmet", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.HELMET, sheet));
        return this;
    }

    public LpcCharacterBuilder withWeapon(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("weapon", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.WEAPON, sheet));
        return this;
    }

    public LpcCharacterBuilder withShield(String assetPath) {
        LpcSpriteSheet sheet = new LpcSpriteSheet("shield", assetPath);
        visual.setLayer(new SpriteLayer(LayerType.SHIELD, sheet));
        return this;
    }

    public CompositeCharacterVisual build() {
        if (!visual.hasLayer(LayerType.BODY)) {
            throw new IllegalStateException("Nhân vật LPC tối thiểu phải có lớp BODY (Thân thể)!");
        }
        return visual;
    }
}
