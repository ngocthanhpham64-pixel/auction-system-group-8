package vn.edu.vnu.uet.group8.common.entity;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.SpecKey;
import vn.edu.vnu.uet.group8.common.interfaces.SpecAccessor;

/**
 * Lớp biểu diễn thông tin vật lý của một sản phẩm trên hệ thống.
 */
public final class Item extends Entity implements SpecAccessor {

  // ── Immutable sau khi tạo ────────────────────────────
  private final int sellerId;
  private final String title;
  private final ItemCategory category;

  // ── Mutable có kiểm soát ─────────────────────────────
  private String description;
  private ItemCondition condition;
  private Map<String, String> specs;

  private Item(Builder b) {
    super(0, Instant.now(), false);
    this.sellerId = b.sellerId;
    this.title = b.title;
    this.category = b.category;
    this.description = b.description != null ? b.description : "";
    this.condition = b.condition;
    this.specs = b.specs != null ? new HashMap<>(b.specs) : new HashMap<>();
  }

  private Item(Reconstructor r) {
    super(r.id, r.createdAt, r.isDeleted);
    this.sellerId = r.sellerId;
    this.title = r.title;
    this.category = r.category;
    this.description = r.description;
    this.condition = r.condition;
    this.specs = r.specs != null ? new HashMap<>(r.specs) : new HashMap<>();
  }

  public static Reconstructor reconstructor() {
    return new Reconstructor();
  }

  // ════════════════════════════════════════════════════
  // RECONSTRUCTOR
  // ════════════════════════════════════════════════════
  public static class Reconstructor {
    private Integer id;
    private Instant createdAt;
    private Boolean isDeleted;
    private Integer sellerId;
    private String title;
    private String description;
    private ItemCondition condition;
    private ItemCategory category;
    private Map<String, String> specs;

    public Reconstructor id(int id) { this.id = id; return this; }
    public Reconstructor createdAt(Instant v) { this.createdAt = v; return this; }
    public Reconstructor isDeleted(boolean v) { this.isDeleted = v; return this; }
    public Reconstructor sellerId(int v) { this.sellerId = v; return this; }
    public Reconstructor title(String v) { this.title = v; return this; }
    public Reconstructor description(String v) { this.description = v; return this; }
    public Reconstructor condition(ItemCondition v) { this.condition = v; return this; }
    public Reconstructor category(ItemCategory v) { this.category = v; return this; }
    public Reconstructor specs(Map<String, String> v) { this.specs = v; return this; }

    public Item build() {
      requireNonNull(id, "id");
      requireNonNull(createdAt, "createdAt");
      requireNonNull(isDeleted, "isDeleted");
      requireNonNull(sellerId, "sellerId");
      requireNonNull(title, "title");
      requireNonNull(category, "category");
      requireNonNull(specs, "specs");
      return new Item(this);
    }

    private void requireNonNull(Object value, String fieldName) {
      if (value == null)
        throw new IllegalStateException("Reconstructor thiếu field bắt buộc: [" + fieldName + "].");
    }
  }

  // ════════════════════════════════════════════════════
  // BUILDER
  // ════════════════════════════════════════════════════
  public static class Builder {
    private final int sellerId;
    private final String title;
    private final ItemCategory category;

    private String description = "";
    private ItemCondition condition = ItemCondition.USED;
    private Map<String, String> specs = new HashMap<>();

    public Builder(int sellerId, String title, ItemCategory category) {
      if (sellerId <= 0) throw new IllegalArgumentException("sellerId không tồn tại");
      if (title == null || title.isBlank()) throw new IllegalArgumentException("Tiêu đề item không được trống");
      if (category == null) throw new IllegalArgumentException("Category không được null");
      
      this.sellerId = sellerId;
      this.title = title.trim();
      this.category = category;
    }

    public Builder description(String description) {
      this.description = description != null ? description.trim() : "";
      return this;
    }

    public Builder condition(ItemCondition condition) {
      this.condition = condition != null ? condition : ItemCondition.USED;
      return this;
    }

    public Builder specs(Map<String, String> specs) {
      this.specs = specs != null ? new HashMap<>(specs) : new HashMap<>();
      return this;
    }

    public Builder putSpecs(String key, String value) {
      if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
        this.specs.put(key, value);
      }
      return this;
    }

    public Item build() {
      return new Item(this);
    }
  }

  // ════════════════════════════════════════════════════
  // GETTERS
  // ════════════════════════════════════════════════════
  public int getSellerId() { return sellerId; }
  public String getTitle() { return title; }
  public ItemCategory getCategory() { return category; }
  public String getDescription() { return description; }
  public ItemCondition getCondition() { return condition; }
  public Map<String, String> getSpecs() { return Collections.unmodifiableMap(specs); }

  // ════════════════════════════════════════════════════
  // SPEC HELPERS
  // ════════════════════════════════════════════════════
  public String getSpecs(SpecKey key) { return specs.getOrDefault(key.name(), ""); }
  public String getSpecs(String rawKey) { return specs.getOrDefault(rawKey, ""); }
  public boolean hasSpec(SpecKey key) { return specs.containsKey(key.name()); }
  public boolean hasSpec(String rawKey) { return specs.containsKey(rawKey); }
  @Override
  public Map<String, String> getRawSpecs() { return Collections.unmodifiableMap(specs); }

  // ════════════════════════════════════════════════════
  // SETTERS CÓ KIỂM SOÁT
  // ════════════════════════════════════════════════════
  public void setDescription(String description) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Mô tả không được trống");
    }
    this.description = description.trim();
  }

  public void setCondition(ItemCondition condition) {
    if (condition == null) {
      throw new IllegalArgumentException("Condition không được null");
    }
    this.condition = condition;
  }

  public void putSpecs(SpecKey key, String value) {
    if (key == null) return;
    if (value != null && !value.isBlank()) {
      specs.put(key.name(), value.trim());
    }
  }

  public void putCustomSpec(String rawKey, String value) {
    specs.put("custom_" + rawKey, value);
  }

  // ════════════════════════════════════════════════════
  // OVERRIDE
  // ════════════════════════════════════════════════════
  @Override
  public String toString() {
    return "Item{" +
            "id='"           + getId()        + '\'' +
            ", title='"      + title          + '\'' +
            ", category="    + category       +
            ", isDeleted="   + isDeleted()    +
            '}';
  }
}