package com.projectsync.backend.vcs.dto;

import java.util.ArrayList;
import java.util.List;

public class StageFilesRequest {

    private List<String> filePaths = new ArrayList<>();

    public StageFilesRequest() {
    }

    public StageFilesRequest(List<String> filePaths) {
        this.filePaths = filePaths != null ? filePaths : new ArrayList<>();
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }
}
