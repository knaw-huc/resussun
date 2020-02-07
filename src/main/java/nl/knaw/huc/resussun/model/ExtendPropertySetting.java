package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;
import java.util.TreeMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtendPropertySetting<T> {
  @JsonProperty
  private String name;

  @JsonProperty
  private String label;

  @JsonProperty
  private String type;

  @JsonProperty("default")
  private T defaultValue;

  @JsonProperty("help_text")
  private String helpText;

  @JsonProperty
  @JsonSerialize(using = MapToListSerializer.MapToChoicesList.class)
  private Map<String, String> choices;

  public ExtendPropertySetting(String name, String label, String type, T defaultValue, String helpText) {
    this.name = name;
    this.label = label;
    this.type = type;
    this.defaultValue = defaultValue;
    this.helpText = helpText;
  }

  public ExtendPropertySetting<T> choice(String value, String name) {
    if (choices == null) {
      choices = new TreeMap<>();
    }

    choices.put(value, name);
    return this;
  }
}
