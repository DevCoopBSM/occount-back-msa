package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.domain.payment.CardType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class VanResponseParser(
    private val protocolSpec: VanProtocolSpec,
    private val protocolCodes: VanProtocolCodes,
) {
    private val log = LoggerFactory.getLogger(VanResponseParser::class.java)
    private val eucKr: Charset = Charset.forName("EUC-KR")

    /**
     * VAN 원시 응답을 파싱하여 의미있는 객체로 변환
     * 필드 순서에 의존하지 않고 명확한 필드명으로 접근 가능
     */
    fun parseToRawResponse(responseBytes: ByteArray): VanRawResponse? {
        return try {
            val validData = extractValidData(responseBytes) ?: return null
            val fields = parseFields(validData)

            if (fields.isEmpty()) {
                log.warn("필드 파싱 실패: 빈 필드 목록")
                return null
            }

            log.debug("파싱된 필드 개수: {}", fields.size)
            log.trace("필드 데이터: {}", fields)

            mapFieldsToResponse(fields)
        } catch (e: Exception) {
            log.error("VAN 응답 파싱 에러: {}", e.message, e)
            null
        }
    }

    private fun extractValidData(response: ByteArray): ByteArray? {
        if (response.isEmpty()) return null

        // STX 바이트를 찾아서 유효한 전문 데이터 추출
        val stxIndex = response.indexOf(protocolSpec.stxByte)
        if (stxIndex == -1) {
            // STX가 없으면 제어 문자로 간주하고 무시
            log.trace("제어 문자 수신 (무시): {}", protocolSpec.toHex(response))
            return null
        }

        // STX부터 ETX+체크섬까지 또는 끝까지 추출
        val etxIndex = response.sliceArray(stxIndex until response.size).indexOf(protocolSpec.etxByte)
            .takeIf { it != -1 }?.let { it + stxIndex }
        return if (etxIndex != null && etxIndex < response.size - 1) {
            response.sliceArray(stxIndex..minOf(etxIndex + 1, response.size - 1))
        } else {
            response.sliceArray(stxIndex until response.size)
        }
    }

    private fun parseFields(validData: ByteArray): List<String> {
        return validData.toString(eucKr).split(protocolSpec.separatorChar)
    }

    private fun mapFieldsToResponse(fields: List<String>): VanRawResponse {
        val serviceType = parseServiceType(getField(fields, 0))
        val acquirer = parseInstitutionField(
            primary = getField(fields, 13),
            fallback = getField(fields, 12),
        )
        val issuer = parseInstitutionField(
            primary = getField(fields, 14),
            fallback = getField(fields, 13),
        )

        return VanRawResponse(
            protocolCodes = protocolCodes,
            serviceType = serviceType,
            typeCode = getField(fields, 1),
            cardNumber = getField(fields, 2),
            amount = getField(fields, 3)?.toIntOrNull(),
            installmentMonths = getField(fields, 5)?.toIntOrNull() ?: getField(fields, 4)?.toIntOrNull(),
            cancelType = getField(fields, 4),
            terminalId = getField(fields, 7) ?: getField(fields, 6),
            merchantNumber = getField(fields, 12) ?: getField(fields, 11),
            approvalDate = getField(fields, 8),
            approvalTime = getField(fields, 9),
            transactionId = getField(fields, 10),
            acquirerCode = acquirer.code ?: getField(fields, 11),
            acquirerName = acquirer.name,
            issuerName = issuer.name,
            issuerCode = issuer.code,
            cardType = parseCardType(getField(fields, 15)),
            status = parseStatus(fields),
            icCredit = getField(fields, 17),
            uuid = getField(fields, 19) ?: getField(fields, 18),
        )
    }

    private fun getField(fields: List<String>, index: Int): String? {
        return fields.getOrNull(index)?.takeIf { it.isNotBlank() }
    }

    private fun parseCardType(cardTypeField: String?): CardType? {
        return when (cardTypeField) {
            "2" -> CardType.CREDIT
            "1" -> CardType.DEBIT
            else -> null
        }
    }

    private fun parseStatus(fields: List<String>): String? {
        return getField(fields, 6) ?: getField(fields, 16) ?: getField(fields, 17)
    }

    private fun parseServiceType(rawHeader: String?): String? {
        val stxChar = (protocolSpec.stxByte.toInt() and 0xff).toChar()
        val normalized = rawHeader?.removePrefix(stxChar.toString()).orEmpty()
        if (normalized.length < 5) {
            return normalized.ifBlank { null }
        }

        return normalized.drop(4).ifBlank { null }
    }

    private fun parseInstitutionField(primary: String?, fallback: String? = null): InstitutionField {
        val value = primary ?: fallback?.takeIf(::looksLikeInstitutionField)
        if (value.isNullOrBlank()) {
            return InstitutionField(code = null, name = null)
        }

        val code = value.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }
        val name = if (code != null) value.drop(4).trim().ifBlank { null } else value.trim().ifBlank { null }
        return InstitutionField(code = code, name = name)
    }

    private fun looksLikeInstitutionField(value: String): Boolean {
        return value.length > 4 &&
            value.take(4).all(Char::isDigit) &&
            value.drop(4).any { !it.isDigit() }
    }

    private data class InstitutionField(
        val code: String?,
        val name: String?,
    )
}
