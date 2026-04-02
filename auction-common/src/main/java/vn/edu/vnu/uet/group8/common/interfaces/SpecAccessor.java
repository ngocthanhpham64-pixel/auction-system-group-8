package vn.edu.vnu.uet.group8.common.interfaces;

import java.util.Map;

import vn.edu.vnu.uet.group8.common.enums.SpecKey;

public interface SpecAccessor {
  Map<String, String> getSpecs();

    default String getSpec(SpecKey key) {
        return getSpecs().getOrDefault(key.name(), "");
    }

    default boolean hasSpec(SpecKey key) {
        return getSpecs().containsKey(key.name());
    }
}

