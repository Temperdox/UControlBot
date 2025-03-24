package com.cottonlesergal.ucontrolbot.models.dto;

import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

public abstract class BaseDTO implements DiscordDTO {
    protected Map<String, Object> data = new HashMap<>();
    private static final Gson gson = new Gson();

    @Override
    public String toJson() {
        return gson.toJson(data);
    }

    @Override
    public void fromJson(String json) {
        // This assumes the JSON structure matches your Map<String, Object>
        // You might need more complex deserialization logic for nested objects
        data = gson.fromJson(json, Map.class);
    }

    @Override
    public Object get(String property) {
        return data.get(property);
    }

    @Override
    public void set(String property, Object value) {
        data.put(property, value);
    }

    // Additional helper methods
    protected void copyFrom(BaseDTO other) {
        data.putAll(other.data);
    }
}