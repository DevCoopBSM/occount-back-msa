package devcoop.occount.warmup

import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
@ConditionalOnBean(EntityManagerFactory::class)
@ConditionalOnProperty(
    prefix = "app.startup-warmup",
    name = ["enabled", "jpa-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class JpaStartupWarmup(
    private val entityManagerFactory: EntityManagerFactory,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val entityNames = entityManagerFactory.metamodel.entities
            .map { it.name }
            .sorted()

        if (entityNames.isEmpty()) {
            return
        }

        val elapsed = runCatching {
            measureTimeMillis {
                repeat(POOL_WARMUP_COUNT) {
                    val entityManager = entityManagerFactory.createEntityManager()
                    try {
                        entityNames.forEach { entityName ->
                            entityManager.createQuery("select e from $entityName e")
                                .setHint("org.hibernate.readOnly", true)
                                .setMaxResults(1)
                                .resultList
                        }
                    } finally {
                        entityManager.close()
                    }
                }
            }
        }

        elapsed.onSuccess { duration ->
            log.info(
                "JPA startup warmup completed for {} entities ({} rounds) in {} ms",
                entityNames.size, POOL_WARMUP_COUNT, duration,
            )
        }.onFailure { exception ->
            log.warn("JPA startup warmup failed", exception)
        }
    }

    companion object {
        private const val POOL_WARMUP_COUNT = 10
        private val log = LoggerFactory.getLogger(JpaStartupWarmup::class.java)
    }
}
