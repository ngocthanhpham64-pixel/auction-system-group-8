package vn.edu.vnu.uet.group8.common.dto.request;

import java.math.BigDecimal;
import java.util.List;

public class CreateItemRequest {
  public String name;
  public String category;
  public String condition;
  public String description;
  public BigDecimal startPrice;
  public BigDecimal bidStep;
  public int durationMinutes;
  public Long startTime; // Unix timestamp in milliseconds
  // public Map<String, String> specs;  // brand, model, year, material, origin
  public List<String> imageUrls;

  public CreateItemRequest(String name, String category, String condition,
                              String description, BigDecimal startPrice,
                              BigDecimal bidStep, int durationMinutes,
                              Long startTime,
                              // Map<String, String> specs,
                              List<String> imageUrls) {
    this.name = name;
    this.category = category;
    this.condition = condition;
    this.description = description;
    this.startPrice = startPrice;
    this.bidStep = bidStep;
    this.durationMinutes = durationMinutes;
    this.startTime = startTime;
    // this.specs = specs;
    this.imageUrls = imageUrls;
  }
}
