package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {
    private static final Logger logger = LogManager.getLogger("ParkingDataBaseIT");

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;

        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;

        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1); // Car type vehicle
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries(); // Clean database before each test
    }

    @AfterAll
    public static void tearDown() {
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        logger.info("Starting test: testParkingACar");

        parkingService.processIncomingVehicle();

        Ticket ticketSaved = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketSaved, "The ticket should not be null.");
        assertEquals("ABCDEF", ticketSaved.getVehicleRegNumber(), "Vehicle registration number should match.");

        ParkingSpot parkingSpot = parkingSpotDAO.getParkingSpot(ticketSaved.getParkingSpot().getId());
        assertNotNull(parkingSpot, "Parking spot should not be null.");
        assertFalse(parkingSpot.isAvailable(), "The parking spot should be marked as occupied.");
    }

    @Test
    public void testParkingLotExit() {
        testParkingACar(); // Simulate vehicle entry

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processExitingVehicle();

        Ticket ticketSaved = ticketDAO.getTicket("ABCDEF");
        logger.info("ticketSaved" + ticketSaved);
        logger.info("ticketSaved outtime" + ticketSaved.getOutTime());

        assertNotNull(ticketSaved, "The ticket should not be null.");
        assertNotNull(ticketSaved.getOutTime(), "The out time should not be null.");
        assertTrue(ticketDAO.updateTicket(ticketSaved), "The ticket should be updated successfully.");
    }

    @Test
    @DisplayName("Verify 5% discount for recurring users")
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // First parking session
        Ticket firstTicket = new Ticket();
        firstTicket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 hour ago
        firstTicket.setOutTime(new Date());
        firstTicket.setVehicleRegNumber("ABCDEF");
        firstTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticketDAO.saveTicket(firstTicket);

        parkingService.processExitingVehicle();

        // Second parking session (Recurring user)
        Ticket secondTicket = new Ticket();
        secondTicket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 hour ago
        secondTicket.setOutTime(new Date());
        secondTicket.setVehicleRegNumber("ABCDEF");
        secondTicket.setParkingSpot(new ParkingSpot(2, ParkingType.CAR, false));
        ticketDAO.saveTicket(secondTicket);

        parkingService.processExitingVehicle();

        // Verify user is recurring and discount is applied
        Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(savedTicket.getOutTime(), "The out time should not be null.");
        assertTrue(ticketDAO.getNbTicket("ABCDEF") > 1, "The user should be marked as recurring.");

        double expectedFare = Fare.CAR_RATE_PER_HOUR * 0.95; // 5% discount
        assertEquals(expectedFare, savedTicket.getPrice(), 0.01, "5% discount should be applied.");
    }
}
