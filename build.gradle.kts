import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.jooq.jooq-codegen-gradle") version "3.19.8"
    id("org.liquibase.gradle") version "2.2.2"
    jacoco
}

group = "com.abistama"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.arrow-kt:arrow-core:1.2.4")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("ch.qos.logback:logback-classic")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.jooq:jooq:3.19.8")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.github.openfeign:feign-jackson")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.liquibase:liquibase-core")
    implementation("com.slack.api:slack-api-client:1.39.3")
    implementation("com.slack.api:slack-api-model-kotlin-extension:1.39.3")
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.13.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jooq:5.13.0")
    implementation("dev.langchain4j:langchain4j:0.31.0")
    implementation("dev.langchain4j:langchain4j-anthropic:0.31.0")
    implementation("dev.langchain4j:langchain4j-spring-boot-starter:0.31.0")
    implementation("dev.langchain4j:langchain4j-anthropic-spring-boot-starter:0.31.0")

    runtimeOnly("org.postgresql:postgresql:42.7.3")
    jooqCodegen("org.postgresql:postgresql:42.7.3")
    liquibaseRuntime("org.liquibase:liquibase-core")
    liquibaseRuntime("org.postgresql:postgresql:42.7.3")
    liquibaseRuntime("info.picocli:picocli:4.7.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation("io.kotest:kotest-framework-datatest:5.8.1")
    implementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.1")
    }
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/supporthero"
            user = "postgres"
            password = "password"
        }
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                inputSchema = "public"
            }
            target {
                packageName = "com.abistama.supporthero.infrastructure.generated"
                directory = "src/main/kotlin"
            }
        }
    }
}

liquibase {
    activities.register("main") {
        this.arguments =
            mapOf(
                "changelogFile" to "src/main/resources/db/master.xml",
                "url" to "jdbc:postgresql://localhost:5432/supporthero",
                "username" to "postgres",
                "password" to "password",
            )
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<JacocoReport> {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) {
                    exclude("**/generated/**")
                }
            },
        ),
    )
}
