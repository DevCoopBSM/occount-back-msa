package devcoop.occount.member.infrastructure.portone

import devcoop.occount.member.application.exception.IdentityNotVerifiedException
import devcoop.occount.member.application.exception.IdentityVerificationFailedException
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@DisplayName("PortOneIdentityVerificationClient 단위 테스트")
class PortOneIdentityVerificationClientTest {
    private lateinit var httpClient: HttpClient
    private lateinit var client: PortOneIdentityVerificationClient

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = any<Any>() as T

    @BeforeEach
    fun setUp() {
        httpClient = mock(HttpClient::class.java)
        client = PortOneIdentityVerificationClient(
            PortOneProperties(secretKey = "test-secret"),
            httpClient,
            "http://portone.test/identity-verifications",
        )
    }

    private fun stubResponse(statusCode: Int, body: String) {
        @Suppress("UNCHECKED_CAST")
        val response = mock(HttpResponse::class.java) as HttpResponse<String>
        `when`(response.statusCode()).thenReturn(statusCode)
        `when`(response.body()).thenReturn(body)
        `when`(httpClient.send(anyArg(), anyArg<HttpResponse.BodyHandler<String>>())).thenReturn(response)
    }

    @Test
    @DisplayName("프로퍼티만으로 생성하면 기본 HttpClient와 기본 API URL을 사용한다")
    fun `default constructor builds its own http client and base url`() {
        val defaultClient = PortOneIdentityVerificationClient(PortOneProperties(secretKey = "sk"))

        org.junit.jupiter.api.Assertions.assertNotNull(defaultClient)
    }

    @Test
    @DisplayName("인증 성공 시 ci/이름/전화/생년월일을 매핑한다")
    fun `maps verified identity including birth date`() {
        stubResponse(
            200,
            """
            {
              "status": "VERIFIED",
              "verifiedCustomer": {
                "ci": "CI_123",
                "name": "홍길동",
                "phoneNumber": "01012345678",
                "birthDate": "2000-01-15"
              }
            }
            """.trimIndent(),
        )

        val result = client.verify("iv-1")

        assertEquals("CI_123", result.ciNumber)
        assertEquals("홍길동", result.username)
        assertEquals("01012345678", result.phone)
        assertEquals(LocalDate.of(2000, 1, 15), result.birthDate)
    }

    @Test
    @DisplayName("생년월일이 응답에 없으면 birthDate는 null이다")
    fun `birth date is null when absent`() {
        stubResponse(
            200,
            """{"status":"VERIFIED","verifiedCustomer":{"ci":"CI_123","name":"홍길동","phoneNumber":"010"}}""",
        )

        assertNull(client.verify("iv-1").birthDate)
    }

    @Test
    @DisplayName("생년월일 형식이 올바르지 않으면 birthDate는 null이다")
    fun `birth date is null when malformed`() {
        stubResponse(
            200,
            """{"status":"VERIFIED","verifiedCustomer":{"ci":"CI_123","name":"홍길동","phoneNumber":"010","birthDate":"not-a-date"}}""",
        )

        assertNull(client.verify("iv-1").birthDate)
    }

    @Test
    @DisplayName("상태가 VERIFIED가 아니면 IdentityNotVerifiedException을 던진다")
    fun `throws IdentityNotVerifiedException when status not verified`() {
        stubResponse(200, """{"status":"FAILED","verifiedCustomer":null}""")

        assertFailsWith<IdentityNotVerifiedException> { client.verify("iv-1") }
    }

    @Test
    @DisplayName("2xx가 아니면 IdentityVerificationFailedException을 던진다")
    fun `throws IdentityVerificationFailedException on non-2xx`() {
        stubResponse(500, "error")

        assertFailsWith<IdentityVerificationFailedException> { client.verify("iv-1") }
    }

    @Test
    @DisplayName("HTTP 호출이 예외를 던지면 IdentityVerificationFailedException으로 변환한다")
    fun `throws IdentityVerificationFailedException when http call fails`() {
        `when`(httpClient.send(anyArg(), anyArg<HttpResponse.BodyHandler<String>>()))
            .thenThrow(IOException("network"))

        assertFailsWith<IdentityVerificationFailedException> { client.verify("iv-1") }
    }

    @Test
    @DisplayName("응답 본문 파싱에 실패하면 IdentityVerificationFailedException을 던진다")
    fun `throws IdentityVerificationFailedException when body cannot be parsed`() {
        stubResponse(200, "not-json")

        assertFailsWith<IdentityVerificationFailedException> { client.verify("iv-1") }
    }

    @Test
    @DisplayName("필수 값(ci/이름)이 비어있으면 IdentityVerificationFailedException을 던진다")
    fun `throws IdentityVerificationFailedException when required fields blank`() {
        stubResponse(
            200,
            """{"status":"VERIFIED","verifiedCustomer":{"ci":"","name":"","phoneNumber":"010"}}""",
        )

        assertFailsWith<IdentityVerificationFailedException> { client.verify("iv-1") }
    }
}
