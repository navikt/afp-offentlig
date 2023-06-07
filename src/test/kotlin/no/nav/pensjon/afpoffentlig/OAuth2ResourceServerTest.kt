package no.nav.pensjon.afpoffentlig

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.TestSocketUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ExtendWith(SpringExtension::class)
class OAuth2ResourceServerTest(@Autowired private val mockMvc: MockMvc) {

    companion object {
        val authenticationServerPort = TestSocketUtils.findAvailableTcpPort()
        val issuer = "http://localhost:$authenticationServerPort/"
        val trustedAuthenticationServer =
            OAuthAuthenticationServerMock(issuer = issuer, port = authenticationServerPort, startHttpServer = true)
        val notTrustedAuthenticationServer =
            OAuthAuthenticationServerMock(issuer = issuer, startHttpServer = false)

        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("MASKINPORTEN_ISSUER") { issuer }
            registry.add("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT") { "test" }
            registry.add("AZURE_OPENID_CONFIG_JWKS_URI") { "test" }
            registry.add("TP_FSS_URL") { "test" }
            registry.add("TP_FSS_SCOPE") { "test" }
            registry.add("AZURE_APP_CLIENT_SECRET") { "test" }
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            trustedAuthenticationServer.stopHttpServer()
        }
    }

    @Test
    fun `correctly signed jwt with correct scope has access`() {
        this.mockMvc
            .perform(
                MockMvcRequestBuilders.get("/ping").header(
                    "Authorization", "Bearer ${
                        trustedAuthenticationServer.generateClientJWT(
                            scopes = listOf("nav:pensjon/afpprivat"),
                            issuer = issuer
                        )
                    }"
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `correctly signed jwt with wrong scope is forbidden`() {
        this.mockMvc
            .perform(
                MockMvcRequestBuilders.get("/ping").header(
                    "Authorization", "Bearer ${
                        trustedAuthenticationServer.generateClientJWT(
                            scopes = listOf("nav:pensjon/feilscope"), issuer = issuer
                        )
                    }"
                )
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `jwt with correct issuer and scope, but signed with untrusted private key, is unauthorized`() {
        this.mockMvc
            .perform(
                MockMvcRequestBuilders.get("/ping").header(
                    "Authorization", "Bearer ${
                        notTrustedAuthenticationServer.generateClientJWT(
                            scopes = listOf("nav:pensjon/afpprivat"), issuer = issuer
                        )
                    }"
                )
            )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `correctly signed jwt that has expired is unauthorized`() {
        this.mockMvc
            .perform(
                MockMvcRequestBuilders.get("/ping").header(
                    "Authorization", "Bearer ${
                        trustedAuthenticationServer.generateClientJWT(
                            scopes = listOf("nav:pensjon/afpprivat"), issuer = issuer, expiration = Date(0)
                        )
                    }"
                )
            )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `correctly signed jwt with not before time in the future is unauthorized`() {
        this.mockMvc
            .perform(
                MockMvcRequestBuilders.get("/ping").header(
                    "Authorization", "Bearer ${
                        trustedAuthenticationServer.generateClientJWT(
                            scopes = listOf("nav:pensjon/afpprivat"), issuer = issuer,
                            notBeforeTime = inAnHour()
                        )
                    }"
                )
            )
            .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    private fun inAnHour() = Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant())
}