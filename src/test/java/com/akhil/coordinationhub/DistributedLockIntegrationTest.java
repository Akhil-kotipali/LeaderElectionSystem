package com.akhil.coordinationhub;

import com.akhil.coordinationhub.dto.LockResponse;
import com.akhil.coordinationhub.service.ClusterNodeService;
import com.akhil.coordinationhub.service.DistributedLockService;
import com.akhil.coordinationhub.service.ProtectedResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DistributedLockIntegrationTest {

    @Autowired
    private DistributedLockService lockService;

    @Autowired
    private ClusterNodeService nodeService;

    @Autowired
    private ProtectedResourceService resourceService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM protected_resource");
        jdbc.update("DELETE FROM distributed_lock");

        nodeService.register("test-node-1");
        nodeService.register("test-node-2");
    }

    @Test
    void onlyOneNodeAcquiresTheSameLockConcurrently() throws Exception {
        int contenders = 2;
        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<LockResponse>> futures = IntStream.rangeClosed(1, contenders)
                    .mapToObj(i -> pool.submit(() -> {
                        String node = "test-node-" + i;
                        ready.countDown();

                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start signal timed out");
                        }

                        return lockService.acquire("shared-job", node);
                    }))
                    .toList();

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<LockResponse> results = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            long successes = results.stream()
                    .filter(LockResponse::success)
                    .count();

            assertEquals(1, successes,
                    "Exactly one contender should acquire the lock");
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void expiredLockCanBeAcquiredByAnotherNode() {
        LockResponse first = lockService.acquire("expiring-job", "test-node-1");
        assertTrue(first.success());

        jdbc.update("""
            UPDATE distributed_lock
            SET lease_until = TIMESTAMPADD(SECOND, -1, CURRENT_TIMESTAMP(6))
            WHERE lock_name = ?
            """, "expiring-job");

        LockResponse second = lockService.acquire("expiring-job", "test-node-2");

        assertTrue(second.success());
        assertEquals("test-node-2", second.ownerNodeId());
        assertTrue(second.fencingToken() > first.fencingToken());
    }

    @Test
    void oldOwnerCannotWriteAfterTakeover() {
        LockResponse first = lockService.acquire("protected-job", "test-node-1");
        assertTrue(first.success());

        jdbc.update("""
            UPDATE distributed_lock
            SET lease_until = TIMESTAMPADD(SECOND, -1, CURRENT_TIMESTAMP(6))
            WHERE lock_name = ?
            """, "protected-job");

        LockResponse second = lockService.acquire("protected-job", "test-node-2");
        assertTrue(second.success());

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> resourceService.write(
                        "protected-job",
                        "test-node-1",
                        first.fencingToken(),
                        "important-record",
                        "stale write"
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void nonOwnerCannotRenewLock() {
        LockResponse first = lockService.acquire("owner-check", "test-node-1");
        assertTrue(first.success());

        LockResponse attempt = lockService.renew("owner-check", "test-node-2");

        assertFalse(attempt.success());
        assertEquals("test-node-1", attempt.ownerNodeId());
    }
}
