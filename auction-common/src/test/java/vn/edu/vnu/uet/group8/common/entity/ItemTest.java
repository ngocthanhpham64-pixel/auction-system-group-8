package vn.edu.vnu.uet.group8.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import vn.edu.vnu.uet.group8.common.enums.ItemCategory;
import vn.edu.vnu.uet.group8.common.enums.ItemCondition;
import vn.edu.vnu.uet.group8.common.enums.ItemStatus;

/** Test cho {@link Item} entity. */
class ItemTest {

  @Nested
  @DisplayName("Builder pattern")
  class BuilderTest {

    @Test
    @DisplayName("Builder hợp lệ tạo Item thành công với đầy đủ field")
    void builderHopLeTaoItem() {
      Item item =
          new Item.Builder(1, "iPhone 17", ItemCategory.ELECTRONICS)
              .description("Smartphone mới nhất")
              .condition(ItemCondition.NEW)
              .status(ItemStatus.LISTED)
              .build();

      assertEquals(1, item.getSellerId());
      assertEquals("iPhone 17", item.getTitle());
      assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
      assertEquals("Smartphone mới nhất", item.getDescription());
      assertEquals(ItemCondition.NEW, item.getCondition());
      assertEquals(ItemStatus.LISTED, item.getStatus());
    }

    @Test
    @DisplayName("Title bị trim khoảng trắng đầu/cuối")
    void titleBiTrim() {
      Item item = new Item.Builder(1, "  iPhone  ", ItemCategory.ELECTRONICS).build();
      assertEquals("iPhone", item.getTitle());
    }

    @Test
    @DisplayName("Description bị trim khoảng trắng")
    void descriptionBiTrim() {
      Item item =
          new Item.Builder(1, "X", ItemCategory.ELECTRONICS).description("  Mô tả  ").build();
      assertEquals("Mô tả", item.getDescription());
    }

    @Test
    @DisplayName("Status mặc định là DRAFT nếu không set")
    void statusMacDinhDRAFT() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertEquals(ItemStatus.DRAFT, item.getStatus());
    }

