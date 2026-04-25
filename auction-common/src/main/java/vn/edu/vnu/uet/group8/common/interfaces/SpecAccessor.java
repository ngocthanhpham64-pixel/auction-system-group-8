package vn.edu.vnu.uet.group8.common.interfaces;

import java.util.Map;

import vn.edu.vnu.uet.group8.common.enums.SpecKey;

public interface SpecAccessor {
  
  Map<String, String> getRawSpecs();

  // Hàm default xử lý logic với Enum SpecKey
  default String getSpecs(SpecKey key) {
    java.util.Map<String, String> map = getRawSpecs();
    if (map == null || key == null) return "N/A";
    
    // key.name() biến Enum thành String để tìm trong Map
    return map.getOrDefault(key.name(), "N/A");
  }

  default boolean hasSpec(SpecKey key) {
    java.util.Map<String, String> map = getRawSpecs();
    return map != null && key != null && map.containsKey(key.name());
  }
}
