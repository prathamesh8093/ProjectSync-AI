package com.projectsync.backend.vcs.dto;

import jakarta.validation.constraints.NotBlank;

public class RemoteConfigDto {

    @NotBlank(message = "Remote name is required")
    private String name = "origin";

    @NotBlank(message = "Remote URL is required")
    private String fetchUrl;

    private String pushUrl;

    public RemoteConfigDto() {
    }

    public RemoteConfigDto(String name, String fetchUrl, String pushUrl) {
        this.name = name;
        this.fetchUrl = fetchUrl;
        this.pushUrl = pushUrl != null ? pushUrl : fetchUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFetchUrl() {
        return fetchUrl;
    }

    public void setFetchUrl(String fetchUrl) {
        this.fetchUrl = fetchUrl;
    }

    public String getPushUrl() {
        return pushUrl;
    }

    public void setPushUrl(String pushUrl) {
        this.pushUrl = pushUrl;
    }
}
