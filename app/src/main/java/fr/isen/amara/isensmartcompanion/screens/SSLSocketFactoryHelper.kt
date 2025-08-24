package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Helper historique (comme l'ancien projet).
 * Retourne une SSLSocketFactory qui accepte tous les certificats.
 * Pas utilisé directement si tu passes par TrustAllSocketFactory, mais on le garde
 * pour compatibilité avec l’ancien code si besoin.
 */
object SSLSocketFactoryHelper {
    fun getSocketFactory(@Suppress("UNUSED_PARAMETER") context: Context): SSLSocketFactory {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        })
        val sc = SSLContext.getInstance("TLS")
        sc.init(null, trustAll, SecureRandom())
        return sc.socketFactory
    }
}