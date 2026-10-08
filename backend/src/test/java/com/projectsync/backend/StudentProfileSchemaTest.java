package com.projectsync.backend;

import com.projectsync.backend.entity.StudentProfile;
import com.projectsync.backend.repository.StudentProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schematestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;INIT=CREATE DOMAIN IF NOT EXISTS \"TEXT\" AS CHARACTER LARGE OBJECT",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true"
})
class StudentProfileSchemaTest {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Test
    void testStudentProfileCreationAndLongBioStorage() {
        UUID userId = UUID.randomUUID();
        String longBio = "A".repeat(5000); // 5000 character bio to verify long text support

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setStudentId("STU-" + System.currentTimeMillis());
        profile.setDepartment("Computer Science & AI");
        profile.setYear(3);
        profile.setBio(longBio);
        profile.setCreatedAt(OffsetDateTime.now());
        profile.setUpdatedAt(OffsetDateTime.now());

        StudentProfile saved = studentProfileRepository.save(profile);
        assertNotNull(saved);

        StudentProfile retrieved = studentProfileRepository.findById(userId).orElse(null);
        assertNotNull(retrieved);
        assertEquals(5000, retrieved.getBio().length());
        assertEquals(longBio, retrieved.getBio());
        assertEquals(3, retrieved.getYear());
    }
}
