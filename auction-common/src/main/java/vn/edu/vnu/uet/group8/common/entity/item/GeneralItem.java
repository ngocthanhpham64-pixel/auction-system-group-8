package vn.edu.vnu.uet.group8.common.entity.item;

import java.util.HashMap;
import java.util.Map;

import vn.edu.vnu.uet.group8.common.entity.Item;

public class GeneralItem extends Item {
  private String brand;
  private String model;

  private Map<String, String> additionalAttributes = new HashMap<>();

  public GeneralItem() {
    super();
  }

  public void addAdditionalAttribute(String key, String value) {
    additionalAttributes.put(key, value);
  }

  public String getAdditionalAttribute(String key) {
    return additionalAttributes.getOrDefault(key, "N/A");
  }

  public void setSpecificaAttributes(Map<String, String> attributes) {
    this.additionalAttributes = attributes;
  }

  public String getBrand() {
    return brand;
  }

  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }
}
