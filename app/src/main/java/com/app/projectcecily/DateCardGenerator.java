package com.app.projectcecily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DateCardGenerator {

    public static List<DateCard> generateDeck(boolean isSameCity, String cityName, DatePreferences prefs) {
        List<DateCard> deck = new ArrayList<>();

        if (isSameCity) {
            // SAME CITY DATE IDEAS (Food, Cafes, Parks, Local Outings)
            String city = (cityName != null && !cityName.isEmpty() && !cityName.equalsIgnoreCase("unknown city")) ? cityName : "Your City";

            if (prefs == null || prefs.prefCafes) {
                deck.add(new DateCard("sc_cafe_1", "Cozy Artisanal Cafe", "Cafe & Coffee",
                        "Explore a warm, aesthetic local cafe in " + city + " for lattes, matcha, and pastries.",
                        "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80", false));
            }
            if (prefs == null || prefs.prefDining) {
                deck.add(new DateCard("sc_dining_1", "Candlelight Italian Dining", "Romantic Dinner",
                        "Share pasta, wine, and wood-fired pizza in a cozy romantic restaurant in " + city + ".",
                        "https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_ramen_1", "Japanese Sushi & Ramen Night", "Food & Dining",
                        "Warm up with rich tonkotsu ramen bowls and fresh sushi platters.",
                        "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=80", false));
            }
            if (prefs == null || prefs.prefOutdoorParks) {
                deck.add(new DateCard("sc_park_1", "Sunset Waterfront Park Walk", "Outdoor Date",
                        "Take a peaceful evening stroll along " + city + "'s scenic waterfront park as the sun sets.",
                        "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_nightmarket_1", "Vibrant Street Food Night Market", "Local Food",
                        "Taste local street food snacks, bubble tea, and skewers at the night market in " + city + ".",
                        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80", false));
            }
            deck.add(new DateCard("sc_dessert_1", "Boba Tea & Dessert Spot", "Desserts",
                    "Grab fluffy souffle pancakes, waffles, and fresh boba drinks together.",
                    "https://images.unsplash.com/photo-1551024709-8f23befc6f87?auto=format&fit=crop&w=800&q=80", false));

        } else {
            // LDR MODE DATE IDEAS (Games, Movie Watch Parties, Virtual Dates)

            // CO-OP GAMES
            if (prefs == null || prefs.prefCoOpGames) {
                deck.add(new DateCard("ldr_game_1", "It Takes Two (Co-Op)", "Co-Op Video Game",
                        "The ultimate award-winning 2-player co-op adventure game! Solve puzzles and platform together.",
                        "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=800&q=80", true));
                deck.add(new DateCard("ldr_game_2", "Stardew Valley Co-Op", "Cozy Farming Game",
                        "Build a peaceful farm, raise animals, go fishing, and explore the mines together online.",
                        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80", true));
                deck.add(new DateCard("ldr_game_3", "Overcooked 2 Chaos", "Fun Kitchen Co-Op",
                        "Cook and serve orders under chaotic pressure! Test your communication skills in this hilarious game.",
                        "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=800&q=80", true));
                deck.add(new DateCard("ldr_game_4", "Minecraft World Building", "Creative World",
                        "Build your dream cozy house, explore caves, and survive the night together in your own server.",
                        "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80", true));
            }

            if (prefs == null || prefs.prefCasualGames) {
                deck.add(new DateCard("ldr_game_5", "Roblox Obby & Hangout", "Casual Multiplayer",
                        "Explore fun Roblox obstacle courses, horror games, or cozy virtual cafes together.",
                        "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80", true));
                deck.add(new DateCard("ldr_game_6", "Portal 2 Co-Op Testing", "Puzzle Game",
                        "Play as Atlas and P-Body solving mind-bending portal physics puzzles together.",
                        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80", true));
            }

            // MOVIE & SHOW WATCH PARTIES
            if (prefs == null || prefs.prefAnimeMovies) {
                deck.add(new DateCard("ldr_movie_1", "Anime Movie Binge Night", "Watch Party",
                        "Stream 'Your Name', 'Weathering With You', or Studio Ghibli classics together on Discord or Teleparty.",
                        "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80", true));
            }
            if (prefs == null || prefs.prefHorrorMovies) {
                deck.add(new DateCard("ldr_movie_2", "Late-Night Horror Movie", "Spooky Movie Night",
                        "Turn down the lights, get on voice call, and watch a spine-chilling horror movie together.",
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80", true));
            }
            if (prefs == null || prefs.prefRomanceMovies) {
                deck.add(new DateCard("ldr_movie_3", "K-Drama / Rom-Com Marathon", "Romance Binge",
                        "Cuddle up with blankets online and watch heart-warming romance shows together.",
                        "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=800&q=80", true));
            }

            // VIRTUAL DATES
            deck.add(new DateCard("ldr_vdate_1", "Shared Spotify Jam Session", "Music Date",
                    "Listen to the exact same music playlist in real time on Spotify while chatting on call.",
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80", true));
            deck.add(new DateCard("ldr_vdate_2", "Online Couple's Quiz & Trivia", "Virtual Date",
                    "Play online couple quizzes, 'Who Knows Who Better?', and fun trivia games on call.",
                    "https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80", true));
            deck.add(new DateCard("ldr_vdate_3", "Discord Meal Date", "Virtual Dinner",
                    "Order your favourite food, turn on video call, and enjoy a virtual candlelit dinner together.",
                    "https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=800&q=80", true));
        }

        // Randomize the card deck every time!
        Collections.shuffle(deck);
        return deck;
    }
}