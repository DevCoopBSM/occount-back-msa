package devcoop.occount.member.infrastructure.crypto

class UniqueEncryptedValueGenerator(
    private val encrypt: (String?) -> String?,
) {
    constructor(cryptoHelper: CryptoHelper) : this(cryptoHelper::encrypt)

    fun generate(
        plainText: String?,
        existsByEncryptedValue: (String) -> Boolean,
    ): String? {
        if (plainText.isNullOrEmpty()) {
            return plainText
        }

        repeat(MAX_ATTEMPTS) {
            val encryptedValue = encrypt(plainText) ?: return null
            if (!existsByEncryptedValue(encryptedValue)) {
                return encryptedValue
            }
        }

        throw IllegalStateException("Failed to generate unique encrypted value.")
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
    }
}
