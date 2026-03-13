package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocationGateway.
 *
 * Tests cover:
 * 1. Positive scenarios (valid identifiers)
 * 2. Negative scenarios (unknown identifiers)
 * 3. Edge/error conditions (null, empty, case sensitivity)
 */
public class LocationGatewayTest {

    private static final Logger LOGGER =
            Logger.getLogger(LocationGatewayTest.class);

    private LocationGateway locationGateway;

    @BeforeEach
    void setUp() {

        LOGGER.info("Initializing LocationGateway for test");

        locationGateway = new LocationGateway();
    }

    // -------------------------
    // Positive Test Cases
    // -------------------------

    @Test
    void shouldResolveZwolleLocation() {

        LOGGER.info("Testing Zwolle location resolution");

        Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

        assertNotNull(location);
        assertEquals("ZWOLLE-001", location.identifier());
        assertEquals(1, location.maxNumberOfWarehouses());
        assertEquals(40, location.maxCapacity());

        LOGGER.info("Zwolle location resolved successfully");
    }

    @Test
    void shouldResolveAmsterdamLocation() {

        LOGGER.info("Testing Amsterdam location resolution");

        Location location = locationGateway.resolveByIdentifier("AMSTERDAM-001");

        assertNotNull(location);
        assertEquals("AMSTERDAM-001", location.identifier());
        assertEquals(5, location.maxNumberOfWarehouses());
        assertEquals(100, location.maxCapacity());

        LOGGER.info("Amsterdam location resolved successfully");
    }

    @Test
    void shouldResolveMultipleLocationsCorrectly() {

        LOGGER.info("Testing multiple location resolution");

        Location tilburg = locationGateway.resolveByIdentifier("TILBURG-001");
        Location helmond = locationGateway.resolveByIdentifier("HELMOND-001");
        Location eindhoven = locationGateway.resolveByIdentifier("EINDHOVEN-001");

        assertNotNull(tilburg);
        assertNotNull(helmond);
        assertNotNull(eindhoven);

        LOGGER.info("Multiple locations resolved successfully");
    }

    // -------------------------
    // Negative Test Cases
    // -------------------------

    @Test
    void shouldReturnNullWhenLocationDoesNotExist() {

        LOGGER.info("Testing unknown location");

        Location location = locationGateway.resolveByIdentifier("UNKNOWN-001");

        assertNull(location);

        LOGGER.info("Unknown location handled correctly");
    }

    @Test
    void shouldBeCaseSensitive() {

        LOGGER.info("Testing case sensitivity");

        Location location = locationGateway.resolveByIdentifier("zwolle-001");

        assertNull(location);

        LOGGER.info("Case sensitivity validation passed");
    }

    // -------------------------
    // Edge / Error Test Cases
    // -------------------------

    @Test
    void shouldReturnNullWhenIdentifierIsNull() {

        LOGGER.info("Testing null identifier");

        Location location = locationGateway.resolveByIdentifier(null);

        assertNull(location);

        LOGGER.info("Null identifier handled correctly");
    }

    @Test
    void shouldReturnNullWhenIdentifierIsEmpty() {

        LOGGER.info("Testing empty identifier");

        Location location = locationGateway.resolveByIdentifier("");

        assertNull(location);

        LOGGER.info("Empty identifier handled correctly");
    }

    @Test
    void shouldReturnNullWhenIdentifierIsWhitespace() {

        LOGGER.info("Testing whitespace identifier");

        Location location = locationGateway.resolveByIdentifier("   ");

        assertNull(location);

        LOGGER.info("Whitespace identifier handled correctly");
    }
}