package com.fulfilment.application.monolith.warehouses.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.junit.jupiter.api.Test;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Sophisticated Test: Concurrency Integration Test
 * 
 * Tests race conditions and thread safety by simulating concurrent requests.
 * This test is NOT explicitly mentioned in documentation - candidates discover it!
 * 
 * Key Concepts:
 * - ExecutorService for concurrent execution
 * - CountDownLatch for synchronization
 * - Database constraints under load
 * - Handling concurrent duplicates
 */
@QuarkusTest
public class WarehouseConcurrencyIT {

  @Inject
  WarehouseRepository warehouseRepository;

  @Inject
  LocationResolver locationResolver;

  @Inject
  private CreateWarehouseUseCase createWarehouseUseCase;
  
  @Inject
  ReplaceWarehouseOperation replaceWarehouseOperation;
  
  @Inject
  ManagedExecutor executor;
  
  @Inject
  EntityManager em;


  /**
   * Test concurrent creation of warehouses with unique codes.
   * All should succeed.
   */
  @Test
  @ActivateRequestContext
  public void testConcurrentWarehouseCreationWithUniqueCodesSucceeds() throws InterruptedException {
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    List<Future<Boolean>> futures = new ArrayList<>();
    
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      Future<Boolean> future = executor.submit(() -> {
        try {
          Warehouse warehouse = new Warehouse();
          warehouse.businessUnitCode = "CONCURRENT-" + index;
          warehouse.location = "AMSTERDAM-001";
          warehouse.capacity = 50;
          warehouse.stock = 10;
          
          createWarehouseUseCase.create(warehouse);
          return true;
        } catch (Exception e) {
          return false;
        } finally {
          latch.countDown();
        }
      });
      futures.add(future);
    }
    
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    
    // All should succeed since codes are unique
    long successCount = futures.stream().filter(f -> {
      try {
        return f.get();
      } catch (Exception e) {
        return false;
      }
    }).count();
    
    assertEquals(threadCount, successCount, "All concurrent creations with unique codes should succeed");
  }

  /**
   * Test concurrent creation of warehouses with SAME code.
   * Only one should succeed, others should fail with duplicate error.
   */
  @Test
  @ActivateRequestContext
  public void testConcurrentWarehouseCreationWithDuplicateCodeFails() throws InterruptedException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    
    String duplicateCode = "DUPLICATE-CODE-" + System.currentTimeMillis();
    
    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          Warehouse warehouse = new Warehouse();
          warehouse.businessUnitCode = duplicateCode;  // Same code for all!
          warehouse.location = "ZWOLLE-001";
          warehouse.capacity = 30;
          warehouse.stock = 5;
          
          createWarehouseUseCase.create(warehouse);
          successCount.incrementAndGet();
        } catch (Exception e) {
          // Expected: duplicate key or already exists error
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    
    // Only one should succeed
    assertEquals(1, successCount.get(), "Only one warehouse with duplicate code should be created");
    assertEquals(threadCount - 1, failureCount.get(), "Other attempts should fail");
  }

  /**
   * Test concurrent reads don't block each other (read scalability).
   */
  @Test
  public void testConcurrentReadsAreNonBlocking() throws InterruptedException {

      // Create warehouse first
      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "READ-TEST-001";
      warehouse.location = "AMSTERDAM-001";
      warehouse.capacity = 100;
      warehouse.stock = 50;

      createWarehouseUseCase.create(warehouse);

      int readThreadCount = 20;

      ExecutorService executor = Executors.newFixedThreadPool(readThreadCount);
      CountDownLatch latch = new CountDownLatch(readThreadCount);
      AtomicInteger successfulReads = new AtomicInteger(0);

      for (int i = 0; i < readThreadCount; i++) {

          executor.submit(() -> {
              try {

                  // Each thread runs inside its own transaction
                  QuarkusTransaction.requiringNew().run(() -> {

                      Warehouse found =
                          warehouseRepository.findByBusinessUnitCode("READ-TEST-001");

                      if (found != null) {
                          successfulReads.incrementAndGet();
                      }

                  });

              } finally {
                  latch.countDown();
              }
          });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      assertEquals(readThreadCount, successfulReads.get(),
              "All concurrent reads should succeed");
  }
  
  @Test
  @ActivateRequestContext
  public void testConcurrentReplaceFailsForSecondRequest() throws InterruptedException {

      Warehouse warehouse = new Warehouse();
      warehouse.businessUnitCode = "REPLACE-CONCURRENT";
      warehouse.location = "AMSTERDAM-001";
      warehouse.capacity = 100;
      warehouse.stock = 10;

      createWarehouseUseCase.create(warehouse);

      int threadCount = 2;

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      CountDownLatch latch = new CountDownLatch(threadCount);

      AtomicInteger success = new AtomicInteger(0);
      AtomicInteger failure = new AtomicInteger(0);

      for (int i = 0; i < threadCount; i++) {

          executor.submit(() -> {
              try {

                  QuarkusTransaction.requiringNew().run(() -> {

                      replaceWarehouseOperation.replace(
                              "REPLACE-CONCURRENT",
                              "AMSTERDAM-001",
                              120,
                              20
                      );

                  });

                  success.incrementAndGet();

              } catch (Exception e) {

                  // Expected for one of the threads
                  failure.incrementAndGet();

              } finally {
                  latch.countDown();
              }
          });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      assertEquals(1, success.get(), "Only one replace should succeed");
      assertEquals(1, failure.get(), "Second replace should fail due to concurrency");
  }
}
