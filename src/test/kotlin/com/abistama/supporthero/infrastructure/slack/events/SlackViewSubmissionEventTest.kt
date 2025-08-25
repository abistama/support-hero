package com.abistama.supporthero.infrastructure.slack.events

import com.abistama.supporthero.infrastructure.jira.Fixtures
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SlackViewSubmissionEventTest :
    FunSpec({

        test("should deserialize SlackViewSubmissionEvent") {
            // Given
            val fixtures = Fixtures()
            val submissionEvent = fixtures.getSlackViewSubmissionEvent()

            // Then
            (
                submissionEvent.view.state.values["project_input_block"]
                    ?.get("project_input") as SlackViewSubmissionEvent.StaticSelect
            ).selectedOption.value shouldBe "TO"

            (
                submissionEvent.view.state.values["reaction_input_block"]
                    ?.get("reaction_input") as SlackViewSubmissionEvent.StaticSelect
            ).selectedOption.value shouldBe "ticket"

            (
                submissionEvent.view.state.values["user_or_group_id_input_block"]
                    ?.get("user_or_group_id_input") as SlackViewSubmissionEvent.UsersSelect
            ).selectedUser shouldBe "U05084E8G2Y"

            (
                submissionEvent.view.state.values["feedback_message_input_block"]
                    ?.get("feedback_message_input") as SlackViewSubmissionEvent.PlainTextInput
            ).value shouldBe "Hi #reporter! We'll take care of it"

            (
                submissionEvent.view.state.values["send_csat_input_block"]
                    ?.get("send_csat_input") as SlackViewSubmissionEvent.RadioButtons
            ).selectedOption.value shouldBe "Yes"

            (
                submissionEvent.view.state.values["labels_input_block"]
                    ?.get("labels_input") as SlackViewSubmissionEvent.PlainTextInput
            ).value shouldBe "bot"
        }
    })
