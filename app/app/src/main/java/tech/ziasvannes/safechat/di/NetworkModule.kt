package tech.ziasvannes.safechat.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.ziasvannes.safechat.data.remote.ApiService
import tech.ziasvannes.safechat.data.remote.AuthInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
        private const val BASE_URL = "https://safechat.ziasvannes.tech/"

        @Provides @Singleton fun provideGson(): Gson = GsonBuilder().create()

        /**
         * Provides a singleton OkHttpClient configured with the given authentication interceptor.
         *
         * The returned OkHttpClient will apply the provided AuthInterceptor to all HTTP requests,
         * enabling request modification such as adding authentication headers.
         *
         * @param authInterceptor The interceptor used to handle authentication for network
         * requests.
         * @return A singleton OkHttpClient instance with the authentication interceptor applied.
         */
        @Provides
        @Singleton
        fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
                OkHttpClient.Builder().addInterceptor(authInterceptor).build()

        /**
         * Provides a singleton Retrofit instance configured with the specified base URL,
         * OkHttpClient, and Gson converter.
         *
         * @param gson The Gson instance used for JSON serialization and deserialization.
         * @param okHttpClient The OkHttpClient instance used to handle HTTP requests.
         * @return A configured Retrofit instance for network communication.
         */
        @Provides
        @Singleton
        fun provideRetrofit(gson: Gson, okHttpClient: OkHttpClient): Retrofit =
                Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build()

        @Provides
        @Singleton
        fun provideApiService(retrofit: Retrofit): ApiService =
                retrofit.create(ApiService::class.java)
}
