package com.abistama.supporthero.domain.slack

import org.bouncycastle.util.encoders.Hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class SlackSignatureValidation(val requestBody: String, val requestTimestamp: String, val slackSignature: String) {
    fun isValid(signingSecret: String): Boolean {
        val baseString = "v0:$requestTimestamp:$requestBody"

        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(signingSecret.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)

        val finalHex = "v0=${Hex.toHexString(sha256Hmac.doFinal(baseString.toByteArray()))}"
        return finalHex.compareTo(slackSignature) == 0
    }
}
