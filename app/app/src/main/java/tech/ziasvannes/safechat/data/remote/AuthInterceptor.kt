package tech.ziasvannes.safechat.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import tech.ziasvannes.safechat.session.UserSession

@Singleton
class AuthInterceptor @Inject constructor(private val userSession: UserSession) : Interceptor {
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
