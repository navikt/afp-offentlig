package no.nav.pensjon.afpoffentlig.controllers

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder


@RestController
@RequestMapping("ping")
class PingController(
    private val restTemplate: RestTemplate,
    @Value("\${TP_FSS_URL}") val penProxyFssUrl: String)
{
    companion object {
        private val logger = LoggerFactory.getLogger(PingController::class.java)
    }

    @GetMapping("")
    fun ping(request: HttpServletRequest) = "pong"

    @GetMapping("deep")
    fun deepPing(): String? {
        return try {
            restTemplate.getForObject(UriComponentsBuilder.fromUriString("$penProxyFssUrl/ping/deep").build().toUri(), String::class.java)
        } catch(e: Exception) {
            logger.error("Ping mot pensjon-pen-proxy-fss feilet.", e)
            throw HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}