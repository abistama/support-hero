package com.abistama.supporthero.infrastructure.shedlock

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jooq.JooqLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "1m")
class ShedLockConfiguration {
    @Bean
    fun getLockProvider(dslContext: DSLContext): LockProvider {
        return JooqLockProvider(dslContext)
    }
}
