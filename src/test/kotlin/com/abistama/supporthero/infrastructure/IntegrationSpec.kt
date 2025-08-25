package com.abistama.supporthero.infrastructure

import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.spring.SpringExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
abstract class IntegrationSpec(
    body: FunSpec.() -> Unit = {},
) : FunSpec(body) {
    override fun extensions(): List<Extension> = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    override suspend fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)
    }

    companion object {
        @Container
        @JvmField
        var container = PostgreSQLContainer(DockerImageName.parse("postgres:16"))

        init {
            container.start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun setDbProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { container.jdbcUrl }
        }
    }
}
