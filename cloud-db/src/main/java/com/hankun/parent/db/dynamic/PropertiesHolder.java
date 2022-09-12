package com.hankun.parent.db.dynamic;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PropertiesHolder {

    private static final String DS_SPLIT = "_";

    private Map<String, DataSourceProperty> propertyMap;

    public void setPropertyMap(Map<String, DataSourceProperty> map) {
        propertyMap = new HashMap<>(map.size());
        for (Map.Entry<String, DataSourceProperty> entry : map.entrySet()) {
            String alias;
            boolean withSuffix = false;
            if (entry.getKey().contains(DS_SPLIT)) {
                alias = entry.getKey().split(DS_SPLIT)[0];
                withSuffix = true;
            } else {
                alias = entry.getKey();
            }
            if (!(propertyMap.containsKey(alias) && withSuffix)) {
                propertyMap.put(alias, entry.getValue());
            }
        }
        for (Map.Entry<String, DataSourceProperty> entry : propertyMap.entrySet()) {
            DataSourceProperty property = new DataSourceProperty();
            BeanUtils.copyProperties(entry.getValue(), property);
            entry.setValue(property);
        }
    }

    public DataSourceProperty getModel(String key) {
        if (propertyMap.containsKey(key)) {
            DataSourceProperty property = new DataSourceProperty();
            BeanUtils.copyProperties(propertyMap.get(key), property);
            return property;
        }
        return null;
    }
}
