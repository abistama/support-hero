package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.domain.slack.SlackUserId
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    val id: SlackUserId,
)
