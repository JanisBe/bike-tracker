package com.biketracker.data.model

enum class ActivityType(val id: String, val displayName: String) {
    CYCLING("cycling", "Rower"),
    WALKING("walking", "Spacer");

    companion object {
        fun fromId(id: String?): ActivityType =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: CYCLING
    }
}
