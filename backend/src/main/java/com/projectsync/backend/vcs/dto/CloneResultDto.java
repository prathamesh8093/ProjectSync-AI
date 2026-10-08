package com.projectsync.backend.vcs.dto;

public class CloneResultDto {

    private RepositoryDetailsDto repository;
    private String message;
    private String cloneOutput;

    public CloneResultDto() {}

    public CloneResultDto(RepositoryDetailsDto repository, String message, String cloneOutput) {
        this.repository = repository;
        this.message = message;
        this.cloneOutput = cloneOutput;
    }

    public RepositoryDetailsDto getRepository() {
        return repository;
    }

    public void setRepository(RepositoryDetailsDto repository) {
        this.repository = repository;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCloneOutput() {
        return cloneOutput;
    }

    public void setCloneOutput(String cloneOutput) {
        this.cloneOutput = cloneOutput;
    }
}

