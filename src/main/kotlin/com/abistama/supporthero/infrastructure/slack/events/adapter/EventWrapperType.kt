package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.fasterxml.jackson.annotation.JsonCreator

enum class EventWrapperType(
    private val type: String,
) {
    EVENT_CALLBACK("event_callback"),
    URL_VERIFICATION("url_verification"),
    BLOCK_ACTIONS("block_actions"),
    VIEW_SUBMISSION("view_submission"),
    SHORTCUT("shortcut"),
    UNKNOWN("unknown"),
    ;

    override fun toString(): String = type

    companion object {
        @JvmStatic
        @JsonCreator
        fun forValue(value: String): EventWrapperType = entries.firstOrNull { it.type == value } ?: UNKNOWN
    }
}
