package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class MapToListSerializer extends JsonSerializer<Map<String, String>> {
    private final String keyName;
    private final String valueName;

    public MapToListSerializer(String keyName, String valueName) {
        this.keyName = keyName;
        this.valueName = valueName;
    }

    @Override
    public void serialize(Map<String, String> values, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartArray();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            gen.writeStartObject();
            gen.writeObjectField(keyName, entry.getKey());
            gen.writeObjectField(valueName, entry.getValue());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }

    public static final class MapToTypeList extends MapToListSerializer {
        public MapToTypeList() {
            super("id", "name");
        }
    }

    public static final class MapToChoicesList extends MapToListSerializer {
        public MapToChoicesList() {
            super("value", "name");
        }
    }
}
