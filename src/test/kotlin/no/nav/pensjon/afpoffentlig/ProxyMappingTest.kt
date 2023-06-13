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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMaskinportenValidator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProxyMappingTest(
    @Autowired private val tokenGenerator: MaskinportenValidatorTokenGenerator,
    @Autowired private val mockMvc: MockMvc
) {

    private val wireMockServer = WireMockServer(8080)
    private val responseBody = """{"json":"test"}"""

    @BeforeAll
    fun setup() {
        wireMockServer.start()
    }

    @AfterAll
    fun tearDown() {
        wireMockServer.stop()
    }

    @Test
    fun `test gets, request is forwarded with correct method, correlationId header and status - body and ok status is returned correct`() {
        val correlationId = UUID.randomUUID().toString()
        val fnr = "121212121212"
        val authorization = "bearer ${tokenGenerator.generateToken("nav:pensjon/v1/tpregisteret", "12345678910")}"

        stubFor(
            get(urlEqualTo("/api/tjenestepensjon/harAFPoffentlig"))
                .withHeader(ApiController.CORRELATION_ID, equalTo(correlationId))
                .withHeader(ApiController.FNR, equalTo(fnr))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(authorization))
                .willReturn(aResponse().withBody(responseBody))
        )

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/harAFPoffentlig")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(ApiController.FNR, fnr)
                    .header(ApiController.CORRELATION_ID, correlationId)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(responseBody))

    }

    @Test
    fun `test internal server error returns bad gateway`() {
        val correlationId = UUID.randomUUID().toString()
        val fnr = "121212121212"
        val authorization = "bearer ${tokenGenerator.generateToken("nav:pensjon/v1/tpregisteret", "12345678910")}"

        stubFor(
            get(urlEqualTo("/api/tjenestepensjon/harAFPoffentlig"))
                .withHeader(ApiController.CORRELATION_ID, equalTo(correlationId))
                .withHeader(ApiController.FNR, equalTo(fnr))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(authorization))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()))
        )

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/harAFPoffentlig")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(ApiController.FNR, fnr)
                    .header(ApiController.CORRELATION_ID, correlationId)
            )
            .andExpect(MockMvcResultMatchers.status().isBadGateway)
    }
}
