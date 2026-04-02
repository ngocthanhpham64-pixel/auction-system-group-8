package vn.edu.vnu.uet.group8.common.entity.item;

import java.util.HashMap;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.entity.Item;

public class UniqueCollectible extends Item {
  private String source;
  private String creationYear;
  private String certificateId;
  private String rarityScore;
  private double conditionScore; // Điểm đánh giá tình trạng từ 0.0 đến 10.0

  private Map<String, String> additionalAttributes = new HashMap<>();

  public UniqueCollectible() {
    super();
  }

  public void addAdditionalAttribute(String key, String value) {
    additionalAttributes.put(key, value);
  }

  public String getAdditionalAttribute(String key) {
    return additionalAttributes.getOrDefault(key, "N/A");
  }

}
