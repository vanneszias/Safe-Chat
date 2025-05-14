package tech.ziasvannes.safechat.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import tech.ziasvannes.safechat.session.UserSession

@Singleton
class AuthInterceptor @Inject constructor(private val userSession: UserSession) : Interceptor {
    /**
     * Intercepts outgoing HTTP requests to add an Authorization header with a bearer token if available.
     *
     * If a non-null, non-blank token exists in the user session, the request is modified to include
     * an Authorization header using the bearer token. Otherwise, the original request is sent unmodified.
     *
     * @param chain The OkHttp interceptor chain containing the request.
     * @return The HTTP response from proceeding with the (possibly modified) request.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = userSession.token
        return if (!token.isNullOrBlank()) {
            val authenticatedRequest =
                    originalRequest.newBuilder().addHeader("Authorization", "Bearer $token").build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
