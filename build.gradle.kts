plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "no.nav.pensjon"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/maskinporten-validation")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
    implementation("no.nav.pensjonsamhandling:maskinporten-validation-spring:2.0.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.wiremock", "wiremock-jetty12", "3.9.1")
    testImplementation("no.nav.pensjonsamhandling:maskinporten-validation-spring-test:2.0.3")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
