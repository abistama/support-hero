package com.abistama.supporthero.infrastructure.slack.events.adapter

import mu.KLogging
import java.net.URLDecoder
import kotlin.text.Charsets.UTF_8

class FormUrlEncodedAdapter : KLogging() {
    fun from(requestBody: String): String? {
        val parameters =
            requestBody.split("&").associate {
                val (key, value) = it.split("=")
                key to URLDecoder.decode(value, UTF_8)
            }
        return parameters["payload"]
    }
}
