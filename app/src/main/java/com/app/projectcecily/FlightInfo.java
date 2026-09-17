package com.app.projectcecily;

public class FlightInfo {
    public String id;
    public String flightNumber;
    public String depAirport;
    public String arrAirport;
    public long depTimeMillis;
    public long estimatedArrivalMillis;
    public String status; // ON TIME, DELAYED, ACTIVE, LANDED
    public String lastNotifiedStatus;
    public double currentLat;
    public double currentLon;
    public String ownerId;

    public FlightInfo() {} // Firebase

    public FlightInfo(String id, String flightNumber, String depAirport, String arrAirport, long depTimeMillis, String ownerId) {
        this.id = id;
        this.flightNumber = flightNumber;
        this.depAirport = depAirport;
        this.arrAirport = arrAirport;
        this.depTimeMillis = depTimeMillis;
        this.estimatedArrivalMillis = depTimeMillis + (2 * 60 * 60 * 1000); // Default 2 hours
        this.ownerId = ownerId;
        this.status = "SCHEDULED";
        this.lastNotifiedStatus = "SCHEDULED";
    }
}