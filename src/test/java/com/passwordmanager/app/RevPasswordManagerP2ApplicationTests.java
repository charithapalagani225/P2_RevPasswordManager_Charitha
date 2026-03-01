package com.passwordmanager.app;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration test that loads the full Spring context.
 * This test requires a running database connection.
 * Run manually with: mvn test -Dtest=RevPasswordManagerP2ApplicationTests
 * Skipped during regular unit test runs.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RevPasswordManagerP2ApplicationTests {

    @Ignore("Requires database connection - run manually for integration testing")
    @Test
    public void contextLoads() {
        // Full application context integration test
    }

}
