package com.abistama.supporthero.infrastructure.webpages

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class WebPageController {
    @GetMapping("/")
    fun index(): String = "index"

    @GetMapping("/slack-success")
    fun slackSuccess(): String = "slack-success"

    @GetMapping("/slack-error")
    fun slackError(): String = "slack-error"

    @GetMapping("/jira-success")
    fun jiraSuccess(): String = "jira-success"

    @GetMapping("/jira-error")
    fun jiraError(): String = "jira-error"
}
