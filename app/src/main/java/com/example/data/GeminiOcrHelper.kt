package com.example.data

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object GeminiOcrHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if a valid Gemini API Key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        // Safe check for BuildConfig fields
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.startsWith("your_")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Scans an ID card image (Base64) using the Gemini 2.5 Flash model.
     * Returns structured guest details.
     */
    fun scanIdCard(
        base64Image: String,
        onResult: (name: String?, idNumber: String?, idType: String?, errorMsg: String?) -> Unit
    ) {
        if (!isApiKeyConfigured()) {
            onResult(null, null, null, "لم يتم تهيئة مفتاح API للذكاء الاصطناعي")
            return
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        // Construct a structured prompt to guide the output content and force JSON
        val prompt = """
            Analyze this uploaded ID card, Passport, or Residency ID card. 
            Extract characters precisely and return a JSON object with the following fields:
            - "name": The guest's full name in classical clear Arabic (matching how it is printed on the ID, but clean). If printed in English only, return the English name.
            - "id_number": The exact identification number digit-by-digit.
            - "id_type": This must be exactly one of: "هوية وطنية" (for Saudi/local National ID), "جواز سفر" (for Passport), or "هوية مقيم" / "إقامة" (for resident card).
            - "nationality": (optional) National origin in Arabic.
            
            Return ONLY the raw JSON object. Do NOT wrap it in ```json ... ``` tags. Do NOT add any introductory text.
        """.trimIndent()

        try {
            // Build Gemini schema body
            val requestJson = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            // Part 1: The Image
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                            // Part 2: The Command Prompt
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                // Request structural JSON constraint
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    onResult(null, null, null, "خطأ في الاتصال بالسيرفر: ${e.localizedMessage}")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use { resp ->
                        val respBody = resp.body?.string() ?: ""
                        if (!resp.isSuccessful) {
                            onResult(null, null, null, "خطأ بالطلب المدخل من السيرفر (${resp.code})")
                            return
                        }

                        try {
                            val responseJson = JSONObject(respBody)
                            val candidates = responseJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val text = parts.getJSONObject(0).optString("text", "").trim()
                                    
                                    // Parse inner structured JSON block
                                    val resultJson = JSONObject(text)
                                    val extractedName = resultJson.optString("name", "")
                                    val extractedId = resultJson.optString("id_number", "")
                                    val extractedType = resultJson.optString("id_type", "هوية وطنية")

                                    onResult(extractedName, extractedId, extractedType, null)
                                    return
                                }
                            }
                            onResult(null, null, null, "لم يتمكن الذكاء الاصطناعي من تحليل الصورة بشكل صحيح")
                        } catch (e: Exception) {
                            onResult(null, null, null, "خطأ في تحليل استجابة السيرفر: ${e.localizedMessage}")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            onResult(null, null, null, "فشل إنشاء الطلب: ${e.localizedMessage}")
        }
    }
}
