package devcoop.occount.member.infrastructure.client.wallet

import devcoop.occount.member.application.exception.PointNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class WalletPointReaderImplTest {
    @Test
    fun `getPoint returns point from wallet client`() {
        val walletClient = mock(WalletClient::class.java)
        val walletPointReader = WalletPointReaderImpl(walletClient)
        `when`(walletClient.getPoint("7")).thenReturn(WalletPointClientResponse(point = 5000))

        val point = walletPointReader.getPoint(7L)

        assertEquals(5000, point)
    }

    @Test
    fun `getPoint throws PointNotFoundException when wallet client returns 404`() {
        val walletClient = mock(WalletClient::class.java)
        val walletPointReader = WalletPointReaderImpl(walletClient)
        `when`(walletClient.getPoint("7")).thenThrow(
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                ByteArray(0),
                null,
            ),
        )

        assertThrows(PointNotFoundException::class.java) {
            walletPointReader.getPoint(7L)
        }
    }
}
