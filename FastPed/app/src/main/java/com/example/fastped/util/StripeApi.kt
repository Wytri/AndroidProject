package com.example.fastped.util

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object StripeApi {
    private const val SECRET_KEY = "sk_test_51RnEgGPW2JlMPOP0CNlcvuHD1CYieJzQdwzWRnBbS6CUFlDLJpnY8Cw8yt3fnwEUo7sMzmYNdDcF8wzbkzKZVqmN000G7nTCzb"
    private val client = OkHttpClient()

    /**
     * Crea un PaymentIntent en Stripe y devuelve el client_secret via callback.
     * amount en centavos (p.ej. S/12.50 => 1250)
     */
    fun createPaymentIntent(
        amount: Int,
        currency: String = "pen",
        callback: (Result<String>) -> Unit
    ) {
        val formBody = FormBody.Builder()
            .add("amount", amount.toString())
            .add("currency", currency)
            .add("payment_method_types[]", "card")
            .build()

        val request = Request.Builder()
            .url("https://api.stripe.com/v1/payment_intents")
            .post(formBody)
            .header("Authorization", Credentials.basic(SECRET_KEY, ""))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(Result.failure(IOException("Código HTTP ${it.code}")))
                        return
                    }
                    val json = JSONObject(it.body!!.string())
                    val clientSecret = json.getString("client_secret")
                    callback(Result.success(clientSecret))
                }
            }
        })
    }
}
