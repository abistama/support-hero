package com.abistama.supporthero.infrastructure.slack.oauth.adapter

import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

class URLReplacerWithEncoding {
    companion object {
        fun replace(
            template: String,
            toSubstitute: Map<String, String>,
        ): String {
            var result = template
            toSubstitute.forEach { (key, value) ->
                result = result.replace(key, URLEncoder.encode(value, UTF_8))
            }
            return result
        }
    }
}
