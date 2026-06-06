package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionAccount
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@DataJpaTest
@DisplayName("Contribution persistence version 테스트")
class ContributionPersistenceVersionTest @Autowired constructor(
    private val depositRequestRepository: ContributionDepositRequestPersistenceRepository,
    private val withdrawalRequestRepository: ContributionWithdrawalRequestPersistenceRepository,
    private val accountRepository: ContributionAccountPersistenceRepository,
) {
    @Test
    @DisplayName("첫 입금 신청 저장은 null version으로 insert되고 저장 후 version이 부여된다")
    fun `new deposit request is inserted with null version`() {
        val request = ContributionDepositRequest.request(userId = 1L, amount = 10_000)

        val entity = ContributionDepositRequestPersistenceMapper.toEntity(request)
        assertNull(entity.version)

        val saved = depositRequestRepository.saveAndFlush(entity)

        assertNotNull(saved.id)
        assertEquals(0L, saved.version)
    }

    @Test
    @DisplayName("첫 출금 신청 저장은 null version으로 insert되고 저장 후 version이 부여된다")
    fun `new withdrawal request is inserted with null version`() {
        val request = ContributionWithdrawalRequest.request(userId = 1L, amount = 5_000)

        val entity = ContributionWithdrawalRequestPersistenceMapper.toEntity(request)
        assertNull(entity.version)

        val saved = withdrawalRequestRepository.saveAndFlush(entity)

        assertNotNull(saved.id)
        assertEquals(0L, saved.version)
    }

    @Test
    @DisplayName("첫 출자금 계좌 저장은 null version으로 insert되고 저장 후 version이 부여된다")
    fun `new contribution account is inserted with null version`() {
        val account = ContributionAccount(userId = 1L)

        val entity = ContributionAccountPersistenceMapper.toEntity(account)
        assertNull(entity.version)

        val saved = accountRepository.saveAndFlush(entity)

        assertEquals(1L, saved.userId)
        assertEquals(0L, saved.version)
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = [ContributionDepositRequestJpaEntity::class])
@EnableJpaRepositories(basePackageClasses = [ContributionDepositRequestPersistenceRepository::class])
class ContributionPersistenceVersionTestConfig
