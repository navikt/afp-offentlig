package no.nav.pensjon.afpoffentlig.controllers

import no.nav.pensjonsamhandling.maskinporten.validation.annotation.Maskinporten
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder


@RestController
@RequestMapping("/")
class ApiController(
    @Value("\${TP_FSS_URL}") val tpFssUrl: String
) {
    private val restTemplate = RestTemplate()
    private val logger = LoggerFactory.getLogger(javaClass)
    companion object {
        const val FNR = "fnr"
        const val CORRELATION_ID = "correlationId"
    }

    @GetMapping("/harAFPoffentlig")
    @Maskinporten("nav:pensjon/v1/tpregisteret")
    fun harAFPoffentlig(
        @RequestHeader(FNR) fnr: String,
        @RequestHeader(CORRELATION_ID, required = false) correlationId: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION) auth: String
    ): ResponseEntity<String> {

        return try {
            logger.debug("Received call with correlationId = $correlationId, forwarding to TP.")
            restTemplate.exchange<String>(
                UriComponentsBuilder.fromUriString("$tpFssUrl/api/tjenestepensjon/harAFPoffentlig").build().toString(),
                HttpMethod.GET,
                HttpEntity<Nothing?>(HttpHeaders()
                    .apply {
                        add(FNR, fnr)
                        correlationId?.let { add(CORRELATION_ID, it) }
                        add(HttpHeaders.AUTHORIZATION, auth)
                    })
            ).also { logger.debug("statuscode: ${it.statusCode}, body: ${it.body}") }

        } catch(e: HttpClientErrorException) {
            logger.debug("Call with correlationId = $correlationId received error from TP: ${e.statusCode.value()} - ${e.responseBodyAsString}")
            throw ResponseStatusException(e.statusCode, e.responseBodyAsString)
        } catch (e: HttpServerErrorException) {
            logger.warn("${e.statusCode}-feil fra proxy", e)
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY)
        }
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException, request: WebRequest): ResponseEntity<Any> {
        return ResponseEntity.status(e.statusCode).body(e.reason)
    }

}