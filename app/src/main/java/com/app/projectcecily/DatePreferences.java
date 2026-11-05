package com.app.projectcecily;

public class DatePreferences {
    // LDR Preferences
    public boolean prefCoOpGames = true;
    public boolean prefCasualGames = true;
    public boolean prefAnimeMovies = true;
    public boolean prefHorrorMovies = true;
    public boolean prefRomanceMovies = true;

    // Together Mode Food Preferences
    public boolean prefLocalFood = true;         // Malaysian & Indian
    public boolean prefAsianFood = true;         // Japanese & Korean
    public boolean prefWesternFood = true;       // Western & Italian
    public boolean prefMiddleEasternFood = true; // Middle Eastern & Arabian
    public boolean prefCafes = true;             // Cafes, Desserts & Pastries

    // Together Mode Activity Preferences
    public boolean prefOutdoorNature = true;     // Parks, Waterfront & Beach
    public boolean prefEntertainment = true;     // Malls, Cinema, Arcade, Bowling, Karaoke, Escape Room
    public boolean prefCozyCreative = true;      // Board Game Cafe, Rooftop Lounge, Art & Pottery

    public DatePreferences() {} // Required for Firebase
}