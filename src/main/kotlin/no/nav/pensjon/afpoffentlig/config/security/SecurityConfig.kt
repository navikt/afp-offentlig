package no.nav.pensjon.afpoffentlig.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain


@Configuration
@Profile("!disable-sec")
class SecurityConfig(@Value("\${maskinporten.scopes.afpprivat}")private val afpPrivatScope: String) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .csrf().disable()
            .authorizeHttpRequests()
            .requestMatchers("/actuator/health/liveness", "/actuator/health/readiness", "/actuator/prometheus").permitAll()
            .anyRequest().hasAuthority("SCOPE_$afpPrivatScope")
            .and()
            .oauth2ResourceServer()
            .jwt()

        return http.build()
    }
}