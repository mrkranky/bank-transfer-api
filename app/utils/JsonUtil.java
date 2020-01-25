package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import play.Logger;

@Singleton
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T parseJson(JsonNode json, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(json, clazz);
        } catch (Exception e) {
            Logger.error("Failed to parse JSON {} for class: {}", json.toString(), clazz.getCanonicalName());
            Logger.error("Json Parse Exception Stack: ", e);
            throw new RuntimeException("Json parsing failed - " + e);
        }
    }
}
