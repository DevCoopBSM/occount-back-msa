package devcoop.occount.order.application.support

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal object GuestOrderToken {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generate(): String = encoder.encodeToString(ByteArray(32).also(random::nextBytes))

    fun hash(token: String): String = encoder.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)),
    )

    fun matches(token: String, hash: String): Boolean = token.length == 43 &&
        MessageDigest.isEqual(hash(token).toByteArray(Charsets.US_ASCII), hash.toByteArray(Charsets.US_ASCII))
}
