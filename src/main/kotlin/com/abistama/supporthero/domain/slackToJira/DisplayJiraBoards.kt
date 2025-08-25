package com.abistama.supporthero.domain.slackToJira

import com.abistama.supporthero.domain.slack.JiraBoardConfiguration
import java.util.*

class DisplayJiraBoards(
    val slackToJiraTenantId: UUID,
    val jiraBoardsConfiguration: List<JiraBoardsStatistics>,
)

data class JiraBoardsStatistics(
    val configurationId: UUID,
    val configuration: JiraBoardConfiguration,
    val avgCsat: Float,
)
