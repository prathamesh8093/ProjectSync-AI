package com.projectsync.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR;INIT=CREATE DOMAIN IF NOT EXISTS \"TEXT\" AS CHARACTER LARGE OBJECT")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
