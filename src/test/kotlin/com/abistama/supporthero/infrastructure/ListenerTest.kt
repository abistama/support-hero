package com.abistama.supporthero.infrastructure

import io.github.classgraph.ClassGraph
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener

class ListenerTest(
    @Autowired val applicationContext: ApplicationContext,
) : IntegrationSpec({

        class ProjectConfig : AbstractProjectConfig() {
            override fun extensions() = listOf(SpringExtension)
        }

        test("all listeners are registered") {
            val listeners = applicationContext.getBeansOfType(ApplicationListener::class.java)

            val expectedListeners =
                ClassGraph()
                    .enableClassInfo()
                    .acceptPackages("com.abistama.supporthero.*")
                    .scan()
                    .getClassesImplementing(ApplicationListener::class.java.name)
                    .loadClasses()
                    .map { it.name }
                    .toSet()

            val registeredListeners = listeners.values.map { it.javaClass.name }.toSet()

            registeredListeners shouldContainAll expectedListeners
        }
    })
