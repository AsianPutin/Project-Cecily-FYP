package com.app.projectcecily;

public class FlightInfo {
    public String id;
    public String flightNumber;
    public String airlineName = "Airline";
    public String depAirport = "KUL";
    public String depCity = "Kuala Lumpur";
    public String arrAirport = "KCH";
    public String arrCity = "Kuching";
    public String depTimeStr = "";
    public String arrTimeStr = "";
    public String depDateStr = "";
    public String arrDateStr = "";
    public String depTerminal = "-";
    public String depGate = "-";
    public String arrTerminal = "-";
    public String arrGate = "-";
    public String durationStr = "";

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
        this.depTerminal = "-";
        this.depGate = "-";
        this.arrTerminal = "-";
        this.arrGate = "-";
    }
}