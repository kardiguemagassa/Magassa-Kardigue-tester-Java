package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    // method calculateFare avec une remise tarif.
    public void calculateFare(Ticket ticket, boolean discount){

        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        //int inHour = ticket.getInTime().getHours();
        //int outHour = ticket.getOutTime().getHours();

        //TODO: Some tests are failing here. Need to check if this logic is correct
        //int duration = outHour - inHour;

        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        // convertir la durée en heure
        double durationInHours = (outTimeMillis - inTimeMillis) / (1000.0 * 60 * 60); // 1000ms/s * 60s/min * 60min/h

        // Si la durée est inférieure ou egale à 30 minutes, le prix est de 0
        if (durationInHours <= 0.5) { // 0.5 heure = 30 minutes
            ticket.setPrice(0.0);

            return;
        }

        // calcul de la durée
        double fare = 0.0;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                //ticket.setPrice(durationInHours * Fare.CAR_RATE_PER_HOUR);
                fare = durationInHours * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                //ticket.setPrice(durationInHours * Fare.BIKE_RATE_PER_HOUR);
                fare = durationInHours * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }

        if (discount) {
            fare *= 0.95; // 5% de reduction
        }
        ticket.setPrice(fare);
    }

    // method calculateFare plein tarif.
    public void calculateFare (Ticket ticket){

        calculateFare(ticket, false);
    }
}