package com.example.data.local

import com.example.data.model.BookingEntity
import com.example.data.model.BookingStatus
import com.example.data.model.ChatMessageEntity
import com.example.data.model.LuggageAllowance
import com.example.data.model.RecurringFrequency
import com.example.data.model.TripEntity
import com.example.data.model.TripStatus
import com.example.data.model.UserPreferenceEntity
import org.json.JSONArray
import org.json.JSONObject

object SampleDataProvider {

    fun waypointsToJson(points: List<Map<String, Any>>): String {
        val array = JSONArray()
        for (p in points) {
            val obj = JSONObject()
            obj.put("name", p["name"])
            obj.put("address", p["address"])
            obj.put("lat", p["lat"])
            obj.put("lon", p["lon"])
            obj.put("estimatedMinutesFromStart", p["estimatedMinutesFromStart"])
            obj.put("pickupPrice", p["pickupPrice"])
            array.put(obj)
        }
        return array.toString()
    }

    // 1. Islamabad to Lahore via M-2 Motorway
    val trip1Waypoints = waypointsToJson(listOf(
        mapOf("name" to "Islamabad Zero Point", "address" to "Kashmir Highway / Zero Point Interchange, Islamabad", "lat" to 33.6844, "lon" to 73.0479, "estimatedMinutesFromStart" to 0, "pickupPrice" to 0.0),
        mapOf("name" to "Islamabad Toll Plaza (M-2)", "address" to "M-2 Motorway Entry Toll, Islamabad", "lat" to 33.5651, "lon" to 72.8552, "estimatedMinutesFromStart" to 25, "pickupPrice" to 600.0),
        mapOf("name" to "Bhera Service Area (M-2)", "address" to "M-2 Motorway Midway Service Area, Bhera", "lat" to 32.4821, "lon" to 72.9097, "estimatedMinutesFromStart" to 110, "pickupPrice" to 1200.0),
        mapOf("name" to "Sukheke Interchange", "address" to "M-2 Sukheke Exit & Rest Stop", "lat" to 31.8624, "lon" to 73.5042, "estimatedMinutesFromStart" to 175, "pickupPrice" to 1500.0),
        mapOf("name" to "Lahore Thokar Niaz Baig", "address" to "M-2 Motorway Terminal, Thokar Niaz Baig, Lahore", "lat" to 31.4697, "lon" to 74.2498, "estimatedMinutesFromStart" to 240, "pickupPrice" to 1800.0)
    ))

    // 2. Karachi to Hyderabad via M-9 Motorway
    val trip2Waypoints = waypointsToJson(listOf(
        mapOf("name" to "Karachi Sohrab Goth", "address" to "Super Highway / Sohrab Goth Terminal, Karachi", "lat" to 24.9462, "lon" to 67.0822, "estimatedMinutesFromStart" to 0, "pickupPrice" to 0.0),
        mapOf("name" to "Karachi Toll Plaza (M-9)", "address" to "M-9 Motorway Main Toll Plaza, Karachi", "lat" to 25.0189, "lon" to 67.2185, "estimatedMinutesFromStart" to 20, "pickupPrice" to 350.0),
        mapOf("name" to "Nooriabad Industrial Stop", "address" to "M-9 Midway Restaurant & Fuel Stop, Nooriabad", "lat" to 25.1328, "lon" to 67.8920, "estimatedMinutesFromStart" to 60, "pickupPrice" to 650.0),
        mapOf("name" to "Hyderabad Autobahn", "address" to "Autobahn Road / Latifabad, Hyderabad", "lat" to 25.3783, "lon" to 68.3582, "estimatedMinutesFromStart" to 120, "pickupPrice" to 950.0)
    ))

    // 3. Rawalpindi to Islamabad Daily Office Commute
    val trip3Waypoints = waypointsToJson(listOf(
        mapOf("name" to "Saddar Rawalpindi Metro", "address" to "Saddar Commercial Center / Mall Road, Rawalpindi", "lat" to 33.5973, "lon" to 73.0489, "estimatedMinutesFromStart" to 0, "pickupPrice" to 0.0),
        mapOf("name" to "Faizabad Interchange", "address" to "Murree Road / IJP Road Junction, Faizabad", "lat" to 33.6631, "lon" to 73.0841, "estimatedMinutesFromStart" to 15, "pickupPrice" to 180.0),
        mapOf("name" to "Zero Point Interchange", "address" to "Faisal Avenue / Kashmir Highway, Islamabad", "lat" to 33.6844, "lon" to 73.0479, "estimatedMinutesFromStart" to 25, "pickupPrice" to 260.0),
        mapOf("name" to "Blue Area / Centaurus", "address" to "Jinnah Avenue, Blue Area Business District, Islamabad", "lat" to 33.7077, "lon" to 73.0501, "estimatedMinutesFromStart" to 35, "pickupPrice" to 350.0)
    ))

