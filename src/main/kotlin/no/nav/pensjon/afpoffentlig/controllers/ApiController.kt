package no.nav.pensjon.afpoffentlig.controllers

import jakarta.servlet.http.HttpServletRequest
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
    private val restTemplate: RestTemplate,
    @Value("\${TP_FSS_URL}") val tpFssUrl: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ApiController::class.java)
        val correlationID = "correlationId"
    }

    @GetMapping("/harAFPoffentlig")
    fun harAFPoffentlig(@RequestHeader(FNR) fnr: String, request: HttpServletRequest): ResponseEntity<Any> {

        return try {
            restTemplate.exchange(
                UriComponentsBuilder.fromUriString("$tpFssUrl/api/tjenestepensjon/harAFPoffentlig")
                    .build().toString(),
                HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders()
                    .apply {
                        this.add(FNR, fnr)
                        request.getHeader(correlationID)?.let { this.add(correlationID, it) }
                    })
            )

        } catch(e: HttpClientErrorException) {
            throw ResponseStatusException(e.statusCode, e.responseBodyAsString)
        } catch (e: HttpServerErrorException) {
            logger.warn("${e.statusCode}-feil fra proxy", e)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException, request: WebRequest): ResponseEntity<Any> {
        return ResponseEntity.status(e.statusCode).body(e.reason)
    }

}