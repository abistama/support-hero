package com.abistama.supporthero.infrastructure.slackToJira

import com.abistama.supporthero.application.slackJira.SendCsatReminderUseCase
import mu.KLogging
import net.javacrumbs.shedlock.core.LockAssert
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled

open class SendCsatReminderScheduler(
    private val sendCsatReminderUseCase: SendCsatReminderUseCase,
) {
    companion object : KLogging()

    @Scheduled(fixedRate = 60000)
    @SchedulerLock(name = "sendCsatReminderScheduler")
    open fun getPendingTickets() {
        LockAssert.assertLocked()
        logger.info { "Executing scheduler ${this.javaClass.simpleName}" }
        sendCsatReminderUseCase.execute()
    }
}
