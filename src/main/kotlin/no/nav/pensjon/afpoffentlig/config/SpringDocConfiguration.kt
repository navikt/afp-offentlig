package no.nav.pensjon.afpoffentlig.config

import org.springdoc.core.SpringDocConfigProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class SpringDocConfiguration {
    @Bean
    fun springDocConfiguration(): SpringDocConfiguration? {
        return SpringDocConfiguration()
    }

    @Bean
    fun springDocConfigProperties(): SpringDocConfigProperties? {
        return SpringDocConfigProperties()
    }

}