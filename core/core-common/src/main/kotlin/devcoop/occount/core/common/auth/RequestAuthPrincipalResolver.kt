package devcoop.occount.core.common.auth

import jakarta.servlet.http.HttpServletRequest

object RequestAuthPrincipalResolver {
    fun resolve(request: HttpServletRequest): AuthPrincipal {
        val userId = request.getHeader(AuthHeaders.AUTHENTICATED_USER_ID)?.toLongOrNull()
            ?: throw InvalidAuthenticatedRequestException()

        return AuthPrincipal(
            userId = userId,
        )
    }
}
