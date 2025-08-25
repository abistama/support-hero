package com.abistama.supporthero.infrastructure.slack.oauth.adapter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class URLReplacerWithEncodingTest :
    FunSpec({

        test("replaceWithEncoding") {
            val template = "https://example.com?param1=#{param1}&param2=#{param2}"
            val toSubstitute =
                mapOf(
                    "#{param1}" to "http://example.com/1",
                    "#{param2}" to "value2",
                )
            val expected = "https://example.com?param1=http%3A%2F%2Fexample.com%2F1&param2=value2"
            URLReplacerWithEncoding.replace(template, toSubstitute) shouldBe expected
        }
    })
