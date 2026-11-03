package com.uvg.agroconecta.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException

private val socketDisconnectMessages = setOf("Connection reset", "Connection reset by peer", "Broken pipe")
private val safeMethods = setOf("GET", "HEAD", "OPTIONS")

/** Tres intentos en total. Los dos reintentos esperan 200 y 400 ms respectivamente. */
internal class RetryInterceptor(
    private val waitMillis: (Long) -> Unit = Thread::sleep
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val retryable = request.method in safeMethods &&
            request.body?.isOneShot() != true && request.body?.isDuplex() != true
        var retries = 0
        while (true) {
            try {
                return HttpFollowUpGuard.restore(chain.proceed(request))
            } catch (failure: IOException) {
                if (!retryable || retries == 2 || chain.call().isCanceled() || !failure.isTransient()) {
                    throw failure
                }
                retries++
                try {
                    waitMillis(200L * retries)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw InterruptedIOException("Espera de reintento interrumpida").apply { initCause(interrupted) }
                }
                if (chain.call().isCanceled()) throw failure
            }
        }
    }

    private fun IOException.isTransient(): Boolean {
        val causes = generateSequence<Throwable>(this) { it.cause }
        return causes.none { it.isPermanent() } && causes.any { it.isTransportFailure() }
    }

    private fun Throwable.isPermanent(): Boolean =
        this is SSLException || this is CertificateException || this is ProtocolException ||
            (this is InterruptedIOException && this !is SocketTimeoutException)

    private fun Throwable.isTransportFailure(): Boolean = when (this) {
        is SocketTimeoutException, is EOFException, is ConnectException, is NoRouteToHostException -> true
        is SocketException -> message in socketDisconnectMessages
        else -> false
    }
}

/**
 * OkHttp 4.12 procesa algunos estados HTTP incluso con retryOnConnectionFailure(false).
 * Solo durante ese procesamiento usamos un estado sin seguimiento automático. La etiqueta
 * local conserva la respuesta de red original sin consumir su cuerpo ni alterar cabeceras.
 */
internal object HttpFollowUpGuard : Interceptor {
    private class Original(val networkResponse: Response)

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val suppressFollowUp = when (response.code) {
            421, 503 -> true
            307, 308 -> response.request.method !in safeMethods
            else -> false
        }
        if (!suppressFollowUp) return response

        val original = Original(response.newBuilder().body(null).build())
        val tagged = response.request.newBuilder().tag(Original::class.java, original).build()
        return response.newBuilder().request(tagged).code(599).build()
    }

    fun restore(response: Response): Response {
        val original = response.networkResponse?.request?.tag(Original::class.java)?.networkResponse
            ?: return response
        return response.newBuilder().code(original.code).networkResponse(original).build()
    }
}
