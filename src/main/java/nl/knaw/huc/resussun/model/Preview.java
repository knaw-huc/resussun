package nl.knaw.huc.resussun.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Preview {
    @JsonProperty
    private String url;

    @JsonProperty
    private int width;

    @JsonProperty
    private int height;

    public Preview(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }
}