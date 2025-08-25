package com.abistama.supporthero.infrastructure.slack.events.adapter

import com.abistama.supporthero.infrastructure.slack.events.SlackViewSubmissionEvent
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SlackEventUrlVerification::class, name = "url_verification"),
    JsonSubTypes.Type(value = SlackEventCallBack::class, name = "event_callback"),
    JsonSubTypes.Type(value = SlackBlockActionsEvent::class, name = "block_actions"),
    JsonSubTypes.Type(value = SlackViewSubmissionEvent::class, name = "view_submission"),
    JsonSubTypes.Type(value = SlackShortcutEvent::class, name = "shortcut"),
)
interface SlackEventWrapper {
    val type: EventWrapperType
}
