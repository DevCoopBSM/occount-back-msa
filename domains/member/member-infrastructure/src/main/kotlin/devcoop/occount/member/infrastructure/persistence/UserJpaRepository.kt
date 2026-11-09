package devcoop.occount.member.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import devcoop.occount.member.domain.user.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserJpaEntity u where u.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): UserJpaEntity?

    @Modifying
    @Query("update UserJpaEntity u set u.role = :memberRole where u.id = :id and u.role = :userRole")
    fun promoteToMember(
        @Param("id") id: Long,
        @Param("userRole") userRole: Role,
        @Param("memberRole") memberRole: Role,
    ): Int

    fun findByUserBarcode(userBarcode: String): UserJpaEntity?
    fun findByEmail(userEmail: String): UserJpaEntity?
    fun existsByEmail(userEmail: String): Boolean
    fun existsByPhone(phone: String): Boolean
    fun existsByUserCiNumber(userCiNumber: String): Boolean

    /**
     * username/email/cooperativeNumber 부분일치(OR) 검색.
     * keyword 는 LIKE 메타문자(%, _, \)가 이스케이프된 상태로 전달된다고 가정한다(어댑터에서 처리).
     */
    @Query(
        """
        SELECT u FROM UserJpaEntity u
        WHERE u.username LIKE CONCAT('%', :keyword, '%') ESCAPE '\'
           OR u.email LIKE CONCAT('%', :keyword, '%') ESCAPE '\'
           OR u.cooperativeNumber LIKE CONCAT('%', :keyword, '%') ESCAPE '\'
        """,
    )
    fun searchByKeyword(@Param("keyword") keyword: String, pageable: Pageable): Page<UserJpaEntity>
}
