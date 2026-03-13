package com.fulfilment.application.monolith.stores;

import com.fulfilment.application.monolith.stores.domain.model.Store;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class LegacyStoreManagerGateway {

    private static final Logger LOGGER =
            Logger.getLogger(LegacyStoreManagerGateway.class);

    public void createStoreOnLegacySystem(Store store) {
        LOGGER.infof("Sending CREATE event to legacy system for store: %s",
                store.getName());
        write(store);
    }

    public void updateStoreOnLegacySystem(Store store) {
        LOGGER.infof("Sending UPDATE event to legacy system for store: %s",
                store.getName());
        write(store);
    }

    private void write(Store store) {
        try {
            Path file = Files.createTempFile(store.getName(), ".txt");

            String content = "Store synced [name=" + store.getName()
                    + ", stock=" + store.getQuantityProductsInStock() + "]";

            Files.write(file, content.getBytes());

            LOGGER.debugf("Temporary file created for legacy sync: %s",
                    file.toAbsolutePath());

            Files.delete(file);

            LOGGER.infof("Legacy sync completed successfully for store: %s",
                    store.getName());

        } catch (Exception e) {
            LOGGER.errorf(e,
                    "Legacy sync FAILED for store: %s",
                    store.getName());

            // Optional: rethrow if you want transaction to fail
            // throw new RuntimeException("Legacy sync failed", e);
        }
    }
}