    @Test
    @DisplayName("Condition mặc định là USED nếu không set")
    void conditionMacDinhUSED() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertEquals(ItemCondition.USED, item.getCondition());
    }

    @Test
    @DisplayName("Description rỗng nếu không set")
    void descriptionMacDinhRong() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertEquals("", item.getDescription());
    }

    @Test
    @DisplayName("Condition null fallback về USED (qua setter của builder)")
    void conditionNullFallback() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).condition(null).build();
      assertEquals(ItemCondition.USED, item.getCondition());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("sellerId <= 0 phải ném IllegalArgumentException")
    void sellerIdKhongHopLe(int sellerId) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Item.Builder(sellerId, "X", ItemCategory.ELECTRONICS));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Title null/rỗng/blank phải ném IllegalArgumentException")
    void titleKhongHopLe(String title) {
      assertThrows(
          IllegalArgumentException.class,
          () -> new Item.Builder(1, title, ItemCategory.ELECTRONICS));
    }

    @Test
    @DisplayName("Category null phải ném IllegalArgumentException")
    void categoryNull() {
      assertThrows(IllegalArgumentException.class, () -> new Item.Builder(1, "X", null));
    }

    @Test
    @DisplayName("imageUrls hợp lệ - giữ danh sách URL")
    void imageUrlsHopLe() {
      List<String> urls = List.of("http://a/1.png", "http://a/2.png");
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).imageUrls(urls).build();
      assertEquals(2, item.getImageUrls().size());
      assertTrue(item.getImageUrls().contains("http://a/1.png"));
    }

    @Test
    @DisplayName("imageUrls null fallback về list rỗng")
    void imageUrlsNullFallback() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).imageUrls(null).build();
      assertNotNull(item.getImageUrls());
      assertTrue(item.getImageUrls().isEmpty());
    }
  }

  @Nested
  @DisplayName("Reconstructor pattern")
  class ReconstructorTest {

    @Test
    @DisplayName("Reconstructor đầy đủ field tạo được Item từ DB")
    void reconstructorHopLe() {
      java.time.Instant now = java.time.Instant.now();
      Item item =
          Item.reconstructor()
              .id(100)
              .createdAt(now)
              .isDeleted(false)
              .sellerId(1)
              .title("iPhone")
              .description("X")
              .condition(ItemCondition.NEW)
              .category(ItemCategory.ELECTRONICS)
              .imageUrls(new ArrayList<>())
              .status(ItemStatus.LISTED)
              .build();

      assertEquals(100, item.getId());
      assertEquals("iPhone", item.getTitle());
      assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
      assertEquals(ItemStatus.LISTED, item.getStatus());
      assertFalse(item.isDeleted());
    }

    @Test
    @DisplayName("Reconstructor thiếu field bắt buộc phải ném IllegalStateException")
    void reconstructorThieuField() {
      assertThrows(
          IllegalStateException.class,
          () -> Item.reconstructor().id(1).createdAt(java.time.Instant.now()).isDeleted(false).build());
    }

    @Test
    @DisplayName("Reconstructor thiếu status → IllegalStateException")
    void reconstructorThieuStatus() {
      assertThrows(
          IllegalStateException.class,
          () ->
              Item.reconstructor()
                  .id(1)
                  .createdAt(java.time.Instant.now())
                  .isDeleted(false)
                  .sellerId(1)
                  .title("X")
                  .category(ItemCategory.ELECTRONICS)
                  .build());
    }

    @Test
    @DisplayName("Reconstructor isDeleted = true tạo soft-deleted item")
    void reconstructorSoftDeleted() {
      Item item =
          Item.reconstructor()
              .id(1)
              .createdAt(java.time.Instant.now())
              .isDeleted(true)
              .sellerId(1)
              .title("X")
              .category(ItemCategory.ELECTRONICS)
              .condition(ItemCondition.USED)
              .description("")
              .imageUrls(new ArrayList<>())
              .status(ItemStatus.ARCHIVED)
              .build();

      assertTrue(item.isDeleted());
    }
  }

  @Nested
  @DisplayName("imageUrls immutability")
  class ImageUrlsImmutable {

    @Test
    @DisplayName("getImageUrls trả unmodifiable - không sửa được từ ngoài")
    void getImageUrlsImmutable() {
      Item item =
          new Item.Builder(1, "X", ItemCategory.ELECTRONICS).imageUrls(List.of("url1")).build();
      assertThrows(UnsupportedOperationException.class, () -> item.getImageUrls().add("hack"));
    }

    @Test
    @DisplayName("Sửa list gốc KHÔNG ảnh hưởng item (defensive copy)")
    void defensiveCopy() {
      List<String> urls = new ArrayList<>();
      urls.add("url1");
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).imageUrls(urls).build();

      urls.clear();
      assertEquals(1, item.getImageUrls().size(), "Sửa list gốc không ảnh hưởng item");
    }
  }

  @Nested
  @DisplayName("Setters validation")
  class SettersValidation {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("setTitle null/rỗng/blank phải ném exception")
    void setTitleKhongHopLe(String title) {
      Item item = new Item.Builder(1, "Orig", ItemCategory.ELECTRONICS).build();
      assertThrows(IllegalArgumentException.class, () -> item.setTitle(title));
    }

    @Test
    @DisplayName("setTitle hợp lệ phải bị trim")
    void setTitleBiTrim() {
      Item item = new Item.Builder(1, "Orig", ItemCategory.ELECTRONICS).build();
      item.setTitle("  New Title  ");
      assertEquals("New Title", item.getTitle());
    }

    @Test
    @DisplayName("setStatus null phải ném exception")
    void setStatusNull() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertThrows(IllegalArgumentException.class, () -> item.setStatus(null));
    }

    @Test
    @DisplayName("setStatus hợp lệ - thay đổi status")
    void setStatusHopLe() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      item.setStatus(ItemStatus.LISTED);
      assertEquals(ItemStatus.LISTED, item.getStatus());
    }

    @Test
    @DisplayName("setCondition null phải ném exception")
    void setConditionNull() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertThrows(IllegalArgumentException.class, () -> item.setCondition(null));
    }

    @Test
    @DisplayName("setCondition hợp lệ - thay đổi condition")
    void setConditionHopLe() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      item.setCondition(ItemCondition.NEW);
      assertEquals(ItemCondition.NEW, item.getCondition());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("setDescription null/rỗng/blank → ném exception")
    void setDescriptionKhongHopLe(String desc) {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertThrows(IllegalArgumentException.class, () -> item.setDescription(desc));
    }

    @Test
    @DisplayName("setImageUrls null phải ném exception")
    void setImageUrlsNull() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      assertThrows(IllegalArgumentException.class, () -> item.setImageUrls(null));
    }

    @Test
    @DisplayName("setImageUrls hợp lệ - thay danh sách")
    void setImageUrlsHopLe() {
      Item item = new Item.Builder(1, "X", ItemCategory.ELECTRONICS).build();
      item.setImageUrls(List.of("a", "b", "c"));
      assertEquals(3, item.getImageUrls().size());
    }
  }

  @Test
  @DisplayName("toString chứa title và category")
  void toStringFull() {
    Item item = new Item.Builder(1, "iPhone", ItemCategory.ELECTRONICS).build();
    String s = item.toString();
    assertTrue(s.contains("iPhone"));
    assertTrue(s.contains("ELECTRONICS"));
  }
}