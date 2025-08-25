package com.abistama.supporthero.application.summarizer

import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V

interface Summarizer {
    @SystemMessage(
        """
        Provide a short summary of the given text? The summary should be short, like a title. 
        It MUST not go over 200 characters. NOTHING ELSE. Examples of good titles:
        - "Question about Kubernetes"
        - "Problem accessing our internal VPN"
        - "Question about GDPR and privacy concerns"
        - "Doubt about a company policy"
        - "Issue with the new software update"
        - "Question about the new project deadline"
        - "Problem with the new employee onboarding process"
        - "Doubt about the new company policy"
        """,
    )
    fun summarizeTitle(userMessage: String): String

    @SystemMessage(
        """
        Provide a summary of the content. The summary should cover all the key points 
        and main ideas presented in the original text, while also condensing the information into a concise 
        and easy-to-understand format. Be concise. AVOID any text that's not the
        summary itself. DO NOT start with any introduction like "here is a summary". Put the summary directly
        """,
    )
    @UserMessage("{{text}}")
    fun summarizeText(
        @V("text") text: String,
    ): String
}
