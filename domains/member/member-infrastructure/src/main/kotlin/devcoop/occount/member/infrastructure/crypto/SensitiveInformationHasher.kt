package devcoop.occount.member.infrastructure.crypto

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SensitiveInformationHasher(
    secretKey: String,
) {
    private val secretKeySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256)

    fun hash(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }

        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(secretKeySpec)
        return mac.doFinal(value.toByteArray(StandardCharsets.UTF_8)).toHex()
    }

    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
    }
}

object SensitiveInformationHash {
    @Volatile
    private var hasher: SensitiveInformationHasher? = null

    fun configure(hasher: SensitiveInformationHasher) {
        this.hasher = hasher
    }

    fun hash(value: String?): String? {
        return (hasher ?: throw IllegalStateException("SensitiveInformationHash is not configured."))
            .hash(value)
    }
}

private fun ByteArray.toHex(): String {
    return joinToString(separator = "") { byte -> "%02x".format(byte) }
}
