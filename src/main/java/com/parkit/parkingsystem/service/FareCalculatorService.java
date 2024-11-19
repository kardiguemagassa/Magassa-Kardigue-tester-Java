package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean discount){

        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        double durationInHours = (outTimeMillis - inTimeMillis) / (1000.0 * 60 * 60);

        if (durationInHours <= 0.5) {
            ticket.setPrice(0.0);

            return;
        }

        double fare = 0.0;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                fare = durationInHours * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                fare = durationInHours * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unknown Parking Type");
        }

        if (discount) {
            fare *= 0.95;
        }
        ticket.setPrice(fare);
    }

    public void calculateFare (Ticket ticket){

        calculateFare(ticket, false);
    }
}