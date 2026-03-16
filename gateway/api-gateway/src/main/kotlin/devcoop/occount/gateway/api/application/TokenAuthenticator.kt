package devcoop.occount.gateway.api.application

interface TokenAuthenticator {
    fun authenticate(authorizationHeader: String?): AuthenticatedUser
}
