package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceManifest {
  @JsonProperty
  private String name;

  @JsonProperty
  private String identifierSpace;

  @JsonProperty
  private String schemaSpace;

  @JsonProperty
  @JsonSerialize(using = MapToListSerializer.MapToTypeList.class)
  private Map<String, String> defaultTypes;

  @JsonProperty("view")
  @JsonSerialize(using = ViewSerializer.class)
  private String viewUrl;

  @JsonProperty
  private Preview preview;

  @JsonProperty
  private SuggestServices suggest;

  @JsonProperty
  private Extend extend;

  public ServiceManifest(String name, String identifierSpace, String schemaSpace) {
    this.name = name;
    this.identifierSpace = identifierSpace;
    this.schemaSpace = schemaSpace;
  }

  public ServiceManifest defaultType(String id, String name) {
    if (defaultTypes == null) {
      defaultTypes = new HashMap<>();
    }

    defaultTypes.put(id, name);
    return this;
  }

  public ServiceManifest viewUrl(String viewUrl) {
    this.viewUrl = viewUrl;
    return this;
  }

  public ServiceManifest preview(Preview preview) {
    this.preview = preview;
    return this;
  }

  public ServiceManifest suggestEntity(Suggest suggestEntity) {
    if (suggest == null) {
      suggest = new SuggestServices();
    }

    suggest.entity = suggestEntity;
    return this;
  }

  public ServiceManifest suggestProperty(Suggest suggestProperty) {
    if (suggest == null) {
      suggest = new SuggestServices();
    }

    suggest.property = suggestProperty;
    return this;
  }

  public ServiceManifest suggestType(Suggest suggestType) {
    if (suggest == null) {
      suggest = new SuggestServices();
    }

    suggest.type = suggestType;
    return this;
  }

  public ServiceManifest extend(Extend extend) {
    this.extend = extend;
    return this;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private static final class SuggestServices {
    @JsonProperty
    private Suggest entity;

    @JsonProperty
    private Suggest property;

    @JsonProperty
    private Suggest type;
  }

  private static final class ViewSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String viewUrl, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      gen.writeStartObject();
      gen.writeObjectField("url", viewUrl);
      gen.writeEndObject();
    }
  }
}
