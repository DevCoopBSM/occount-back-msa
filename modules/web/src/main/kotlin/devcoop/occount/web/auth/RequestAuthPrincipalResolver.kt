package devcoop.occount.web.auth

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.InvalidAuthenticatedRequestException
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
