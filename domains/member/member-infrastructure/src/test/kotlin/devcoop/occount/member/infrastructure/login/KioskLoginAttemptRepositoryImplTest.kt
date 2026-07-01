package devcoop.occount.member.infrastructure.login

import devcoop.occount.member.application.login.KioskLoginAttempt
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant

@Testcontainers
@SpringBootTest(classes = [KioskLoginAttemptRepositoryImplTestConfig::class])
@DisplayName("KioskLoginAttemptRepositoryImpl MySQL 통합 테스트")
class KioskLoginAttemptRepositoryImplTest @Autowired constructor(
    private val kioskLoginAttemptJpaRepository: KioskLoginAttemptJpaRepository,
) {
    private val repository = KioskLoginAttemptRepositoryImpl(kioskLoginAttemptJpaRepository)

    @Test
    @DisplayName("같은 사용자 바코드를 다시 저장하면 기존 로그인 실패 기록을 갱신한다")
    fun `save upserts login attempt for same user barcode`() {
        val userBarcode = "8554L"
        val firstUpdatedAt = Instant.parse("2026-07-01T01:00:00Z")
        val secondUpdatedAt = Instant.parse("2026-07-01T01:05:00Z")
        val lockedUntil = Instant.parse("2026-07-01T01:15:00Z")

        repository.save(
            KioskLoginAttempt(
                userBarcode = userBarcode,
                failCount = 1,
                lockedUntil = null,
                updatedAt = firstUpdatedAt,
            ),
        )

        assertDoesNotThrow {
            repository.save(
                KioskLoginAttempt(
                    userBarcode = userBarcode,
                    failCount = 2,
                    lockedUntil = lockedUntil,
                    updatedAt = secondUpdatedAt,
                ),
            )
        }

        val found = repository.findByBarcode(userBarcode)

        assertNotNull(found)
        assertEquals(1L, kioskLoginAttemptJpaRepository.count())
        assertEquals(2, found!!.failCount)
        assertEquals(lockedUntil, found.lockedUntil)
        assertEquals(secondUpdatedAt, found.updatedAt)
    }

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun mysqlProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
        }
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = [KioskLoginAttemptJpaEntity::class])
@EnableJpaRepositories(basePackageClasses = [KioskLoginAttemptJpaRepository::class])
class KioskLoginAttemptRepositoryImplTestConfig
