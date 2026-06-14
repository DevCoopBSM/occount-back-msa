package devcoop.occount.payment.api.wallet

import devcoop.occount.payment.api.support.ApiAdviceHandler
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import devcoop.occount.payment.application.usecase.wallet.charge.BulkChargeWalletUseCase
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.Wallet
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import tools.jackson.module.kotlin.jacksonMapperBuilder
import kotlin.test.assertEquals

class WalletControllerTest {
    @Test
    fun `bulk charge returns 204 and increases user points`() {
        val walletRepository = FakeWalletRepository(
            mutableMapOf(
                1L to Wallet(userId = 1L, point = 100),
                2L to Wallet(userId = 2L, point = 200),
            ),
        )
        val chargeLogRepository = FakeChargeLogRepository()
        val mockMvc = mockMvc(walletRepository, chargeLogRepository)

        mockMvc.perform(
            post("/wallet/point/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userIds": [1, 2],
                      "amount": 50,
                      "reason": "체육대회 순위권 상금"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isNoContent)

        assertEquals(150, walletRepository.findByUserId(1L)?.point)
        assertEquals(250, walletRepository.findByUserId(2L)?.point)
        assertEquals(
            listOf("체육대회 순위권 상금", "체육대회 순위권 상금"),
            chargeLogRepository.savedChargeLogs.map { it.detailReason },
        )
    }

    @Test
    fun `bulk charge rejects empty user ids`() {
        val mockMvc = mockMvc(FakeWalletRepository(), FakeChargeLogRepository())

        mockMvc.perform(
            post("/wallet/point/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userIds": [],
                      "amount": 50,
                      "reason": "체육대회 순위권 상금"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.userIds").exists())
    }

    @Test
    fun `bulk charge rejects non positive amount and blank reason`() {
        val mockMvc = mockMvc(FakeWalletRepository(), FakeChargeLogRepository())

        mockMvc.perform(
            post("/wallet/point/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userIds": [1],
                      "amount": 0,
                      "reason": " "
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.amount").exists())
            .andExpect(jsonPath("$.reason").exists())
    }

    private fun mockMvc(
        walletRepository: FakeWalletRepository,
        chargeLogRepository: FakeChargeLogRepository,
    ) = LocalValidatorFactoryBean().let { validator ->
        validator.afterPropertiesSet()
        val messageConverter = JacksonJsonHttpMessageConverter(jacksonMapperBuilder())
        MockMvcBuilders.standaloneSetup(
            WalletController(
                getWalletPointQueryService = GetWalletPointQueryService(walletRepository),
                bulkChargeWalletUseCase = BulkChargeWalletUseCase(walletRepository, chargeLogRepository),
            ),
        )
            .setControllerAdvice(ApiAdviceHandler())
            .setMessageConverters(messageConverter)
            .setValidator(validator)
            .build()
    }

    private class FakeWalletRepository(
        private val wallets: MutableMap<Long, Wallet> = mutableMapOf(),
    ) : WalletRepository {
        override fun findByUserId(userId: Long): Wallet? = wallets[userId]

        override fun save(wallet: Wallet): Wallet {
            wallets[wallet.userId] = wallet
            return wallet
        }
    }

    private class FakeChargeLogRepository : ChargeLogRepository {
        val savedChargeLogs = mutableListOf<ChargeLog>()

        override fun findByPaymentId(paymentId: Long): ChargeLog? = null

        override fun save(chargeLog: ChargeLog): ChargeLog {
            savedChargeLogs += chargeLog
            return chargeLog
        }

        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
            savedChargeLogs += chargeLogs
            return chargeLogs
        }
    }
}
