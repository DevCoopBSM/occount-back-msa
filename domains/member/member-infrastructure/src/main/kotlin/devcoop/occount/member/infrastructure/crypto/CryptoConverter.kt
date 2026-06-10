package devcoop.occount.member.infrastructure.crypto

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class CryptoConverter : AttributeConverter<String, String> {
    override fun convertToDatabaseColumn(attribute: String?): String? {
        return helper().encrypt(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return helper().decryptIfEncrypted(dbData)
    }

    private fun helper(): CryptoHelper {
        return cryptoHelper
            ?: throw IllegalStateException("CryptoConverter is not configured. Check app.encryption.secret-key.")
    }

    companion object {
        @Volatile
        private var cryptoHelper: CryptoHelper? = null

        fun configure(cryptoHelper: CryptoHelper) {
            this.cryptoHelper = cryptoHelper
        }
    }
}
