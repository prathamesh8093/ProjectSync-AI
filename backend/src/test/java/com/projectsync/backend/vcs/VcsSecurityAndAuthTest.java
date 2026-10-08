package com.projectsync.backend.vcs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:h2:mem:vcsauthtest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;INIT=CREATE DOMAIN IF NOT EXISTS \"TEXT\" AS CHARACTER LARGE OBJECT",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "projectsync.vcs.dev-mode=false"
})
public class VcsSecurityAndAuthTest {

        @Autowired
        private WebApplicationContext context;

        @Autowired
        private com.projectsync.backend.repository.UserRepository userRepository;

        @Autowired
        private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private MockMvc mockMvc;

        @BeforeEach
        void setup() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();

                if (!userRepository.existsByEmail("student@projectsync.local")) {
                        com.projectsync.backend.entity.User user = new com.projectsync.backend.entity.User();
                        user.setEmail("student@projectsync.local");
                        user.setPasswordHash(passwordEncoder.encode("Password123!"));
                        user.setFullName("Local Student");
                        user.setRole("STUDENT");
                        user.setIsActive(true);
                        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
                        user.setCreatedAt(now);
                        user.setUpdatedAt(now);
                        userRepository.save(user);
                }
        }

        @Test
        void healthEndpoint_shouldBePublic() throws Exception {
                mockMvc.perform(get("/api/vcs/health"))
                                .andExpect(status().isOk());
        }

        @Test
        void createRepository_withoutAuth_shouldReturn401() throws Exception {
                String repoPayload = "{\"name\":\"unauth-repo\",\"description\":\"desc\",\"defaultBranch\":\"main\",\"initReadme\":true}";

                mockMvc.perform(post("/api/vcs/repositories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(repoPayload))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void loginAndCreateRepository_withAuth_shouldSucceed() throws Exception {
                // 1. Authenticate with seeded student account
                String loginPayload = "{\"email\":\"student@projectsync.local\",\"password\":\"Password123!\"}";
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn();

                JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
                String token = loginJson.get("token").asText();
                assertNotNull(token);
                assertFalse(token.isBlank());

                // 2. Create repository with Bearer token
                String repoName = "auth-student-repo-" + System.currentTimeMillis();
                String repoPayload = "{\"name\":\"" + repoName
                                + "\",\"description\":\"Authenticated repo\",\"defaultBranch\":\"main\",\"initReadme\":true}";

                MvcResult createResult = mockMvc.perform(post("/api/vcs/repositories")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(repoPayload))
                                .andExpect(status().isCreated())
                                .andReturn();

                JsonNode repoJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
                assertEquals(repoName, repoJson.get("name").asText());
        }

        @Test
        void cloneRepository_withoutAuth_shouldReturn401() throws Exception {
                String clonePayload = "{\"remoteUrl\":\"https://github.com/octocat/Hello-World.git\",\"name\":\"hello-clone\"}";

                mockMvc.perform(post("/api/vcs/repositories/clone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(clonePayload))
                                .andExpect(status().isUnauthorized());
        }
}
