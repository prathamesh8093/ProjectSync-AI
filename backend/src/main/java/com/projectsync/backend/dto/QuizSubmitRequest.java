package com.projectsync.backend.dto;

import java.util.Map;

public record QuizSubmitRequest(
    String quizId,
    Map<String, Integer> answers,
    String primaryInterest,
    String pastProjects
) {}
