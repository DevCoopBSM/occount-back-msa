package devcoop.occount.member.application.output

import devcoop.occount.member.domain.user.User

interface UserRepository {
    fun findById(id: Long): User?
    fun findByUserBarcode(userBarcode: String): User?
    fun findByEmail(userEmail: String): User?
    fun existsByEmail(userEmail: String): Boolean
    fun save(user: User): User
}