    // 4. Lahore to Faisalabad via M-3 Motorway
    val trip4Waypoints = waypointsToJson(listOf(
        mapOf("name" to "Lahore Thokar Niaz Baig", "address" to "Multan Road / Canal Bank, Lahore", "lat" to 31.4697, "lon" to 74.2498, "estimatedMinutesFromStart" to 0, "pickupPrice" to 0.0),
        mapOf("name" to "Sheikhupura Interchange", "address" to "M-3 Sheikhupura Bypass Junction", "lat" to 31.7131, "lon" to 73.9783, "estimatedMinutesFromStart" to 30, "pickupPrice" to 450.0),
        mapOf("name" to "Samundri Road Interchange", "address" to "M-3 Samundri Exit", "lat" to 31.2589, "lon" to 73.2201, "estimatedMinutesFromStart" to 75, "pickupPrice" to 900.0),
        mapOf("name" to "Faisalabad Canal Road", "address" to "Canal Expressway & D-Ground, Faisalabad", "lat" to 31.4187, "lon" to 73.0791, "estimatedMinutesFromStart" to 110, "pickupPrice" to 1200.0)
    ))

    // 5. Peshawar to Islamabad via M-1 Motorway
    val trip5Waypoints = waypointsToJson(listOf(
        mapOf("name" to "Peshawar Chamkani", "address" to "GT Road / Ring Road Chamkani Interchange, Peshawar", "lat" to 34.0042, "lon" to 71.6429, "estimatedMinutesFromStart" to 0, "pickupPrice" to 0.0),
        mapOf("name" to "Rashakai Interchange", "address" to "M-1 Rashakai CPEC Special Zone", "lat" to 34.0931, "lon" to 71.9792, "estimatedMinutesFromStart" to 25, "pickupPrice" to 400.0),
        mapOf("name" to "Swabi Interchange (M-1)", "address" to "M-1 Swabi Rest Area & Fuel Stop", "lat" to 34.0722, "lon" to 72.4510, "estimatedMinutesFromStart" to 55, "pickupPrice" to 800.0),
        mapOf("name" to "Islamabad G-11 Markaz", "address" to "Kashmir Highway, G-11 Markaz, Islamabad", "lat" to 33.6693, "lon" to 72.9984, "estimatedMinutesFromStart" to 110, "pickupPrice" to 1400.0)
    ))

