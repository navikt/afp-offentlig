package no.nav.pensjon.afpoffentlig

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.pensjon.afpoffentlig.controllers.ApiController
import no.nav.pensjonsamhandling.maskinporten.validation.test.AutoConfigureMaskinportenValidator
import no.nav.pensjonsamhandling.maskinporten.validation.test.MaskinportenValidatorTokenGenerator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMaskinportenValidator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProxyMappingTest(
    @Autowired private val tokenGenerator: MaskinportenValidatorTokenGenerator,
    @Autowired private val mockMvc: MockMvc,
) {

    private val wireMockServer = WireMockServer(8080)
    private val responseBody = """{"value": false }"""

    @BeforeAll
    fun setup() {
        wireMockServer.start()
    }

    @AfterAll
    fun tearDown() {
        wireMockServer.stop()
    }

    companion object {
        @JvmStatic
        private fun get500ServerErrors() = listOf(
            HttpStatus.BAD_GATEWAY,
            HttpStatus.NOT_IMPLEMENTED,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.SERVICE_UNAVAILABLE
        )
    }

    @Test
    fun `test gets, request is forwarded with correct method, correlationId header and status - body and ok status is returned correct`() {
        val correlationId = UUID.randomUUID().toString()
        val fnr = "121212121212"
        val token = tokenGenerator.generateToken("nav:pensjon/v1/afpoffentlig", "12345678910")

        stubFor(
            get(urlEqualTo("/api/afpoffentlig/harAFPoffentlig?ytelseType=AFP"))
                .withHeader(ApiController.FNR, equalTo(fnr))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token.serialize()))
                .willReturn(aResponse().withBody(responseBody))
        )

        mockMvc.get("/harAFPoffentlig") {
            headers {
                setBearerAuth(token.serialize())
                set(ApiController.FNR, fnr)
                set(ApiController.CORRELATION_ID, correlationId)
            }
        }.andExpect {
            status { isOk()}
            content { string(responseBody) }
        }

    }

    @Test
    fun `test gets, request is forwarded with correct method, ytelseType, correlationId header and status - body and ok status is returned correct`() {
        val correlationId = UUID.randomUUID().toString()
        val fnr = "121212121212"
        val token = tokenGenerator.generateToken("nav:pensjon/v1/afpoffentlig", "12345678910")

        stubFor(
            get(urlEqualTo("/api/afpoffentlig/harAFPoffentlig?ytelseType=LIVSVARIG_AFP"))
                .withHeader(ApiController.FNR, equalTo(fnr))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token.serialize()))
                .willReturn(aResponse().withBody(responseBody))
        )

        mockMvc.get("/harAFPoffentlig?ytelseType=LIVSVARIG_AFP") {
            headers {
                setBearerAuth(token.serialize())
                set(ApiController.FNR, fnr)
                set(ApiController.CORRELATION_ID, correlationId)
            }
        }.andExpect {
            status { isOk()}
            content { string(responseBody) }
        }

    }

    @ParameterizedTest
    @MethodSource("get500ServerErrors")
    fun `test error from server returns as bad gateway`(error: HttpStatus) {
        val correlationId = UUID.randomUUID().toString()
        val fnr = "121212121212"
        val token = tokenGenerator.generateToken("nav:pensjon/v1/afpoffentlig", "12345678910")

        stubFor(
            get(urlEqualTo("/api/afpoffentlig/harAFPoffentlig?ytelseType=AFP"))
                .withHeader(ApiController.FNR, equalTo(fnr))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token.serialize()))
                .willReturn(aResponse().withStatus(error.value()))
        )

        mockMvc.get("/harAFPoffentlig") {
            headers {
                setBearerAuth(token.serialize())
                set(ApiController.FNR, fnr)
                set(ApiController.CORRELATION_ID, correlationId)
            }
        }.andExpect {
            status { isBadGateway() }
        }
    }
}
