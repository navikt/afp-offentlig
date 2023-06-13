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
    fun harAFPoffentlig(@RequestHeader(FNR) fnr: String, @RequestHeader(CORRELATION_ID, required = false) correlationID: String?, @RequestHeader(HttpHeaders.AUTHORIZATION) auth: String): ResponseEntity<String> {

        return try {
            restTemplate.exchange(
                UriComponentsBuilder.fromUriString("$tpFssUrl/api/tjenestepensjon/harAFPoffentlig")
                    .build().toString(),
                HttpMethod.GET,
                HttpEntity<Nothing?>(HttpHeaders()
                    .apply {
                        add(FNR, fnr)
                        correlationID?.let { add(CORRELATION_ID, it) }
                        add(HttpHeaders.AUTHORIZATION, auth)
                    })
            )

        } catch(e: HttpClientErrorException) {
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