package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackEventUrlVerification(
    val token: String,
    val challenge: String,
) : SlackEventWrapper {
    override val type = EventWrapperType.URL_VERIFICATION
}
