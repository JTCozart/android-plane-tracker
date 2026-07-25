package com.jtcozart.planetracker.model

/**
 * Aircraft classification — a direct port of the firmware's classification rules
 * (see Aircraft.cpp). Ordering matches the firmware enum so detection counts line up.
 */
enum class AircraftClass(val displayName: String, val tag: String) {
    MILITARY("MILITARY", "MIL"),
    MEDEVAC("MEDEVAC", "MEDVAC"),
    COMMERCIAL("COMMERCIAL", "COMM"),
    PRIVATE("PRIVATE", "PRIV");

    companion object {
        // Callsign prefixes — all uppercase, matched with startsWith.
        private val MEDEVAC_CALLSIGN_PREFIXES = listOf(
            "LIFEGRD",  // FAA LIFEGUARD (regulated designation)
            "MEDVAC",   // generic medevac callsign
            "MEDIC",    // very common EMS helicopter prefix
            "AIRLIFE",  // Air Life
            "AIRMED",   // AirMed
            "REACH",    // REACH Air Medical
            "LIFEFLT",  // LifeFlight
            "MERCY",    // Mercy Air / Mercy Flight
            "CAREFLT",  // CareFlight
            "STAT",     // STAT MedEvac
            "ANGEL",    // Angel MedFlight
            "RESCUE",   // Air rescue services
        )

        // Operator name substrings — all uppercase, matched with contains.
        // Keep entries specific enough to avoid false positives on common words.
        private val MEDEVAC_OPERATOR_SUBSTRINGS = listOf(
            "LIFEFLIGHT",
            "LIFE FLIGHT",
            "LIFEGUARD",
            "LIFE GUARD",
            "MEDEVAC",
            "MEDIVAC",
            "MEDVAC",
            "AIR AMBULANCE",
            "AEROMED",
            "AIR MED",
            "AIRMED",
            "AIR METHODS",
            "AIR LIFE",
            "AIR EVAC",
            "PHI AIR MEDICAL",
            "REACH AIR",
            "OMNIFLIGHT",
            "GUARDIAN FLIGHT",
            "GUARDIAN FL",
            "METRO AVIATION",  // major air medical operator
            "AIR METHODS",
            "SURVIVAL FLIGHT",
            "CAREFLIGHT",
            "CARE FLIGHT",
            "FLIGHT FOR LIFE",
            "MERCY FLIGHT",
            "MERCY AIR",
            "ANGEL MEDFLIGHT",
            "ANGEL MED",
            "STAT MEDEVAC",
            "EMERGENCY MEDICAL SERV",
            "AIR RESCUE",
            "HELICOPTER EMS",
            "HEMS",
        )

        /** Returns the AircraftClass for the given aircraft attributes. */
        fun classify(
            callsign: String,
            owner: String,
            milFlag: Boolean,
            category: String
        ): AircraftClass {
            // Military: API flags only — no callsign guessing to avoid false positives.
            if (milFlag) return MILITARY

            val upperCallsign = callsign.uppercase()
            val upperOwner = owner.uppercase()

            // Medevac: regulated callsign prefix or known operator substring.
            if (MEDEVAC_CALLSIGN_PREFIXES.any { upperCallsign.startsWith(it) }) return MEDEVAC
            if (MEDEVAC_OPERATOR_SUBSTRINGS.any { upperOwner.contains(it) }) return MEDEVAC

            // Commercial: API category A3=large, A4=high-vortex (B757), A5=heavy.
            if (category == "A3" || category == "A4" || category == "A5") return COMMERCIAL

            // Commercial: standard ICAO airline designator pattern (e.g. DAL123, UAL456).
            if (upperCallsign.length in 5..8 &&
                upperCallsign[0].isLetter() &&
                upperCallsign[1].isLetter() &&
                upperCallsign[2].isLetter() &&
                upperCallsign[3].isDigit()
            ) return COMMERCIAL

            return PRIVATE
        }
    }
}
