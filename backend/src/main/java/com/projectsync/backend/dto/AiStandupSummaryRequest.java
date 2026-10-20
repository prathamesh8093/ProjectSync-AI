package com.projectsync.backend.dto;

public record AiStandupSummaryRequest(
        String meetingNotes,
        String date
) {}
