package com.websocket_client

import android.content.Context

import java.io.IOException
import java.io.InputStream
import java.security.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

class SSL {
    companion object {
        fun getContext(): SSLContext {
            val x509TrustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }

            try {
                val sslContext: SSLContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf<TrustManager>(x509TrustManager), null)

                return sslContext
            } catch (e: KeyStoreException) {
                throw IllegalArgumentException()
            } catch (e: IOException) {
                throw IllegalArgumentException()
            } catch (e: CertificateException) {
                throw IllegalArgumentException()
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalArgumentException()
            } catch (e: KeyManagementException) {
                throw IllegalArgumentException()
            } catch (e: UnrecoverableKeyException) {
                throw IllegalArgumentException()
            }
        }

        fun getContextFromAndroidKeystore(context: Context, storePassword: String, keyPassword: String, keyStorePath: String, keyStoreType: String = "BKS"): SSLContext {
            try {
                val keystore: KeyStore = KeyStore.getInstance(keyStoreType)

                val inputStream: InputStream = context.assets.open(keyStorePath)
                inputStream.use { _inputStream ->
                    keystore.load(_inputStream, storePassword.toCharArray())
                }

                val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("X509")
                keyManagerFactory.init(keystore, keyPassword.toCharArray())

                val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("X509")
                tmf.init(keystore)

                val sslContext: SSLContext = SSLContext.getInstance("TLS")
                sslContext.init(keyManagerFactory.keyManagers, tmf.trustManagers, null)

                return sslContext
            } catch (e: KeyStoreException) {
                throw IllegalArgumentException()
            } catch (e: IOException) {
                throw IllegalArgumentException()
            } catch (e: CertificateException) {
                throw IllegalArgumentException()
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalArgumentException()
            } catch (e: KeyManagementException) {
                throw IllegalArgumentException()
            } catch (e: UnrecoverableKeyException) {
                throw IllegalArgumentException()
            }
        }
    }
}
