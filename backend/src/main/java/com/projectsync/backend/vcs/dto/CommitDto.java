package com.projectsync.backend.vcs.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CommitDto {

    private String hash;
    private String shortHash;
    private String message;
    private String authorName;
    private String authorEmail;
    private Instant timestamp;
    private List<String> parentHashes = new ArrayList<>();

    public CommitDto() {
    }

    public CommitDto(
            String hash,
            String shortHash,
            String message,
            String authorName,
            String authorEmail,
            Instant timestamp,
            List<String> parentHashes) {
        this.hash = hash;
        this.shortHash = shortHash;
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.timestamp = timestamp;
        this.parentHashes = parentHashes != null ? parentHashes : new ArrayList<>();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getShortHash() {
        return shortHash;
    }

    public void setShortHash(String shortHash) {
        this.shortHash = shortHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getParentHashes() {
        return parentHashes;
    }

    public void setParentHashes(List<String> parentHashes) {
        this.parentHashes = parentHashes;
    }
}
