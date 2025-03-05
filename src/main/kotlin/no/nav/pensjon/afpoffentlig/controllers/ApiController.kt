package no.nav.pensjon.afpoffentlig.controllers

import no.nav.pensjonsamhandling.maskinporten.validation.annotation.Maskinporten
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.exchange
import org.springframework.web.context.request.WebRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.util.*


@RestController
@RequestMapping("/")
class ApiController(
    @Value("\${TP_FSS_URL}") val tpFssUrl: String
) {
    private val restTemplate = RestTemplateBuilder().build()
    private val logger = LoggerFactory.getLogger(javaClass)
    companion object {
        const val FNR = "fnr"
        const val CORRELATION_ID = "correlationId"
    }

    @GetMapping("/harAFPoffentlig")
    @Maskinporten("nav:pensjon/v1/afpoffentlig")
    fun harAFPoffentlig(
        @RequestHeader(FNR) fnr: String,
        @RequestHeader(CORRELATION_ID, required = false) correlationId: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION) auth: String
    ): String {

        val xRequestId = correlationId  ?: UUID.randomUUID().toString()
        try {
            MDC.putCloseable("X-Request-Id", xRequestId).use {
                logger.info("Received call with X-Request-Id = $xRequestId, forwarding to TP.")
                return restTempalateToTP(fnr, xRequestId, auth).body
            }
        } catch(e: HttpClientErrorException) {
            logger.warn("Client Error with X-Request-Id = $xRequestId and statusCode: ${e.statusCode.value()}", e)
            throw ResponseStatusException(e.statusCode, e.responseBodyAsString)
        } catch (e: HttpServerErrorException) {
            logger.warn("Server Error with X-Request-Id = $xRequestId, from proxy and statusCode: ${e.statusCode.value()}", e)
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY)
        }
    }

    private fun restTempalateToTP(fnr: String, xRequestId: String, auth: String) = restTemplate.exchange<String>(
            UriComponentsBuilder.fromUriString("$tpFssUrl/api/afpoffentlig/harAFPoffentlig").build().toString(),
            HttpMethod.GET,
            HttpEntity<Any?>(buildHeaders(fnr, xRequestId, auth))
        ).also { logger.info("statuscode: {}, body: {}", it.statusCode, it.body) }

    private fun buildHeaders(fnr: String, xRequestId: String, auth: String) = HttpHeaders()
        .apply {
            add(FNR, fnr)
            add("X-Request-Id", xRequestId)
            add(HttpHeaders.AUTHORIZATION, auth)
        }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(e: ResponseStatusException, request: WebRequest): ResponseEntity<Any> {
        return ResponseEntity.status(e.statusCode).body(e.reason)
    }

}