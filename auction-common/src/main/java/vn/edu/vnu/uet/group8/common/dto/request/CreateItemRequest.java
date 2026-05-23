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
  public int durationHours;
  // public Map<String, String> specs;  // brand, model, year, material, origin
  public List<String> imageUrls;
  public boolean hasCert;
  public String certBody;
  public String certId;

  public CreateItemRequest(String name, String category, String condition,
                              String description, BigDecimal startPrice,
                              BigDecimal bidStep, int durationHours,
                              // Map<String, String> specs,
                              List<String> imageUrls,
                              boolean hasCert, String certBody, String certId) {
    this.name = name;
    this.category = category;
    this.condition = condition;
    this.description = description;
    this.startPrice = startPrice;
    this.bidStep = bidStep;
    this.durationHours = durationHours;
    // this.specs = specs;
    this.imageUrls = imageUrls;
    this.hasCert = hasCert;
    this.certBody = certBody;
    this.certId = certId;
  }
}
