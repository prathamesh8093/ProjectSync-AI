package com.projectsync.backend.vcs.dto;

import java.util.ArrayList;
import java.util.List;

public class CommitDetailsDto {

    private CommitDto commit;
    private List<String> parentHashes = new ArrayList<>();
    private List<CommitFileDiffDto> filesChanged = new ArrayList<>();
    private int totalAdditions;
    private int totalDeletions;

    public CommitDetailsDto() {
    }

    public CommitDetailsDto(
            CommitDto commit,
            List<String> parentHashes,
            List<CommitFileDiffDto> filesChanged,
            int totalAdditions,
            int totalDeletions) {
        this.commit = commit;
        this.parentHashes = parentHashes != null ? parentHashes : new ArrayList<>();
        this.filesChanged = filesChanged != null ? filesChanged : new ArrayList<>();
        this.totalAdditions = totalAdditions;
        this.totalDeletions = totalDeletions;
    }

    public CommitDto getCommit() {
        return commit;
    }

    public void setCommit(CommitDto commit) {
        this.commit = commit;
    }

    public List<String> getParentHashes() {
        return parentHashes;
    }

    public void setParentHashes(List<String> parentHashes) {
        this.parentHashes = parentHashes;
    }

    public List<CommitFileDiffDto> getFilesChanged() {
        return filesChanged;
    }

    public void setFilesChanged(List<CommitFileDiffDto> filesChanged) {
        this.filesChanged = filesChanged;
    }

    public int getTotalAdditions() {
        return totalAdditions;
    }

    public void setTotalAdditions(int totalAdditions) {
        this.totalAdditions = totalAdditions;
    }

    public int getTotalDeletions() {
        return totalDeletions;
    }

    public void setTotalDeletions(int totalDeletions) {
        this.totalDeletions = totalDeletions;
    }
}
