package devcoop.occount.warmup

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Query
import jakarta.persistence.metamodel.EntityType
import jakarta.persistence.metamodel.Metamodel
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.DefaultApplicationArguments

class JpaStartupWarmupTest {
    @Test
    fun `run warms every discovered entity across configured rounds`() {
        val entityManagerFactory = mock(EntityManagerFactory::class.java)
        val entityManager = mock(EntityManager::class.java)
        val metamodel = mock(Metamodel::class.java)
        val firstEntity = mock(EntityType::class.java) as EntityType<*>
        val secondEntity = mock(EntityType::class.java) as EntityType<*>
        val query = mock(Query::class.java)

        `when`(entityManagerFactory.metamodel).thenReturn(metamodel)
        `when`(entityManagerFactory.createEntityManager()).thenReturn(entityManager)
        `when`(metamodel.entities).thenReturn(linkedSetOf(firstEntity, secondEntity))
        `when`(firstEntity.name).thenReturn("FirstEntity")
        `when`(secondEntity.name).thenReturn("SecondEntity")
        `when`(entityManager.createQuery("select e from FirstEntity e")).thenReturn(query)
        `when`(entityManager.createQuery("select e from SecondEntity e")).thenReturn(query)
        `when`(query.setHint("org.hibernate.readOnly", true)).thenReturn(query)
        `when`(query.setMaxResults(1)).thenReturn(query)
        `when`(query.resultList).thenReturn(emptyList<Any>())

        val properties = StartupWarmupProperties().apply { jpaRepeat = 3 }
        JpaStartupWarmup(entityManagerFactory, properties).run(DefaultApplicationArguments())

        verify(entityManagerFactory, times(3)).createEntityManager()
        verify(entityManager, times(3)).createQuery("select e from FirstEntity e")
        verify(entityManager, times(3)).createQuery("select e from SecondEntity e")
        verify(query, times(6)).setHint("org.hibernate.readOnly", true)
        verify(query, times(6)).setMaxResults(1)
        verify(entityManager, times(3)).close()
    }
}
