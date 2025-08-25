package com.abistama.supporthero.infrastructure.configuration

import org.jooq.DSLContext
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class JooqConfiguration {
    @Bean
    fun dslContext(dataSource: DataSource): DSLContext {
        return DSL.using(dataSource, POSTGRES)
    }
}
