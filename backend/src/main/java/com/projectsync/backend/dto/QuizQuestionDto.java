package com.projectsync.backend.dto;

import java.util.List;

public record QuizQuestionDto(
    String id,
    String category,
    String question,
    List<String> options
) {}