    val initialTrips = listOf(
        TripEntity(
            id = "trip_isb_lhr_1",
            driverId = "driver_hamza",
            driverName = "Hamza Tariq",
            driverRating = 4.96f,
            driverTotalRides = 248,
            driverPhone = "+92 300 8492011",
            originCity = "Islamabad",
            originAddress = "Zero Point Interchange, Islamabad",
            originLat = 33.6844,
            originLon = 73.0479,
            destinationCity = "Lahore",
            destinationAddress = "Thokar Niaz Baig Terminal, Lahore",
            destinationLat = 31.4697,
            destinationLon = 74.2498,
            departureDate = "Tomorrow, Aug 26",
            departureTime = "07:30 AM",
            estimatedDurationHours = 4.0,
            totalDistanceKm = 375.0,
            pricePerSeat = 1800.0,
            totalSeats = 4,
            availableSeats = 2,
            vehicleMake = "Toyota",
            vehicleModel = "Corolla Altis Grande 1.8",
            vehicleColor = "Super White",
            vehiclePlate = "ICT-8821",
            vehicleType = "Sedan (Climate Control)",
            luggageAllowance = LuggageAllowance.MEDIUM,
            recurringFrequency = RecurringFrequency.WEEKDAYS,
            recurringDays = "Mon, Tue, Wed, Thu, Fri",
            waypointsJson = trip1Waypoints,
            status = TripStatus.EN_ROUTE,
            allowsPets = false,
            allowsSmoking = false,
            maxTwoInBack = true,
            musicVibe = "Coke Studio & Podcasts",
            specialNotes = "Direct M-2 Motorway drive! Fast M-Tag enabled tolls. Quick 15-min tea stop at Bhera.",
            instantBooking = true
        ),
        TripEntity(
            id = "trip_khi_hyd_2",
            driverId = "driver_ali",
            driverName = "Ali Raza Khan",
            driverRating = 4.90f,
            driverTotalRides = 160,
            driverPhone = "+92 333 7109283",
            originCity = "Karachi",
            originAddress = "Sohrab Goth Terminal, Karachi",
            originLat = 24.9462,
            originLon = 67.0822,
            destinationCity = "Hyderabad",
            destinationAddress = "Autobahn Road, Hyderabad",
            destinationLat = 25.3783,
            destinationLon = 68.3582,
            departureDate = "Friday, Aug 28",
            departureTime = "04:00 PM",
            estimatedDurationHours = 2.0,
            totalDistanceKm = 160.0,
            pricePerSeat = 950.0,
            totalSeats = 3,
            availableSeats = 1,
            vehicleMake = "Honda",
            vehicleModel = "Civic Oriel 1.5T",
            vehicleColor = "Lunar Silver",
            vehiclePlate = "KHI-4902",
            vehicleType = "Sedan (Executive)",
            luggageAllowance = LuggageAllowance.LARGE,
            recurringFrequency = RecurringFrequency.WEEKLY,
            recurringDays = "Fri, Sun",
            waypointsJson = trip2Waypoints,
            status = TripStatus.SCHEDULED,
            allowsPets = false,
            allowsSmoking = false,
            maxTwoInBack = true,
            musicVibe = "Sufi Rock & Chill",
            specialNotes = "Spacious trunk for extra luggage. Dual-zone AC with phone charging ports.",
            instantBooking = true
        ),
        TripEntity(
            id = "trip_rwp_isb_3",
            driverId = "driver_usman",
            driverName = "Usman Farooq",
            driverRating = 4.98f,
            driverTotalRides = 410,
            driverPhone = "+92 321 5590412",
            originCity = "Rawalpindi",
            originAddress = "Saddar Metro Station, Rawalpindi",
            originLat = 33.5973,
            originLon = 73.0489,
            destinationCity = "Islamabad",
            destinationAddress = "Blue Area (Centaurus), Islamabad",
            destinationLat = 33.7077,
            destinationLon = 73.0501,
            departureDate = "Daily (Mon-Fri)",
            departureTime = "08:15 AM",
            estimatedDurationHours = 0.6,
            totalDistanceKm = 22.0,
            pricePerSeat = 350.0,
            totalSeats = 4,
            availableSeats = 3,
            vehicleMake = "Suzuki",
            vehicleModel = "Alto VXR AGS",
            vehicleColor = "Silky Silver",
            vehiclePlate = "RWP-9011",
            vehicleType = "Compact City Car",
            luggageAllowance = LuggageAllowance.SMALL,
            recurringFrequency = RecurringFrequency.WEEKDAYS,
            recurringDays = "Mon, Tue, Wed, Thu, Fri",
            waypointsJson = trip3Waypoints,
            status = TripStatus.SCHEDULED,
            allowsPets = false,
            allowsSmoking = false,
            maxTwoInBack = true,
            musicVibe = "Morning News & FM 100",
            specialNotes = "Fast daily office commute via Islamabad Expressway and Faisal Avenue.",
            instantBooking = true
        ),
        TripEntity(
            id = "trip_lhr_fsd_4",
            driverId = "driver_bilal",
            driverName = "Bilal Ahmed",
            driverRating = 4.92f,
            driverTotalRides = 115,
            driverPhone = "+92 301 4488921",
            originCity = "Lahore",
            originAddress = "Thokar Niaz Baig, Lahore",
            originLat = 31.4697,
            originLon = 74.2498,
            destinationCity = "Faisalabad",
            destinationAddress = "Canal Road & D-Ground, Faisalabad",
            destinationLat = 31.4187,
            destinationLon = 73.0791,
            departureDate = "Saturday, Aug 29",
            departureTime = "09:30 AM",
            estimatedDurationHours = 1.8,
            totalDistanceKm = 180.0,
            pricePerSeat = 1200.0,
            totalSeats = 3,
            availableSeats = 2,
            vehicleMake = "Kia",
            vehicleModel = "Sportage AWD",
            vehicleColor = "Panthera Metal",
            vehiclePlate = "LEB-7744",
            vehicleType = "Compact SUV",
            luggageAllowance = LuggageAllowance.LARGE,
            recurringFrequency = RecurringFrequency.NONE,
            recurringDays = "",
            waypointsJson = trip4Waypoints,
            status = TripStatus.SCHEDULED,
            allowsPets = false,
            allowsSmoking = false,
            maxTwoInBack = true,
            musicVibe = "Pop & Audiobooks",
            specialNotes = "Smooth drive on M-3 Motorway. Comfortable high-seating SUV.",
            instantBooking = true
        ),
        TripEntity(
            id = "trip_pew_isb_5",
            driverId = "driver_shahid",
            driverName = "Shahid Afridi",
            driverRating = 4.97f,
            driverTotalRides = 330,
            driverPhone = "+92 345 9901234",
            originCity = "Peshawar",
            originAddress = "Chamkani Interchange, Peshawar",
            originLat = 34.0042,
            originLon = 71.6429,
            destinationCity = "Islamabad",
            destinationAddress = "G-11 Markaz, Islamabad",
            destinationLat = 33.6693,
            destinationLon = 72.9984,
            departureDate = "Sunday, Aug 30",
            departureTime = "02:00 PM",
            estimatedDurationHours = 1.9,
            totalDistanceKm = 165.0,
            pricePerSeat = 1400.0,
            totalSeats = 4,
            availableSeats = 2,
            vehicleMake = "Changan",
            vehicleModel = "Alsvin Lumiere 1.5",
            vehicleColor = "Cosmic Red",
            vehiclePlate = "PEW-2210",
            vehicleType = "Modern Sedan",
            luggageAllowance = LuggageAllowance.MEDIUM,
            recurringFrequency = RecurringFrequency.WEEKLY,
            recurringDays = "Sun",
            waypointsJson = trip5Waypoints,
            status = TripStatus.SCHEDULED,
            allowsPets = false,
            allowsSmoking = false,
            maxTwoInBack = true,
            musicVibe = "Pashto Folk & Instrumental",
            specialNotes = "Scenic M-1 Motorway route crossing Indus River bridge at Attock. M-Tag enabled.",
            instantBooking = true
        )
    )

