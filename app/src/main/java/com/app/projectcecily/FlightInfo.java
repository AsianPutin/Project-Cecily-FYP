package com.app.projectcecily;

public class FlightInfo {
    public String id;
    public String flightNumber;
    public String airlineName = "AirAsia";
    public String depAirport = "KUL";
    public String depCity = "Kuala Lumpur";
    public String arrAirport = "KCH";
    public String arrCity = "Kuching";
    public String depTimeStr = "7:17 am";
    public String arrTimeStr = "9:04 am";
    public String depDateStr = "Sat, 31 Oct";
    public String arrDateStr = "Sat, 31 Oct";
    public String depTerminal = "2";
    public String depGate = "J10";
    public String arrTerminal = "-";
    public String arrGate = "-";
    public String durationStr = "1h 47m";

    public long depTimeMillis;
    public long estimatedArrivalMillis;
    public String status = "SCHEDULED";
    public String lastNotifiedStatus = "SCHEDULED";
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
        this.estimatedArrivalMillis = depTimeMillis + (2 * 60 * 60 * 1000);
        this.ownerId = ownerId;
    }
}