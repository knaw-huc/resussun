package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Suggest {
    @JsonProperty("service_url")
    private String serviceUrl;

    @JsonProperty("service_path")
    private String servicePath;

    @JsonProperty("flyout_service_url")
    private String flyoutServiceUrl;

    @JsonProperty("flyout_service_path")
    private String flyoutServicePath;

    public Suggest(String serviceUrl, String servicePath) {
        this.serviceUrl = serviceUrl;
        this.servicePath = servicePath;
    }

    public Suggest flyoutServiceUrl(String flyoutServiceUrl) {
        this.flyoutServiceUrl = flyoutServiceUrl;
        return this;
    }

    public Suggest flyoutServicePath(String flyoutServicePath) {
        this.flyoutServicePath = flyoutServicePath;
        return this;
    }
}