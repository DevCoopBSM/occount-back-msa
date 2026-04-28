package devcoop.occount.warmup

import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import kotlin.system.measureTimeMillis

@Order(Ordered.HIGHEST_PRECEDENCE)
class JpaStartupWarmup(
    private val entityManagerFactory: EntityManagerFactory,
    private val properties: StartupWarmupProperties,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val entityNames = entityManagerFactory.metamodel.entities
            .map { it.name }
            .sorted()

        if (entityNames.isEmpty()) {
            return
        }

        val rounds = properties.jpaRepeat.coerceAtLeast(1)

        val elapsed = runCatching {
            measureTimeMillis {
                repeat(rounds) {
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
                entityNames.size, rounds, duration,
            )
        }.onFailure { exception ->
            log.warn("JPA startup warmup failed", exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(JpaStartupWarmup::class.java)
    }
}
