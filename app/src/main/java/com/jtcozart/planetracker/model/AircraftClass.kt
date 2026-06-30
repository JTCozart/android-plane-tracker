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
        // FAA LIFEGUARD prefix is a regulated designation — safe to match on callsign.
        // All other classification comes from the API's own fields.
        private val MEDVAC_CS = listOf(
            "LIFEGRD", "MEDVAC", "AIRLIFE", "REACH", "LIFEFLT"
        )
        private val MEDVAC_OP = listOf(
            "AIR LIFE", "AIR METHODS", "PHI AIR", "METRO AVIA",
            "OMNIFLIGHT", "GUARDIAN FL", "LIFE FLIGHT", "LIFEFLIGHT",
            "AIR EVAC", "REACH AIR", "EMS"
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

            // Medevac: FAA LIFEGUARD callsign prefix or known operator.
            if (MEDVAC_CS.any { upperCallsign.startsWith(it) }) return MEDEVAC
            if (MEDVAC_OP.any { upperOwner.contains(it) }) return MEDEVAC

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
