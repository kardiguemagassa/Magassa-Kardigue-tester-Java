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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** @ExtendWith: => simplifie la configuration des tests en utilisant Mockito.
Il permet d'injecter automatiquement des mocks dans les tests */
@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {
    private static final Logger logger = LogManager.getLogger("ParkingDataBaseIT");
    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static ParkingService parkingService;

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
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        logger.info("Je rentre dans la méthode testParkingACar()");

        parkingService.processIncomingVehicle();

        Ticket geTicketSaved = ticketDAO.getTicket("ABCDEF");

        logger.info("geTicketSaved dans testParkingACar" + geTicketSaved);
        assertNotNull(geTicketSaved);

        boolean updatedParking = parkingSpotDAO.updateParking(geTicketSaved.getParkingSpot());
        assertTrue(updatedParking);

    }

    @Test
    public void testParkingLotExit() {
        testParkingACar();
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        logger.info("Je rentre dans la méthode testParkingLotExit()");

        parkingService.processExitingVehicle();

        Ticket ticketSaved = ticketDAO.getTicket("ABCDEF");
        logger.info("ticketSaved" + ticketSaved);

        logger.info("ticketSaved outtime" + ticketSaved.getOutTime());

        boolean updatedTicket = ticketDAO.updateTicket(ticketSaved);
        assertTrue(updatedTicket);
    }

    @Test
    @DisplayName("Vérifier la remise de 5% pour un utilisateur récurrent")
    public void testParkingLotExitRecurringUser() {

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        /** Première entrée et sortie (utilisateur non récurrent) */
        ParkingSpot parkingSpot1 = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket1 = new Ticket();
        ticket1.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure avant
        ticket1.setOutTime(new Date());
        ticket1.setVehicleRegNumber("ABCDEF");
        ticket1.setParkingSpot(parkingSpot1);
        ticketDAO.saveTicket(ticket1);

        parkingService.processExitingVehicle();

        /** Deuxième entrée et sortie pour l'utilisateur récurrent */
        ParkingSpot parkingSpot2 = new ParkingSpot(2, ParkingType.CAR, false);
        Ticket ticket2 = new Ticket();
        ticket2.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure avant
        ticket2.setOutTime(new Date());
        ticket2.setVehicleRegNumber("ABCDEF");
        ticket2.setParkingSpot(parkingSpot2);
        ticketDAO.saveTicket(ticket2);

        parkingService.processExitingVehicle();

        /** Récupérer le ticket de l'utilisateur récurrent */
        Ticket ticketSaved = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketSaved.getOutTime(), "L'heure de sortie ne doit pas être null.");
        assertNotNull(ticketSaved.getParkingSpot(), "La place de parking ne doit pas être null.");

        /** Vérifier le nombre de tickets pour cet utilisateur (doit être supérieur à 1) */
        int nbOfTickets = ticketDAO.getNbTicket("ABCDEF");
        assertTrue(nbOfTickets > 1, "L'utilisateur doit être marqué comme récurrent.");

        /** Vérifier que la remise de 5% est appliquée */
        double expectedFare = Fare.CAR_RATE_PER_HOUR * 0.95;
        assertEquals(expectedFare, ticketSaved.getPrice(), 0.01, "La remise de 5% doit être appliquée.");
    }
}
