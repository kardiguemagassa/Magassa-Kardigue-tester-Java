package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static junit.framework.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**@ExtendWith: => simplifie la configuration des tests en utilisant Mockito.
Il permet d'injecter automatiquement des mocks dans les tests */
@ExtendWith({MockitoExtension.class})
public class ParkingServiceTest {

    private static final Logger logger = LogManager.getLogger("ParkingServiceTest");

    @InjectMocks
    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    String vehicleRegNumber;
    int id;
    Ticket ticket;

    @BeforeEach
    public void setUpPerTest() {

        logger.info("Méthode setUpPerTest()");

        try {
            //		GIVEN
            lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");

            lenient().when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            lenient().when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void processExitingVehicleTest(){

        /**
         La méthode when() est utilisée pour la configuration des mocks (stubbing),
         tandis que verify() est utilisée pour vérifier que des méthodes ont été appelées un certain nombre de fois.
         */

        parkingService.processExitingVehicle();

        // Vérifier que les méthodes sont appelées exactement une fois
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");// Vérifier que getNbTicket est appelé une fois
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));// Vérifier la mise à jour du ticket
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));// Vérifier la mise à jour du parking
    }

    @Test
    public void testProcessIncomingVehicle() {
        //		WHEN

        // Simuler une entrée correcte pour le type de véhicule
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 car

        // Assurer que la place de parking est disponible
        when(parkingSpotDAO.getNextAvailableSlot((ParkingType.CAR))).thenReturn(1);

        parkingService.processIncomingVehicle();

        //		THEN
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(ticketDAO, times(1)).saveTicket(any(Ticket.class));
    }

    @Test
    public void testProcessExitingVehicleTestUnableUpdate () {
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class)); // Vérifier que le parking n'est pas mis à jour si l'update du ticket échoue
    }

    @Test
    public void testGetNextParkingNumberIfAvailable () {

        // Simuler l'entrée de l'utilisateur pour sélectionner le type de véhicule
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 pour CAR

        // Configurer le mock pour retourner une place disponible pour CAR
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        // Appeler la méthode à tester
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // Vérifier que le DAO a bien été appelé pour obtenir la prochaine place disponible
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);

        // Vérifier que la place de parking retournée n'est pas nulle et est configurée comme attendu
        assert parkingSpot != null;
        assert parkingSpot.getId() == 1;
        assert parkingSpot.getParkingType() == ParkingType.CAR;
        assert parkingSpot.isAvailable();
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound () {
        // WHEN
        when(inputReaderUtil.readSelection()).thenReturn(1); // 1 pour CAR
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(-1);

        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(ParkingType.CAR);
        assertNull(parkingSpot); // Vérifie que parkingSpot est nul si aucune place n'est trouvée
    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument () {
        /**

         assertThrows(IllegalArgumentException.class, () -> {
         parkingService.getNextParkingNumberIfAvailable();
         });

         verify(parkingSpotDAO, never()).getNextAvailableSlot(any(ParkingType.class)); // Vérifie l'appel avec un argument incorrect

         */

        // WHEN
        when(inputReaderUtil.readSelection()).thenReturn(3);

        assertThrows(IllegalArgumentException.class, () -> parkingService.getVehichleType(),
                "Entered input is invalid");
        Assertions.assertNull(parkingService.getNextParkingNumberIfAvailable());
    }

    /** @DisplayName: => permet de nommer les tests de façon lisible par tout */
    @Test
    @DisplayName("processExitingVehicleCheckThatUpdateParkingMethodCalledTest()")
    public void processExitingVehicleCheckThatUpdateParkingMethodCalledTest() {

        logger.debug("Je rentre dans la méthode processExitingVehicleTest()");

        try {

            // GIVEN
            vehicleRegNumber = "ABCDEF";
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

            ticket = new Ticket(id, parkingSpot, vehicleRegNumber, 1.25,
                    new Date(System.currentTimeMillis() - (60 * 60 * 1000)), new Date());

            // WHEN
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
            when(ticketDAO.getTicket(vehicleRegNumber)).thenReturn(ticket);
            when(ticketDAO.updateTicket(ticket)).thenReturn(true);
            when(ticketDAO.getNbTicket(vehicleRegNumber)).thenReturn(4);

            parkingService.processExitingVehicle();

            // THEN
            verify(parkingSpotDAO).updateParking(parkingSpot);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}