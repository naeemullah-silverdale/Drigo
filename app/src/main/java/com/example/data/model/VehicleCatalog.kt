package com.example.data.model

/**
 * Comprehensive Global Vehicle Companies and Models Catalog.
 * Provides curated lists of car manufacturers worldwide along with their popular models,
 * with search and custom brand/model fallback support.
 */
object VehicleCatalog {

    val manufacturers: List<String> = listOf(
        "Toyota",
        "Honda",
        "Suzuki",
        "Nissan",
        "Hyundai",
        "KIA",
        "Changan",
        "MG (Morris Garages)",
        "Daihatsu",
        "BMW",
        "Mercedes-Benz",
        "Audi",
        "Ford",
        "Chevrolet",
        "BYD",
        "Proton",
        "HAVAL / GWM",
        "FAW",
        "Peugeot",
        "Lexus",
        "Mitsubishi",
        "Volkswagen",
        "Mazda",
        "Subaru",
        "Tesla",
        "Volvo",
        "Chery",
        "BAIC",
        "DFSK",
        "United Motors",
        "Prince / Regal",
        "JAC / Master",
        "Renault",
        "Fiat",
        "Skoda",
        "SEAT / Cupra",
        "Jeep",
        "Land Rover / Range Rover",
        "Jaguar",
        "Porsche",
        "Alfa Romeo",
        "Genesis",
        "Geely",
        "Saipa",
        "IKCO (Iran Khodro)",
        "Tata Motors",
        "Mahindra",
        "Maruti Suzuki",
        "Infiniti",
        "Acura",
        "Buick",
        "Cadillac",
        "GMC",
        "Dodge / RAM / Chrysler",
        "Isuzu",
        "SsangYong / KG Mobility",
        "Dongfeng",
        "GAC Motor",
        "SAIC / Roewe",
        "NIO",
        "XPeng",
        "Li Auto",
        "Zeekr",
        "Lucid",
        "Rivian",
        "Aston Martin",
        "Bentley",
        "Ferrari",
        "Lamborghini",
        "Maserati",
        "McLaren",
        "Rolls-Royce",
        "Bugatti",
        "Lotus",
        "Smart",
        "Perodua",
        "Holden",
        "Scion",
        "Daewoo",
        "Zotye",
        "Lifan",
        "JMC",
        "VinFast",
        "Alpine",
        "Abarth",
        "Dacia",
        "Lada",
        "Other / Custom Manufacturer"
    )

