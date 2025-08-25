package com.abistama.supporthero.infrastructure.jira

import feign.codec.ErrorDecoder
import org.apache.commons.io.IOUtils
import kotlin.text.Charsets.UTF_8

class JiraErrorDecoder : ErrorDecoder {
    override fun decode(
        methodKey: String?,
        response: feign.Response?,
    ): Exception {
        if (response != null) {
            val msg = response?.body()?.asInputStream()?.let { IOUtils.toString(it, UTF_8) } ?: "Empty body"
            if (response.status() in 400..499) {
                if (response.status() == 401) {
                    return JiraUnauthorizedException(msg)
                }
                return JiraBadRequestException(msg)
            } else if (response.status() in 500..599) {
                return JiraInternalErrorException(msg)
            }
        }
        return ErrorDecoder.Default().decode(methodKey, response)
    }
}
