package no.nav.pensjon.afpoffentlig

import no.nav.pensjon.afpoffentlig.annotations.SecurityDisabled
import no.nav.pensjon.afpoffentlig.controllers.ApiController
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.net.URI
import java.util.*
import java.util.stream.Stream

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(RestTemplateTestConfig::class)
@SecurityDisabled
class ProxyMappingTest(@Autowired private val mockMvc: MockMvc, @Autowired restTemplate: RestTemplate, @Value("\${TP_FSS_URL}") private val tpfssUrl: String) {

    private val mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val requestBody = "test request body"
    private val responseBody = """{"json":"test"}"""

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("TP_FSS_URL") { "http://tp-api-q1.dev-fss-pub.nais.io" }
        }
    }

    @Test
    fun `test gets, request is forwarded with correct method, correlationId header and status - body and ok status is returned correct`() {
        val correlationId = UUID.randomUUID().toString()

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo(URI("$tpfssUrl/api/tjenestepensjon/harAFPoffentlig")))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header(ApiController.correlationID, correlationId))
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK)
                .body(responseBody).contentType(MediaType.APPLICATION_JSON))

        this.mockMvc
            .perform(MockMvcRequestBuilders.get("/harAFPoffentlig")
                .header("fnr", "121212121212")
                .header(ApiController.correlationID, correlationId))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().string(responseBody))

        mockRestServiceServer.verify()
    }
}

@TestConfiguration
class RestTemplateTestConfig(@Value("\${TP_FSS_URL}") private val proxyUrl: String) {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplateBuilder()
            .uriTemplateHandler(DefaultUriBuilderFactory(proxyUrl))
            .build()
    }
}