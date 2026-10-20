package com.projectsync.backend.service;

import com.projectsync.backend.dto.QuizQuestionDto;
import com.projectsync.backend.dto.QuizResultResponse;
import com.projectsync.backend.dto.QuizSubmitRequest;
import com.projectsync.backend.entity.StudentAssessment;
import com.projectsync.backend.entity.User;
import com.projectsync.backend.repository.StudentAssessmentRepository;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAssessmentService {

    public record InternalQuestion(
            String id,
            String category,
            String question,
            List<String> options,
            int correctIndex,
            String explanation
    ) {}

    private final StudentAssessmentRepository assessmentRepository;
    private final GeminiApiClient gemini;
    private final ObjectMapper objectMapper;

    // Cache active quizzes in memory: quizId -> questions
    private final Map<String, List<InternalQuestion>> quizCache = new ConcurrentHashMap<>();

    private static final List<InternalQuestion> QUESTION_BANK = List.of(
            // Frontend
            new InternalQuestion("fe-1", "FRONTEND",
                    "Which hook should be used in React to perform side effects like fetching data?",
                    List.of("useState", "useEffect", "useMemo", "useCallback"),
                    1,
                    "useEffect is designed for handling side effects like data subscriptions, timers, and HTTP calls."),
            new InternalQuestion("fe-2", "FRONTEND",
                    "What is the primary advantage of the Virtual DOM in modern UI libraries?",
                    List.of("It completely replaces browser rendering", "It minimizes expensive real-DOM manipulations through batch diffing", "It bypasses JavaScript runtime", "It directly compiles CSS to binary"),
                    1,
                    "The Virtual DOM maintains an in-memory representation and calculates optimal diffs to update real DOM nodes efficiently."),

            // Backend
            new InternalQuestion("be-1", "BACKEND",
                    "In RESTful architecture, which HTTP method is considered idempotent and replaces an entire resource?",
                    List.of("POST", "PATCH", "PUT", "CONNECT"),
                    2,
                    "PUT is idempotent and replaces the target resource with the uploaded payload."),
            new InternalQuestion("be-2", "BACKEND",
                    "What core design pattern does Spring Framework use for loose coupling between components?",
                    List.of("Singleton Factory", "Dependency Injection / Inversion of Control", "Observer Pattern", "Active Record"),
                    1,
                    "Dependency Injection (IoC) allows Spring container to inject dependencies at runtime rather than hardcoding them."),

            // Database
            new InternalQuestion("db-1", "DATABASE",
                    "What does the 'A' in ACID transactions stand for, and what does it guarantee?",
                    List.of("Auditability - logs every query", "Atomicity - all operations succeed or all are rolled back", "Availability - database never rejects requests", "Asynchrony - writes happen in background"),
                    1,
                    "Atomicity guarantees that a transaction is all-or-nothing."),
            new InternalQuestion("db-2", "DATABASE",
                    "Which data structure is most commonly used by relational database engines (like PostgreSQL) for primary key indexes?",
                    List.of("Linked List", "Hash Table only", "B-Tree / B+Tree", "Red-Black Tree"),
                    2,
                    "B-Trees provide O(log n) time complexity for range queries, sorting, and equality lookups."),

            // DevOps
            new InternalQuestion("do-1", "DEVOPS",
                    "What is the primary benefit of multi-stage Docker builds?",
                    List.of("Speeds up network downloads only", "Separates build-time dependencies from the runtime image to reduce image size and attack surface", "Allows running multiple operating systems simultaneously", "Replaces the need for a Dockerfile"),
                    1,
                    "Multi-stage builds allow compiling in one stage and copying only the final binary into a lightweight production runtime."),
            new InternalQuestion("do-2", "DEVOPS",
                    "In a CI/CD pipeline, what does Continuous Delivery guarantee over Continuous Deployment?",
                    List.of("Code is tested and automatically deployed to production without human approval", "Code is continuously kept in a deployable state, but production release can require manual trigger", "Code is never merged to main branch", "Developers deploy straight from local machines"),
                    1,
                    "Continuous Delivery ensures artifacts are production-ready at all times with optional manual approval for prod."),

            // AI/ML
            new InternalQuestion("ai-1", "ML_AI",
                    "In Machine Learning, what technique is specifically used to prevent a model from overfitting to training data?",
                    List.of("Removing validation split", "Regularization (e.g., L1/L2, Dropout)", "Increasing training epochs infinitely", "Decreasing learning rate to zero"),
                    1,
                    "Regularization penalizes overly complex weights to improve generalization on unseen test data."),
            new InternalQuestion("ai-2", "ML_AI",
                    "In modern Generative AI, what is 'Retrieval-Augmented Generation' (RAG) primarily used for?",
                    List.of("Retraining the entire base LLM from scratch", "Grounding LLM responses with relevant facts retrieved from an external knowledge base", "Compressing model weights onto mobile devices", "Translating code into machine language"),
                    1,
                    "RAG fetches relevant document context from a vector database and includes it in the prompt to reduce hallucinations.")
    );

    public AiAssessmentService(StudentAssessmentRepository assessmentRepository,
                               GeminiApiClient gemini,
                               ObjectMapper objectMapper) {
        this.assessmentRepository = assessmentRepository;
        this.gemini = gemini;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a 5-question real-time quiz covering Frontend, Backend, Database, DevOps, and ML/AI.
     */
    public Map<String, Object> generateQuiz(User student) {
        String quizId = UUID.randomUUID().toString();
        List<InternalQuestion> questions = null;

        // Try AI generation via Gemini
        try {
            questions = generateWithGemini(quizId);
        } catch (Exception e) {
            // Fall back to curated randomized bank
            questions = generateFromBank(quizId);
        }

        quizCache.put(quizId, questions);

        // Sanitize for frontend (hide correctIndex and explanation)
        List<QuizQuestionDto> clientQuestions = questions.stream()
                .map(q -> new QuizQuestionDto(q.id(), q.category(), q.question(), q.options()))
                .toList();

        return Map.of(
                "quizId", quizId,
                "questions", clientQuestions
        );
    }

    private List<InternalQuestion> generateWithGemini(String quizId) throws Exception {
        String prompt = """
                Generate 5 distinct technical multiple-choice questions for software engineering students.
                Exactly one question per category: FRONTEND, BACKEND, DATABASE, DEVOPS, ML_AI.
                Ensure the questions are highly varied and different from typical examples.
                Randomness seed to ensure uniqueness: %s
                Each question must have 4 options, a 0-based correctIndex, and a 1-sentence explanation.
                Return ONLY a JSON array with schema:
                [
                  {
                    "id": "q1",
                    "category": "FRONTEND",
                    "question": "question text",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctIndex": 1,
                    "explanation": "why option B is correct"
                  }
                ]
                """.formatted(UUID.randomUUID().toString());

        String json = gemini.generateJson(prompt);
        List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});

        List<InternalQuestion> questions = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> item : list) {
            String id = "ai-" + index++;
            String cat = String.valueOf(item.get("category")).toUpperCase();
            String q = String.valueOf(item.get("question"));
            @SuppressWarnings("unchecked")
            List<String> opts = (List<String>) item.get("options");
            int correct = ((Number) item.get("correctIndex")).intValue();
            String exp = String.valueOf(item.get("explanation"));
            questions.add(new InternalQuestion(id, cat, q, opts, correct, exp));
        }

        if (questions.size() < 5) {
            throw new IllegalStateException("Gemini returned fewer than 5 questions.");
        }
        return questions;
    }

    private List<InternalQuestion> generateFromBank(String quizId) {
        Map<String, List<InternalQuestion>> byCat = new HashMap<>();
        for (InternalQuestion q : QUESTION_BANK) {
            byCat.computeIfAbsent(q.category(), k -> new ArrayList<>()).add(q);
        }

        List<InternalQuestion> selected = new ArrayList<>();
        Random rand = new Random();
        for (String cat : List.of("FRONTEND", "BACKEND", "DATABASE", "DEVOPS", "ML_AI")) {
            List<InternalQuestion> inCat = byCat.getOrDefault(cat, List.of());
            if (!inCat.isEmpty()) {
                selected.add(inCat.get(rand.nextInt(inCat.size())));
            }
        }
        return selected;
    }

    /**
     * Grades the submitted quiz, calculates objective 1-10 skill scores, and saves to student_assessments.
     */
    public QuizResultResponse submitQuiz(User student, QuizSubmitRequest request) {
        List<InternalQuestion> questions = quizCache.get(request.quizId());
        if (questions == null || questions.isEmpty()) {
            // If quiz expired or not found, use default bank for grading
            questions = QUESTION_BANK.subList(0, 5);
        }

        Map<String, Integer> answers = request.answers() != null ? request.answers() : Collections.emptyMap();
        Map<String, Integer> categoryScores = new HashMap<>();
        List<QuizResultResponse.QuizQuestionFeedback> reviews = new ArrayList<>();

        int correctCount = 0;

        for (InternalQuestion q : questions) {
            Integer chosenIndex = answers.get(q.id());
            boolean isCorrect = chosenIndex != null && chosenIndex == q.correctIndex();
            if (isCorrect) correctCount++;

            // Base score: 9-10 if correct, 4-5 if incorrect (scale 1-10)
            int score = isCorrect ? 9 : 4;
            categoryScores.put(q.category(), score);

            reviews.add(new QuizResultResponse.QuizQuestionFeedback(
                    q.id(),
                    q.category(),
                    q.question(),
                    q.options(),
                    chosenIndex != null ? chosenIndex : -1,
                    q.correctIndex(),
                    isCorrect,
                    q.explanation()
            ));
        }

        // Save to Database
        StudentAssessment assessment = assessmentRepository.findById(student.getId()).orElseGet(StudentAssessment::new);
        assessment.setUserId(student.getId());
        assessment.setFrontendScore(categoryScores.getOrDefault("FRONTEND", 5));
        assessment.setBackendScore(categoryScores.getOrDefault("BACKEND", 5));
        assessment.setDatabaseScore(categoryScores.getOrDefault("DATABASE", 5));
        assessment.setDevopsScore(categoryScores.getOrDefault("DEVOPS", 5));
        assessment.setMlAiScore(categoryScores.getOrDefault("ML_AI", 5));
        assessment.setPrimaryInterest(request.primaryInterest() != null && !request.primaryInterest().isBlank()
                ? request.primaryInterest() : "Fullstack");
        assessment.setPastProjects(request.pastProjects());
        assessment.setUpdatedAt(OffsetDateTime.now());

        assessment = assessmentRepository.save(assessment);

        // Remove from cache after completion
        if (request.quizId() != null) {
            quizCache.remove(request.quizId());
        }

        return new QuizResultResponse(
                assessment,
                questions.size(),
                correctCount,
                categoryScores,
                reviews
        );
    }
}
