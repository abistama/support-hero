package com.abistama.supporthero.infrastructure.jira.events

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import java.nio.file.Files
import java.nio.file.Paths

class JiraIssueResponseTest :
    FunSpec({

        test("should deserialize a real Jira issue response") {
            val objectMapper = jacksonObjectMapper()
            val uri =
                this::class.java.classLoader
                    .getResource("jira-issue-response-real.json")
                    ?.toURI()
            val json = uri?.let { Paths.get(it) }?.let { Files.readAllLines(it) }?.joinToString("\n") ?: ""

            objectMapper.readValue(json, JiraIssueResponse::class.java) shouldNotBe null
        }
    })
