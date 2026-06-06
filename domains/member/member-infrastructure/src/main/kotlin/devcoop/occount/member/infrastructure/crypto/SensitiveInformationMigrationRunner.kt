package devcoop.occount.member.infrastructure.crypto

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SensitiveInformationMigrationRunner(
    private val jdbcTemplate: JdbcTemplate,
    private val dataSource: DataSource,
    private val cryptoHelper: CryptoHelper,
    private val sensitiveInformationHasher: SensitiveInformationHasher,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        migrate()
    }

    fun migrate(): Int {
        dropLegacyUniqueIndexes()

        val rows = jdbcTemplate.query(
            """
            select id, username, phone, user_ci_number, phone_hash, user_ci_number_hash
            from common_user
            """.trimIndent(),
        ) { rs, _ ->
            SensitiveInformationRow(
                id = rs.getLong("id"),
                username = rs.getString("username"),
                phone = rs.getString("phone"),
                userCiNumber = rs.getString("user_ci_number"),
                phoneHash = rs.getString("phone_hash"),
                userCiNumberHash = rs.getString("user_ci_number_hash"),
            )
        }

        var migratedCount = 0
        rows.forEach { row ->
            val migrated = row.migrate()
            if (migrated != null) {
                jdbcTemplate.update(
                    """
                    update common_user
                    set username = ?, phone = ?, user_ci_number = ?, phone_hash = ?, user_ci_number_hash = ?
                    where id = ?
                    """.trimIndent(),
                    migrated.username,
                    migrated.phone,
                    migrated.userCiNumber,
                    migrated.phoneHash,
                    migrated.userCiNumberHash,
                    row.id,
                )
                migratedCount++
            }
        }

        if (migratedCount > 0) {
            log.info("Migrated member sensitive information. count={}", migratedCount)
        }
        return migratedCount
    }

    private fun dropLegacyUniqueIndexes() {
        dataSource.connection.use { connection ->
            val databaseProductName = connection.metaData.databaseProductName
            if (!databaseProductName.contains("MySQL", ignoreCase = true) &&
                !databaseProductName.contains("MariaDB", ignoreCase = true)
            ) {
                return
            }

            val legacyIndexNames = mutableSetOf<String>()
            connection.metaData.getIndexInfo(connection.catalog, null, COMMON_USER_TABLE, true, false).use { indexes ->
                val columnsByIndexName = linkedMapOf<String, MutableSet<String>>()
                while (indexes.next()) {
                    val indexName = indexes.getString("INDEX_NAME") ?: continue
                    if (indexName.equals("PRIMARY", ignoreCase = true)) {
                        continue
                    }
                    val columnName = indexes.getString("COLUMN_NAME") ?: continue
                    columnsByIndexName.getOrPut(indexName) { linkedSetOf() } += columnName
                }

                columnsByIndexName.forEach { (indexName, columnNames) ->
                    if (columnNames == setOf(PHONE_COLUMN) || columnNames == setOf(USER_CI_NUMBER_COLUMN)) {
                        legacyIndexNames += indexName
                    }
                }
            }

            legacyIndexNames.forEach { indexName ->
                runCatching {
                    jdbcTemplate.execute("alter table $COMMON_USER_TABLE drop index `${indexName.replace("`", "``")}`")
                    log.info("Dropped legacy unique index on encrypted member sensitive information. index={}", indexName)
                }.onFailure { exception ->
                    log.warn(
                        "Failed to drop legacy unique index on encrypted member sensitive information. index={}",
                        indexName,
                        exception,
                    )
                }
            }
        }
    }

    private fun SensitiveInformationRow.migrate(): SensitiveInformationRow? {
        val plainUsername = decryptIfEncrypted(username)
        val plainPhone = decryptIfEncrypted(phone)
        val plainUserCiNumber = decryptIfEncrypted(userCiNumber)
        val migrated = SensitiveInformationRow(
            id = id,
            username = encryptIfPlain(username),
            phone = encryptIfPlain(phone),
            userCiNumber = encryptIfPlain(userCiNumber),
            phoneHash = sensitiveInformationHasher.hash(plainPhone),
            userCiNumberHash = sensitiveInformationHasher.hash(plainUserCiNumber),
        )

        return migrated.takeIf {
            it.username != username ||
                it.phone != phone ||
                it.userCiNumber != userCiNumber ||
                it.phoneHash != phoneHash ||
                it.userCiNumberHash != userCiNumberHash
        }
    }

    private fun encryptIfPlain(value: String?): String? {
        if (value.isNullOrEmpty() || cryptoHelper.isEncrypted(value)) {
            return value
        }
        return cryptoHelper.encrypt(value)
    }

    private fun decryptIfEncrypted(value: String?): String? {
        if (value.isNullOrEmpty() || !cryptoHelper.isEncrypted(value)) {
            return value
        }
        return cryptoHelper.decrypt(value)
    }

    private data class SensitiveInformationRow(
        val id: Long,
        val username: String?,
        val phone: String?,
        val userCiNumber: String?,
        val phoneHash: String?,
        val userCiNumberHash: String?,
    )

    companion object {
        private const val COMMON_USER_TABLE = "common_user"
        private const val PHONE_COLUMN = "phone"
        private const val USER_CI_NUMBER_COLUMN = "user_ci_number"
        private val log = LoggerFactory.getLogger(SensitiveInformationMigrationRunner::class.java)
    }
}
