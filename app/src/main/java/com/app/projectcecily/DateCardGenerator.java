package com.app.projectcecily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DateCardGenerator {

    public static List<DateCard> generateDeck(boolean isSameCity, String cityName, DatePreferences prefs) {
        List<DateCard> deck = new ArrayList<>();

        if (isSameCity) {
            // TOGETHER MODE: GENERAL FOOD CUISINES & GENERAL DATE ACTIVITIES

            // 1. MALAYSIAN & INDIAN CUISINE
            if (prefs == null || prefs.prefDining) {
                deck.add(new DateCard("sc_food_nasilemak", "Nasi Lemak & Local Fare", "Malaysian Food",
                        "Classic fragrant coconut rice with spicy sambal, crispy fried chicken, boiled egg, and peanuts.",
                        "https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_food_nightmarket", "Street Food Night Market", "Street Food",
                        "Explore vibrant night market stalls for satay, lok lok, grilled skewers, and iced desserts.",
                        "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_food_indian", "Indian Cuisine & Roti Canai", "Indian Food",
                        "Enjoy fluffy roti canai, garlic naan, butter chicken, rich curry, and hot teh tarik.",
                        "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=800&q=80", false));
            }

            // 2. JAPANESE CUISINE
            if (prefs == null || prefs.prefDining) {
                deck.add(new DateCard("sc_food_ramen", "Ramen & Gyoza", "Japanese Food",
                        "Rich savory broth ramen bowls served with pan-fried gyoza and soft-boiled eggs.",
                        "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_food_sushi", "Sushi & Sashimi Bar", "Japanese Food",
                        "Fresh salmon, tuna sashimi platters, nigiri, and maki rolls.",
                        "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_food_izakaya", "Japanese Izakaya & Yakitori", "Japanese Food",
                        "Cozy Japanese pub setting with charcoal-grilled yakitori skewers and appetizers.",
                        "https://images.unsplash.com/photo-1552611052-33e04de081de?auto=format&fit=crop&w=800&q=80", false));
            }

            // 3. KOREAN CUISINE
            if (prefs == null || prefs.prefDining) {
                deck.add(new DateCard("sc_food_kbbq", "Korean BBQ & Fried Chicken", "Korean Food",
                        "Sizzling samgyeopsal, marinated beef, cheesy tteokbokki, and crispy Korean fried chicken.",
                        "https://images.unsplash.com/photo-1498654896293-37aacf113fd9?auto=format&fit=crop&w=800&q=80", false));
            }

            // 4. WESTERN & ITALIAN CUISINE
            if (prefs == null || prefs.prefDining) {
                deck.add(new DateCard("sc_food_pizza", "Wood-Fired Pizza & Pasta", "Italian Food",
                        "Hand-tossed Neapolitan pizza, creamy fettuccine carbonara, and garlic bread.",
                        "https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_food_burger", "Gourmet Burgers & Loaded Fries", "Western Food",
                        "Juicy double beef smash burgers, melted cheddar, and truffle fries.",
                        "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_food_steak", "Sizzling Steakhouse Grill", "Western Food",
                        "Premium ribeye or sirloin steaks served hot on sizzling plates with peppercorn sauce.",
                        "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80", false));
            }

            // 5. MIDDLE EASTERN CUISINE
            if (prefs == null || prefs.prefDining) {
                deck.add(new DateCard("sc_food_middleeastern", "Arabian Mandi & Kebab Grill", "Middle Eastern Food",
                        "Tender lamb mandi rice, grilled shish tawook, hummus, and warm pita bread.",
                        "https://images.unsplash.com/photo-1541518763669-27fef04b14e8?auto=format&fit=crop&w=800&q=80", false));
            }

            // 6. COZY CAFES & DESSERTS
            if (prefs == null || prefs.prefCafes) {
                deck.add(new DateCard("sc_cafe_artisan", "Artisanal Coffee & Matcha", "Cafe & Drinks",
                        "Specialty pour-overs, Japanese Uji matcha, and cozy cafe atmosphere.",
                        "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_dessert_souffle", "Souffle Pancakes & Boba", "Desserts",
                        "Fluffy Japanese souffle pancakes, waffles, and brown sugar boba tea.",
                        "https://images.unsplash.com/photo-1551024709-8f23befc6f87?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_dessert_bakery", "Bakery & French Pastries", "Pastries & Sweets",
                        "Butter croissants, fruit tarts, macarons, and hot chocolate.",
                        "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=800&q=80", false));
            }

            // GENERAL DATE LOCATIONS & ACTIVITIES
            if (prefs == null || prefs.prefOutdoorParks) {
                deck.add(new DateCard("sc_act_park", "Public Park & Waterfront Walk", "Outdoor Activity",
                        "Take a relaxing stroll through the park or along the waterfront watching the sunset.",
                        "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?auto=format&fit=crop&w=800&q=80", false));
                deck.add(new DateCard("sc_act_beach", "Beach & Picnic Spot", "Outdoor Activity",
                        "Relax on a cozy blanket with sea breeze, snacks, and ocean views.",
                        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80", false));
            }

            deck.add(new DateCard("sc_act_mall", "Shopping Mall & Window Shopping", "Mall Outing",
                    "Browse stores, grab drinks, check out pop-up shops, and walk around the mall.",
                    "https://images.unsplash.com/photo-1567449303078-57ad995bd301?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_cinema", "Cinema & Movie Night", "Entertainment",
                    "Catch the latest movie on the big screen with popcorn and drinks.",
                    "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_arcade", "Arcade & Claw Machines", "Games & Fun",
                    "Play claw machines, racing simulators, air hockey, and basketball arcade games.",
                    "https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_bowling", "Bowling Alley", "Games & Fun",
                    "Enjoy a fun bowling match together with arcade games and snacks.",
                    "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_karaoke", "Karaoke Lounge", "Entertainment",
                    "Sing your favorite duet songs and pop hits in a private karaoke room.",
                    "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_escaperoom", "Escape Room", "Entertainment",
                    "Solve puzzles, decipher codes, and escape a themed mystery room together.",
                    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_boardgame", "Board Game Cafe", "Cozy Activity",
                    "Try fun strategic board games, card games, and party games over coffee.",
                    "https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_rooftop", "Rooftop Lounge & Night View", "Nightlife",
                    "Enjoy night city views, mocktails, and evening breeze from a rooftop spot.",
                    "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80", false));

            deck.add(new DateCard("sc_act_art", "Art & Pottery Workshop", "Creative Outing",
                    "Paint canvases or try hands-on pottery making together.",
                    "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=800&q=80", false));

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
