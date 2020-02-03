package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extend {
    @JsonProperty("propose_properties")
    private ExtendProposeProperties proposeProperties;

    @JsonProperty("property_settings")
    private List<ExtendPropertySetting> propertySettings;

    public Extend proposeProperties(String serviceUrl, String servicePath) {
        proposeProperties = new ExtendProposeProperties(serviceUrl, servicePath);
        return this;
    }

    public Extend propertySetting(ExtendPropertySetting propertySetting) {
        if (propertySettings == null)
            propertySettings = new ArrayList<>();

        this.propertySettings.add(propertySetting);
        return this;
    }

    private static final class ExtendProposeProperties {
        @JsonProperty("service_url")
        private String serviceUrl;

        @JsonProperty("service_path")
        private String servicePath;

        public ExtendProposeProperties(String serviceUrl, String servicePath) {
            this.serviceUrl = serviceUrl;
            this.servicePath = servicePath;
        }
    }
}