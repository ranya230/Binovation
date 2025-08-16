// FICHIER: TrustAllSocketFactory.kt
// PACKAGE: fr.isen.amara.isensmartcompanion.screens

package fr.isen.amara.isensmartcompanion.screens

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object TrustAllSocketFactory {
    fun socketFactory(): SSLSocketFactory {
        // TrustManager qui ne vérifie rien
        val trustAllManagers = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Construit un SSLContext "TLS" avec ce TrustManager
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllManagers, SecureRandom())
        }
        return sslContext.socketFactory
    }
}