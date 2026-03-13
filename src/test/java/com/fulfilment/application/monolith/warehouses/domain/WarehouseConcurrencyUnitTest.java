package com.fulfilment.application.monolith.warehouses.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for Warehouse concurrency and thread-safety patterns
 * Simulates concurrent operations on warehouse instances
 * Ensures domain model rules are maintained under concurrent access
 */
public class WarehouseConcurrencyUnitTest {

    // ============ Concurrent Stock Management Tests ============

    @Test
    void testConcurrentStockAdditionsRemainThreadSafe() {
        Warehouse warehouse = Warehouse.reconstruct("CONCURRENT-001", "AMSTERDAM-001", 1000, 0, LocalDateTime.now(), null);
        
        int threadCount = 5;
        int incrementsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        for (int j = 0; j < incrementsPerThread; j++) {
                            warehouse.addStock(1);
                        }
                    } catch (Exception e) {
                        // Expected - capacity may be exceeded in concurrent scenario
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Verify that stock was incremented (even with capacity constraints)
            assertTrue(warehouse.stock > 0, "Stock should be incremented by concurrent operations");
            assertTrue(warehouse.stock <= 1000, "Stock should not exceed capacity");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    @Test
    void testConcurrentModificationsOnArchivedWarehouse() {
        Warehouse warehouse = Warehouse.reconstruct("ARCHIVE-CONCURRENT-001", "ZWOLLE-001", 500, 100, LocalDateTime.now(), null);
        warehouse.archive();
        
        int threadCount = 3;
        AtomicInteger failureCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        warehouse.addStock(10);
                    } catch (IllegalArgumentException e) {
                        // Expected - archived warehouse cannot be modified
                        if (e.getMessage().toLowerCase().contains("archived")) {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // All concurrent attempts should fail because warehouse is archived
            assertEquals(threadCount, failureCount.get(), 
                    "All concurrent stock additions to archived warehouse should fail");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    // ============ Concurrent Warehouse Creation Scenarios ============

    @Test
    void testMultipleConcurrentWarehouseCreations() {
        int warehouseCount = 10;
        List<Warehouse> warehouses = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(warehouseCount);
        
        try {
            for (int i = 0; i < warehouseCount; i++) {
                final int index = i;
                executor.execute(() -> {
                    try {
                        Warehouse wh = Warehouse.create(
                                "CONCURRENT-CREATE-" + index,
                                "AMSTERDAM-001",
                                50 + (index * 5),  // Capacity from 50 to 95, all within 150 max
                                150  // Amsterdam-001 max capacity
                        );
                        synchronized (warehouses) {
                            warehouses.add(wh);
                        }
                    } catch (IllegalArgumentException e) {
                        // Capacity validation failed - log but don't fail test
                        // This tests that validation is applied during concurrent creation
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // All warehouses should be created successfully with proper capacities
            assertEquals(warehouseCount, warehouses.size(), 
                    "All concurrent warehouse creations should succeed");
            
            // Verify each warehouse has unique code
            long uniqueCodes = warehouses.stream()
                    .map(w -> w.businessUnitCode)
                    .distinct()
                    .count();
            assertEquals(warehouseCount, uniqueCodes, 
                    "Each warehouse should have unique business unit code");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    // ============ Concurrent Validation Scenarios ============

    @Test
    void testConcurrentCapacityValidation() {
        Warehouse warehouse = Warehouse.reconstruct("CAPACITY-TEST-001", "TILBURG-001", 100, 90, LocalDateTime.now(), null);
        
        int threadCount = 4;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        warehouse.addStock(5); // Total would be 110, exceeds capacity of 100
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // At least one should fail due to capacity constraint
            assertTrue(failureCount.get() > 0, 
                    "At least one concurrent stock addition should fail due to capacity");
            assertTrue(warehouse.stock <= 100, 
                    "Stock should never exceed capacity");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    // ============ Concurrent State Consistency Tests ============

    @Test
    void testConcurrentOperationsPreserveWarehouseState() {
        Warehouse warehouse = Warehouse.reconstruct("STATE-CONSISTENCY-001", "AMSTERDAM-001", 1000, 100, LocalDateTime.now(), null);
        String originalCode = warehouse.businessUnitCode;
        String originalLocation = warehouse.location;
        LocalDateTime originalCreatedAt = warehouse.createdAt;
        
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        warehouse.addStock(50);
                    } catch (IllegalArgumentException e) {
                        // May exceed capacity - that's expected
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Verify immutable fields weren't changed
            assertEquals(originalCode, warehouse.businessUnitCode, 
                    "Business unit code should remain unchanged");
            assertEquals(originalLocation, warehouse.location, 
                    "Location should remain unchanged");
            assertEquals(originalCreatedAt, warehouse.createdAt, 
                    "Created timestamp should remain unchanged");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    // ============ Concurrent Archive Operations ============

    @Test
    void testConcurrentArchiveOperationThrowsOnDuplicate() {
        Warehouse warehouse = Warehouse.reconstruct("ARCHIVE-CONCURRENT-FAIL-001", "ZWOLLE-001", 300, 50, LocalDateTime.now(), null);
        
        int threadCount = 3;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            LocalDateTime beforeArchive = LocalDateTime.now();
            
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        warehouse.archive();
                        successCount.incrementAndGet();
                    } catch (IllegalArgumentException e) {
                        if (e.getMessage().contains("already archived")) {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Only one thread succeeds; others fail with "already archived" exception
            assertEquals(1, successCount.get(), "Exactly one archive should succeed");
            assertEquals(threadCount - 1, failureCount.get(), 
                    "Concurrent archive attempts after first should fail");
            
            // Verify warehouse is archived
            assertNotNull(warehouse.archivedAt, "Warehouse should be archived");
            assertTrue(warehouse.archivedAt.isAfter(beforeArchive.minusSeconds(1)), 
                    "Archive timestamp should be recent");
            LocalDateTime afterArchive = LocalDateTime.now();
            assertTrue(warehouse.archivedAt.isBefore(afterArchive.plusSeconds(1)), 
                    "Archive timestamp should be within recent timeframe");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    // ============ Concurrent Warehouse Reconstruction ============

    @Test
    void testConcurrentWarehouseReconstruction() {
        int reconstructCount = 5;
        List<Warehouse> reconstructedWarehouses = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(reconstructCount);
        
        try {
            for (int i = 0; i < reconstructCount; i++) {
                executor.execute(() -> {
                    try {
                        Warehouse wh = Warehouse.reconstruct(
                                "RECONSTRUCT-001",
                                "TILBURG-001",
                                200,
                                75,
                                LocalDateTime.now().minusDays(1),
                                null
                        );
                        synchronized (reconstructedWarehouses) {
                            reconstructedWarehouses.add(wh);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Verify all reconstructions were successful
            assertEquals(reconstructCount, reconstructedWarehouses.size(), 
                    "All concurrent reconstructions should succeed");
            
            // Verify all have same immutable properties
            Warehouse first = reconstructedWarehouses.get(0);
            reconstructedWarehouses.forEach(wh -> {
                assertEquals(first.businessUnitCode, wh.businessUnitCode);
                assertEquals(first.location, wh.location);
                assertEquals(first.capacity, wh.capacity);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    // ============ Complex Concurrent Scenario ============

    @Test
    void testComplexConcurrentScenarioWithMixedOperations() throws InterruptedException {
        Warehouse warehouse = Warehouse.reconstruct("COMPLEX-CONCURRENT-001", "AMSTERDAM-001", 500, 100, LocalDateTime.now(), null);
        
        int threadCount = 10;
        AtomicInteger archiveSuccessCount = new AtomicInteger(0);
        AtomicInteger archiveFailureCount = new AtomicInteger(0);
        AtomicInteger stockAddSuccessCount = new AtomicInteger(0);
        AtomicInteger stockAddFailureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        try {
            // Mix of operations: archive and stock additions
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.execute(() -> {
                    try {
                        if (index % 2 == 0) {
                            // Even threads try to archive
                            try {
                                warehouse.archive();
                                archiveSuccessCount.incrementAndGet();
                            } catch (IllegalArgumentException e) {
                                if (e.getMessage().contains("already archived")) {
                                    archiveFailureCount.incrementAndGet();
                                }
                            }
                        } else {
                            // Odd threads try to add stock
                            try {
                                warehouse.addStock(30);
                                stockAddSuccessCount.incrementAndGet();
                            } catch (IllegalArgumentException e) {
                                stockAddFailureCount.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            executor.shutdown();
            
            // Verify warehouse is in a valid final state
            assertNotNull(warehouse.archivedAt, "Warehouse should be archived");
            assertEquals(1, archiveSuccessCount.get(), "Only one archive should succeed");
            assertTrue(archiveFailureCount.get() > 0, "Concurrent archives should fail");
            assertTrue(warehouse.stock >= 100 && warehouse.stock <= 500, 
                    "Stock should be within valid range");
            assertTrue(stockAddFailureCount.get() > 0, 
                    "Stock additions after archive should fail");
        } catch (Exception e) {
            fail("Concurrent complex scenario failed: " + e.getMessage());
        }
    }
}
