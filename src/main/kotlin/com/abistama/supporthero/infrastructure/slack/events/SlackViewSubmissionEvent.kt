package com.abistama.supporthero.infrastructure.slack.events

import com.abistama.supporthero.infrastructure.slack.events.adapter.EventWrapperType
import com.abistama.supporthero.infrastructure.slack.events.adapter.SlackEventWrapper
import com.abistama.supporthero.infrastructure.slack.events.adapter.Team
import com.abistama.supporthero.infrastructure.slack.events.adapter.User
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(ignoreUnknown = true)
data class SlackViewSubmissionEvent(
    val view: View,
    val team: Team,
    val user: User,
) : SlackEventWrapper {
    override val type = EventWrapperType.VIEW_SUBMISSION

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class View(
        val state: State,
        @JsonProperty("external_id")
        val externalId: String,
        @JsonProperty("private_metadata")
        val privateMetadata: String,
    )

    data class State(
        val values: Map<String, Map<String, Input>>,
    ) {
        inline fun <reified T : Input> get(key: String): T = this.values["${key}_input_block"]?.get("${key}_input") as T
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = StaticSelect::class, name = "static_select"),
        JsonSubTypes.Type(value = UsersSelect::class, name = "users_select"),
        JsonSubTypes.Type(value = RichTextInput::class, name = "rich_text_input"),
        JsonSubTypes.Type(value = PlainTextInput::class, name = "plain_text_input"),
        JsonSubTypes.Type(value = RadioButtons::class, name = "radio_buttons"),
        JsonSubTypes.Type(value = Checkboxes::class, name = "checkboxes"),
    )
    interface Input {
        val type: String
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class StaticSelect(
        @JsonProperty("selected_option")
        val selectedOption: Option,
    ) : Input {
        override val type = "static_select"

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Option(
            val value: String,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RadioButtons(
        @JsonProperty("selected_option")
        val selectedOption: Option,
    ) : Input {
        override val type = "radio_buttons"

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Option(
            val value: String,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class UsersSelect(
        @JsonProperty("selected_user")
        val selectedUser: String,
    ) : Input {
        override val type = "users_select"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RichTextInput(
        @JsonProperty("rich_text_value")
        val value: RichTextValue,
    ) : Input {
        override val type = "rich_text_input"

        fun getText(): String? =
            this.value.elements
                .firstOrNull()
                ?.elements
                ?.first()
                ?.text
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RichTextValue(
        val elements: List<RichTextElement>,
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class RichTextElement(
            val elements: List<TextElement>,
        ) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            data class TextElement(
                val text: String,
            )
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PlainTextInput(
        val value: String?,
    ) : Input {
        override val type = "plain_text_input"
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Checkboxes(
        @JsonProperty("selected_options")
        val selectedOptions: List<Option>,
    ) : Input {
        override val type = "checkboxes"

        @JsonIgnoreProperties(ignoreUnknown = true)
        data class Option(
            val value: String,
        )
    }
}
