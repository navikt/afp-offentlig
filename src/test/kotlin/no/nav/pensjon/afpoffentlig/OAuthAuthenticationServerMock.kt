package no.nav.pensjon.afpoffentlig

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Body
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/*
 * Mocker et par authorization server-endepunkter og signerer jwt som kan verifiseres med offentlig nøkkel
 */
class OAuthAuthenticationServerMock(private val issuer: String, private val port: Int? = null, startHttpServer: Boolean) {

    private var httpServer: WireMockServer? = null

    private val rsaKey: RSAKey = RSAKeyGenerator(2048)
        .keyUse(KeyUse.SIGNATURE)
        .keyID(UUID.randomUUID().toString())
        .generate()

    init {
        if (startHttpServer) {
            httpServer = setupWiremock()
        }
    }

    fun generateClientJWT(scopes: List<String>, issuer: String, expiration: Date? = null, notBeforeTime: Date? = null): String =
        SignedJWT(generateSignatureHeader(), generateJWTClaimSet(scopes, issuer, expiration, notBeforeTime))
            .apply {
                sign(RSASSASigner(rsaKey))
            }.serialize()

    fun stopHttpServer() = httpServer!!.stop()

    private fun generateSignatureHeader() = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build()

    private fun generateJWTClaimSet(
        scopes: List<String>, issuer: String, expiration: Date? = inAnHour(), notBeforeTime: Date? = Date()
    ) =
        JWTClaimsSet.Builder().apply {
            this.issuer(issuer)
            this.claim("scope", scopes.joinToString(" "))
            issueTime(Date())
            expirationTime(expiration)
            notBeforeTime(notBeforeTime)
        }.build()


    private fun setupWiremock() =
        WireMockServer(WireMockConfiguration.options().port(port!!)).apply {
            this.stubFor(WireMock.get("/").willReturn(WireMock.ok().withResponseBody(Body(issuerEndpoint()))))
            this.stubFor(WireMock.get("/jwk").willReturn(WireMock.ok().withResponseBody(Body(jwkEndpoint()))))
            this.stubFor(
                WireMock.get("/.well-known/oauth-authorization-server/").willReturn(
                    WireMock.ok()
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withResponseBody(Body(wellKnownEndpoint().toByteArray()))
                )
            )
            this.start()
        }

    private fun issuerEndpoint() = wellKnownEndpoint()

    private fun wellKnownEndpoint() =
        """{
              "issuer": "$issuer",
              "token_endpoint": "http://localhost:$port/token",
              "jwks_uri": "http://localhost:$port/jwk",
              "token_endpoint_auth_methods_supported": [
                "private_key_jwt"
              ],
              "grant_types_supported": [
                "urn:ietf:params:oauth:grant-type:jwt-bearer"
              ],
              "token_endpoint_auth_signing_alg_values_supported": [
                "RS256",
                "RS384",
                "RS512"
              ]
            }"""

    private fun jwkEndpoint() = "{\"keys\": [${rsaKey.toPublicJWK()}]}"

    private fun inAnHour() = Date.from(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toInstant())
}