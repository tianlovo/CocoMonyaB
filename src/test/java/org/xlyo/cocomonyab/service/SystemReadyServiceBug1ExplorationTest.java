package org.xlyo.cocomonyab.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.xlyo.cocomonyab.domain.vo.SystemStatusVO;
import org.xlyo.cocomonyab.event.startup.StartupStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

/**
 * Bug Condition Exploration Test for Bug 1: System Startup Status Display Error
 * <p>
 * This test is designed to FAIL on unfixed code to confirm the bug exists.
 * When the system successfully starts up, the ready field is true, but the status field
 * shows "NOT_STARTED" and currentPhase shows "未启动", creating inconsistent state information.
 * <p>
 * Expected behavior (after fix):
 * - When systemReady is true, status should be READY
 * - When systemReady is true, currentPhase should be "应用就绪"
 * - When systemReady is true, progress should be 100
 * <p>
 * This test will PASS after the fix is implemented.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SystemReadyServiceBug1ExplorationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private SystemReadyService systemReadyService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Test
    void systemStatusFieldsShouldBeConsistentWhenReady() {
        // Wait for system to become ready (checkSystemReadiness completes)
        // The bug is in checkSystemReadiness() method which runs asynchronously
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(() -> systemReadyService.getSystemReady().get());
        
        // Query the system status endpoint
        String url = "http://localhost:" + port + "/api/system/status";
        ResponseEntity<SystemStatusVO> response = restTemplate.getForEntity(url, SystemStatusVO.class);
        
        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SystemStatusVO status = response.getBody();
        assertThat(status).isNotNull();
        
        // The bug: when ready is true, status and currentPhase are NOT updated
        // Expected behavior (will fail on unfixed code):
        assertThat(status.getReady())
            .as("System should be ready")
            .isTrue();
        
        assertThat(status.getStatus())
            .as("Status should be READY when system is ready, but got: %s", status.getStatus())
            .isEqualTo(StartupStatus.READY);
        
        assertThat(status.getCurrentPhase())
            .as("Current phase should be '应用就绪' when system is ready, but got: %s", status.getCurrentPhase())
            .isEqualTo("应用就绪");
        
        assertThat(status.getProgress())
            .as("Progress should be 100 when system is ready, but got: %d", status.getProgress())
            .isEqualTo(100);
        
        assertThat(status.getReason())
            .as("Reason should be null when system is ready")
            .isNull();
    }
}
