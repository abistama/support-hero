package com.abistama.supporthero.domain.slack.events

import com.fasterxml.jackson.annotation.JsonCreator

enum class EventType(private val type: String) {
    REACTION_ADDED("reaction_added"),
    UNKNOWN("unknown"),
    ;

    override fun toString(): String {
        return type
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): EventType {
            return entries.firstOrNull { it.type == value } ?: UNKNOWN
        }
    }
}
