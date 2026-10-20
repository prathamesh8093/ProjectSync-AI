package com.projectsync.backend.dto;

import com.projectsync.backend.entity.StudentAssessment;
import java.util.List;
import java.util.Map;

public record QuizResultResponse(
    StudentAssessment assessment,
    int totalQuestions,
    int correctCount,
    Map<String, Integer> categoryScores,
    List<QuizQuestionFeedback> review
) {
    public record QuizQuestionFeedback(
        String id,
        String category,
        String question,
        List<String> options,
        int selectedIndex,
        int correctIndex,
        boolean isCorrect,
        String explanation
    ) {}
}