    val initialBooking = BookingEntity(
        id = "booking_sample_1",
        tripId = "trip_isb_lhr_1",
        passengerId = "current_user",
        passengerName = "Naeem Ullah",
        passengerPhone = "+92 300 1234567",
        pickupLocation = "Islamabad Toll Plaza (M-2)",
        dropoffLocation = "Lahore Thokar Niaz Baig",
        seatsBooked = 1,
        seatNumbers = "Front Passenger",
        totalPrice = 1800.0,
        bookingCode = "PK-8842",
        status = BookingStatus.CONFIRMED,
        luggageCount = 1,
        bookedAtTimestamp = System.currentTimeMillis() - 3600000L,
        qrToken = "PKPASS-ISB-LHR-8842"
    )

    val initialPreferences = UserPreferenceEntity(
        userId = "current_user",
        userName = "Naeem Ullah",
        userPhone = "+92 300 1234567",
        isDriverMode = false,
        homeCity = "Islamabad",
        workCity = "Lahore",
        defaultCommuteDeparture = "07:30 AM",
        defaultCommuteReturn = "05:30 PM",
        favoriteCorridors = "Islamabad to Lahore (M-2), Rawalpindi to Islamabad",
        musicPreference = "Coke Studio / Lo-Fi / Podcasts",
        petAllergies = false,
        preferredLuggage = LuggageAllowance.MEDIUM,
        totalRidesAsPassenger = 24,
        totalRidesAsDriver = 9,
        totalCo2SavedKg = 412.0,
        totalMoneySavedUsd = 48500.0 // Representing PKR 48,500
    )

    val initialChatMessages = listOf(
        ChatMessageEntity(
            id = "msg_1",
            tripId = "trip_isb_lhr_1",
            senderId = "driver_hamza",
            senderName = "Hamza Tariq",
            isDriver = true,
            messageText = "Assalam-o-Alaikum Naeem! Looking forward to having you on the Islamabad to Lahore trip. I'll be in the white Corolla Altis Grande near Islamabad M-2 Toll Plaza.",
            timestamp = System.currentTimeMillis() - 1800000L,
            isRead = true
        ),
        ChatMessageEntity(
            id = "msg_2",
            tripId = "trip_isb_lhr_1",
            senderId = "current_user",
            senderName = "Naeem",
            isDriver = false,
            messageText = "Walaikum Assalam Hamza bhai! Perfect, I have my M-Tag receipt and will be waiting right at the passenger waiting shelter.",
            timestamp = System.currentTimeMillis() - 1200000L,
            isRead = true
        ),
        ChatMessageEntity(
            id = "msg_3",
            tripId = "trip_isb_lhr_1",
            senderId = "driver_hamza",
            senderName = "Hamza Tariq",
            isDriver = true,
            messageText = "Great! We will make a short 15-minute chai stop at Bhera Service Area midway. See you at 7:30 AM InshaAllah!",
            timestamp = System.currentTimeMillis() - 600000L,
            isRead = true
        )
    )
}