    private val modelsMap: Map<String, List<String>> = mapOf(
        "Toyota" to listOf(
            "Corolla", "Yaris", "Camry", "Prius", "Fortuner", "Hilux / Revo", "Passo", "Vitz", "Aqua",
            "Prado", "Land Cruiser", "RAV4", "Corolla Cross", "C-HR", "Harrier", "Belta", "Mark X",
            "Crown", "Rush", "Coaster", "HiAce", "Alphard", "Vellfire", "Avanza", "Raize", "Urban Cruiser",
            "Supra", "GR86", "Sienna", "Highlander", "Tacoma", "Tundra", "4Runner", "Avalon", "Mirai", "Celica"
        ),
        "Honda" to listOf(
            "Civic", "City", "BR-V", "HR-V", "Accord", "Vezel", "CR-V", "Fit", "N-Wgn", "Freed",
            "Insight", "Odyssey", "Jazz", "Shuttle", "Pilot", "CR-Z", "Civic Type R", "Brio", "Grace",
            "Stepwgn", "Passport", "Ridgeline", "Prelude", "S2000"
        ),
        "Suzuki" to listOf(
            "Alto", "Cultus", "Wagon R", "Swift", "Bolan", "Mehran", "Ravi", "Every", "Jimny",
            "Khyber", "Baleno", "Liana", "Vitara", "Grand Vitara", "Ciaz", "Hustler", "Alto Lapin",
            "Ignis", "APV", "Kizashi", "Potohar", "S-Cross", "Ertiga", "Spacia", "Solio", "Across"
        ),
        "Nissan" to listOf(
            "Dayz", "Note", "Sunny", "Patrol", "March / Micra", "Juke", "Clipper", "Rogue", "Altima",
            "Sentra", "Versa", "Kicks", "Pathfinder", "Murano", "Leaf", "GT-R", "Navara", "X-Trail",
            "Armada", "370Z", "350Z", "Z", "Titan", "Frontier", "Elgrand", "Serena", "Roox"
        ),
        "Hyundai" to listOf(
            "Elantra", "Sonata", "Tucson", "Porter", "Grand i10", "Santa Fe", "Accent", "Venue",
            "Creta", "Kona", "Ioniq 5", "Ioniq 6", "Palisade", "Staria", "Terracan", "Genesis",
            "Exter", "Bayon", "Veloster", "Azera", "Shehzore"
        ),
        "KIA" to listOf(
            "Sportage", "Picanto", "Stonic", "Sorento", "Carnival", "Grand Starex", "Rio", "Cerato / Forte",
            "Optima / K5", "Soul", "EV6", "EV9", "Telluride", "Seltos", "Carens", "Stinger", "Cadenza", "Mohave"
        ),
        "Changan" to listOf(
            "Alsvin", "Karvaan", "Oshan X7", "M9", "Uni-K", "Uni-T", "Uni-V", "CS35 Plus", "CS75 Plus",
            "CS55 Plus", "Hunter", "Benni", "Eado", "Deepal S07", "Deepal L07"
        ),
        "MG (Morris Garages)" to listOf(
            "HS", "ZS", "ZS EV", "GT", "MG3", "MG5", "MG6", "Marvel R", "Extender", "Gloster",
            "Hector", "Comet EV", "Cyberster"
        ),
        "Daihatsu" to listOf(
            "Mira", "Move", "Hijet", "Tanto", "Terios", "Cast", "Cuore", "Taft", "Rocky", "Sirion",
            "Charade", "Boon", "Wake", "Thor", "Atrai", "Sonica", "Copen", "Mebius"
        ),
        "BMW" to listOf(
            "3 Series", "5 Series", "7 Series", "X1", "X3", "X5", "X6", "X7", "i4", "iX", "1 Series",
            "2 Series", "4 Series", "6 Series", "8 Series", "M2", "M3", "M4", "M5", "Z4", "i3", "i7", "XM"
        ),
        "Mercedes-Benz" to listOf(
            "C-Class", "E-Class", "S-Class", "CLA", "GLA", "GLC", "GLE", "GLS", "A-Class", "B-Class",
            "G-Class", "EQE", "EQS", "EQA", "EQB", "Vito", "Sprinter", "CLS", "SL", "AMG GT"
        ),
        "Audi" to listOf(
            "A3", "A4", "A6", "A8", "Q3", "Q5", "Q7", "Q8", "e-tron", "e-tron GT", "TT", "R8",
            "A5", "A7", "Q2", "RS3", "RS5", "RS6", "RS Q8"
        ),
        "Ford" to listOf(
            "F-150", "Mustang", "Ranger", "Explorer", "Escape", "Focus", "Fiesta", "EcoSport",
            "Everest", "Bronco", "Bronco Sport", "Transit", "Edge", "Mustang Mach-E", "Expedition", "Maverick"
        ),
        "Chevrolet" to listOf(
            "Aveo", "Cruze", "Malibu", "Captiva", "Tahoe", "Suburban", "Colorado", "Corvette",
            "Camaro", "Spark", "Trailblazer", "Bolt EV", "Trax", "Silverado", "Equinox", "Blazer"
        ),
        "BYD" to listOf(
            "Atto 3", "Dolphin", "Seal", "Han", "Tang", "Song Plus", "Qin Plus", "Seagull",
            "Yuan Plus", "e6", "Yangwang U8", "Denza D9"
        ),
        "Proton" to listOf(
            "Saga", "X70", "X50", "X90", "Persona", "Exora", "Iriz", "S70", "Wira", "Gen-2"
        ),
        "HAVAL / GWM" to listOf(
            "H6", "H6 HEV", "Jolion", "H6 GT", "H9", "Dargo", "Tank 300", "Tank 500", "Poer Cannon", "Ora Good Cat"
        ),
        "FAW" to listOf(
            "V2", "X-PV", "Carrier", "Besturn B50", "Besturn X40", "Sirion", "Hongqi E-HS9"
        ),
        "Peugeot" to listOf(
            "208", "2008", "3008", "5008", "308", "508", "Partner", "Rifter", "Landtrek", "e-208"
        ),
        "Lexus" to listOf(
            "RX", "ES", "NX", "LX", "GX", "IS", "LS", "UX", "LC", "TX", "RZ", "RC", "CT"
        ),
        "Mitsubishi" to listOf(
            "Lancer", "Pajero", "Pajero Sport", "Outlander", "Mirage", "Ek Wagon", "Ek Space",
            "Delica", "L200 / Triton", "Eclipse Cross", "Attrage", "Xpander", "Galant", "ASX"
        ),
        "Volkswagen" to listOf(
            "Golf", "Polo", "Passat", "Tiguan", "Touareg", "Jetta", "ID.4", "ID.3", "ID.7", "Amarok",
            "Transporter", "Beetle", "Arteon", "T-Roc", "Taigo", "Virtus"
        ),
        "Mazda" to listOf(
            "Mazda2", "Mazda3", "Mazda6", "CX-3", "CX-5", "CX-30", "CX-50", "CX-60", "CX-90",
            "MX-5 Miata", "RX-8", "Demio", "Axela", "Atenza", "BT-50"
        ),
        "Subaru" to listOf(
            "Impreza", "Legacy", "Outback", "Forester", "XV / Crosstrek", "WRX", "BRZ", "Solterra", "Ascent"
        ),
        "Tesla" to listOf(
            "Model 3", "Model Y", "Model S", "Model X", "Cybertruck", "Roadster"
        ),
        "Volvo" to listOf(
            "XC40", "XC60", "XC90", "S60", "S90", "V60", "V90", "EX30", "EX90", "C40 Recharge"
        ),
        "Chery" to listOf(
            "Tiggo 4 Pro", "Tiggo 8 Pro", "Arrizo 5", "Tiggo 7 Pro", "Tiggo 2 Pro", "Omoda 5", "Jaecoo 7"
        ),
        "BAIC" to listOf(
            "BJ40 Plus", "D20", "X25", "X35", "EU5", "EU7", "BJ80"
        ),
        "DFSK" to listOf(
            "Glory 580", "Glory 580 Pro", "Prince Pearl", "Glory 500", "C37", "K01", "Seres 3"
        ),
        "United Motors" to listOf(
            "Bravo", "Alpha", "Usmaan 150"
        ),
        "Prince / Regal" to listOf(
            "Pearl", "K07", "K01"
        ),
        "JAC / Master" to listOf(
            "X200", "T8", "T6", "JS4", "iEV7S"
        ),
        "Renault" to listOf(
            "Duster", "Kwid", "Megane", "Clio", "Captur", "Koleos", "Zoe", "Triber", "Kiger", "Arkana"
        ),
        "Fiat" to listOf(
            "500", "Panda", "Tipo", "Doblo", "Ducato", "500X", "500e", "Fiorino"
        ),
        "Skoda" to listOf(
            "Octavia", "Superb", "Kodiaq", "Karoq", "Kamiq", "Fabia", "Slavia", "Kushaq", "Enyaq"
        ),
        "SEAT / Cupra" to listOf(
            "Ibiza", "Leon", "Ateca", "Arona", "Tarraco", "Formentor", "Born"
        ),
        "Jeep" to listOf(
            "Wrangler", "Grand Cherokee", "Compass", "Renegade", "Gladiator", "Avenger", "Cherokee"
        ),
        "Land Rover / Range Rover" to listOf(
            "Defender", "Discovery", "Discovery Sport", "Range Rover", "Range Rover Sport",
            "Range Rover Evoque", "Range Rover Velar"
        ),
        "Jaguar" to listOf(
            "F-Pace", "E-Pace", "XE", "XF", "I-Pace", "F-Type", "XJ"
        ),
        "Porsche" to listOf(
            "911", "Cayenne", "Macan", "Panamera", "Taycan", "718 Boxster", "718 Cayman"
        ),
        "Alfa Romeo" to listOf(
            "Giulia", "Stelvio", "Tonale", "4C", "Giulietta"
        ),
        "Genesis" to listOf(
            "G70", "G80", "G90", "GV70", "GV80", "GV60"
        ),
        "Geely" to listOf(
            "Coolray", "Monjaro", "Geometry C", "Emgrand", "Tugella", "Okavango", "Azkarra"
        ),
        "Saipa" to listOf(
            "Pride", "Tiba", "Saina", "Quik", "Shahin", "Aria"
        ),
        "IKCO (Iran Khodro)" to listOf(
            "Samand", "Dena", "Tara", "Runna", "Arisun", "Paykan"
        ),
        "Tata Motors" to listOf(
            "Nexon", "Punch", "Harrier", "Safari", "Tiago", "Tigor", "Altroz", "Curvv", "Ace", "Nano"
        ),
        "Mahindra" to listOf(
            "Thar", "XUV700", "Scorpio-N", "Bolero", "XUV300", "Major", "XUV400 EV", "Marazzo"
        ),
        "Maruti Suzuki" to listOf(
            "Swift", "Baleno", "Brezza", "Dzire", "Wagon R", "Ertiga", "Fronx", "Grand Vitara",
            "Alto K10", "Jimny", "XL6", "Invicto", "Eeco", "Celerio", "S-Presso"
        ),
        "Infiniti" to listOf(
            "Q50", "QX60", "QX80", "QX50", "Q60", "QX55"
        ),
        "Acura" to listOf(
            "MDX", "RDX", "TLX", "Integra", "ZDX", "NSX"
        ),
        "Buick" to listOf(
            "Enclave", "Encore", "Envision", "Envista", "Regal"
        ),
        "Cadillac" to listOf(
            "Escalade", "CT4", "CT5", "Lyriq", "XT4", "XT5", "XT6", "Celestiq"
        ),
        "GMC" to listOf(
            "Sierra", "Yukon", "Acadia", "Terrain", "Hummer EV", "Canyon"
        ),
        "Dodge / RAM / Chrysler" to listOf(
            "Charger", "Challenger", "Durango", "Hornet", "RAM 1500", "RAM 2500", "300", "Pacifica"
        ),
        "Isuzu" to listOf(
            "D-Max", "MU-X", "N-Series", "F-Series"
        ),
        "SsangYong / KG Mobility" to listOf(
            "Rexton", "Korando", "Tivoli", "Torres", "Musso"
        )
    )

    /**
     * Gets models for a given manufacturer. If not found, returns a standard fallback list.
     */
    fun getModelsForManufacturer(manufacturer: String): List<String> {
        val cleanKey = manufacturers.firstOrNull { it.equals(manufacturer, ignoreCase = true) } ?: manufacturer
        val list = modelsMap[cleanKey] ?: listOf("Sedan", "Hatchback", "SUV / Crossover", "Pickup Truck", "Van / MPV", "Coupe / Convertible", "Commercial Vehicle")
        return list + "Other / Custom Model"
    }

    /**
     * Filters manufacturers based on search query.
     */
    fun searchManufacturers(query: String): List<String> {
        if (query.isBlank()) return manufacturers
        return manufacturers.filter { it.contains(query, ignoreCase = true) }
    }

    /**
     * Filters models based on search query for a specific manufacturer.
     */
    fun searchModels(manufacturer: String, query: String): List<String> {
        val allModels = getModelsForManufacturer(manufacturer)
        if (query.isBlank()) return allModels
        return allModels.filter { it.contains(query, ignoreCase = true) }
    }
}
