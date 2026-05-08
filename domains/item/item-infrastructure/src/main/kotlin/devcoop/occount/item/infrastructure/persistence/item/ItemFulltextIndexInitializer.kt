package devcoop.occount.item.infrastructure.persistence.item

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class ItemFulltextIndexInitializer(
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val exists = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'item'
              AND INDEX_NAME = '$INDEX_NAME'
            """.trimIndent(),
            Int::class.java,
        ) ?: 0

        if (exists > 0) {
            return
        }

        jdbcTemplate.execute(
            "ALTER TABLE item ADD FULLTEXT INDEX $INDEX_NAME (name) WITH PARSER ngram",
        )
        log.info("Created FULLTEXT index {} on item.name with ngram parser", INDEX_NAME)
    }

    companion object {
        private const val INDEX_NAME = "ft_item_name"
    }
}
