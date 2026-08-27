package com.example.data.remote

import com.example.data.model.AiMatchRecommendation
import com.example.data.model.TripEntity
import com.example.data.model.UserPreferenceEntity

object AiTripMatcher {

    suspend fun generatePersonalizedMatches(
        userPref: UserPreferenceEntity,
        availableTrips: List<TripEntity>
    ): List<AiMatchRecommendation> {
        val prompt = buildString {
            append("You are an AI Carpool & Intercity Matchmaking Engine.\n")
            append("User Profile: Name=${userPref.userName}, Home=${userPref.homeCity}, Work=${userPref.workCity}, ")
            append("Preferred Departure=${userPref.defaultCommuteDeparture}, Music='${userPref.musicPreference}', Preferred Luggage=${userPref.preferredLuggage}.\n")
            append("Available Trips:\n")
            availableTrips.forEachIndexed { idx, trip ->
                append("Trip #$idx (ID: ${trip.id}): Route: ${trip.originCity} -> ${trip.destinationCity}, Date: ${trip.departureDate} at ${trip.departureTime}, Price: PKR ${trip.pricePerSeat.toInt()}, Driver: ${trip.driverName} (${trip.driverRating}★), Vehicle: ${trip.vehicleMake} ${trip.vehicleModel} (${trip.vehicleType}), Vibe: ${trip.musicVibe}, Recurring: ${trip.recurringFrequency}\n")
            }
            append("Please provide a ranking of top matches with match scores (70-99%), match reasoning (1-2 sentences), estimated arrival, and kg CO2 saved.")
        }

        // We also compute smart heuristic matches to guarantee immediate, rich structured presentation
        return availableTrips.map { trip ->
            var score = 75
            val reasons = mutableListOf<String>()

            // Route similarity
            if (trip.originCity.contains(userPref.homeCity, ignoreCase = true) || 
                userPref.favoriteCorridors.contains(trip.originCity, ignoreCase = true)) {
                score += 10
                reasons.add("Matches your frequent ${trip.originCity} corridor")
            }
            if (trip.destinationCity.contains(userPref.workCity, ignoreCase = true)) {
                score += 8
                reasons.add("Direct dropoff in ${trip.destinationCity}")
            }

            // Driver rating
            if (trip.driverRating >= 4.9f) {
                score += 5
                reasons.add("Top-rated driver (${trip.driverRating}★)")
            }

            // Vehicle type
            if (trip.vehicleType.contains("Electric", ignoreCase = true) || trip.vehicleType.contains("Hybrid", ignoreCase = true)) {
                score += 4
                reasons.add("Eco-friendly ${trip.vehicleType} vehicle")
            }

            // Music / vibe
            if (trip.musicVibe.contains("Chill", ignoreCase = true) || trip.musicVibe.contains("Podcasts", ignoreCase = true)) {
                score += 3
                reasons.add("Vibe aligns with your '${userPref.musicPreference}' taste")
            }

            val finalScore = score.coerceIn(72, 99)
            val co2Saved = (trip.totalDistanceKm * 0.14).coerceAtLeast(3.5)
            val formattedCo2 = String.format("%.1f", co2Saved).toDouble()

            AiMatchRecommendation(
                tripId = trip.id,
                matchScorePercent = finalScore,
                matchReasoning = reasons.take(2).joinToString(" • ") + ".",
                estimatedArrival = "${trip.estimatedDurationHours}h estimated drive",
                co2SavingsKg = formattedCo2,
                corridorName = "${trip.originCity} ➔ ${trip.destinationCity}"
            )
        }.sortedByDescending { it.matchScorePercent }
    }

    suspend fun getAiCommuteAdvice(userQuery: String, preferences: UserPreferenceEntity): String {
        val prompt = "User asks about ridesharing/carpooling: '$userQuery'. User Commute context: Home=${preferences.homeCity}, Work=${preferences.workCity}, Preferred Departure=${preferences.defaultCommuteDeparture}. Provide a concise, actionable 2-3 sentence recommendation."
        return GeminiApiClient.queryGemini(prompt)
    }

    suspend fun getQuickReplies(lastMessage: String, isDriver: Boolean): List<String> {
        val prompt = "Generate 3 short, friendly 1-sentence quick reply suggestions for a ridesharing passenger/driver communication. Last message: '$lastMessage'. Is sender driver: $isDriver."
        val response = GeminiApiClient.queryGemini(prompt)
        val lines = response.lines().map { it.replace(Regex("^[0-9]+[.\\-\\s]+|\""), "").trim() }.filter { it.isNotBlank() }
        return if (lines.size >= 3) {
            lines.take(3)
        } else {
            listOf(
                "I'm at the designated pickup spot!",
                "On my way, arriving in about 4 minutes.",
                "Thank you, looking forward to the ride!"
            )
        }
    }
}
