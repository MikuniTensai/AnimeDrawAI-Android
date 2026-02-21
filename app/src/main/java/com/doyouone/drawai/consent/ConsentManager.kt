package com.doyouone.drawai.consent

import android.content.Context
import kotlinx.coroutines.tasks.await
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.ump.FormError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class ConsentResult {
    data class Success(val isFormAvailable: Boolean = false) : ConsentResult()
    data class Error(val message: String) : ConsentResult()
}

sealed class ConsentFormResult {
    object Success : ConsentFormResult()
    data class Error(val message: String) : ConsentFormResult()
    object NotAvailable : ConsentFormResult()
}

class ConsentManager(private val context: Context) {
    
    private var consentInformation: ConsentInformation? = null
    
    suspend fun initializeConsent(isDebugMode: Boolean = false): ConsentResult {
        return try {
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .apply {
                    if (isDebugMode) {
                        // Add debug settings if needed
                    }
                }
                .build()
            
            consentInformation = UserMessagingPlatform.getConsentInformation(context)
            
            suspendCancellableCoroutine { continuation ->
                consentInformation?.requestConsentInfoUpdate(
                    context as? androidx.activity.ComponentActivity ?: return@suspendCancellableCoroutine continuation.resume(ConsentResult.Error("Activity required")),
                    params,
                    {
                        val isFormAvailable = consentInformation?.isConsentFormAvailable ?: false
                        continuation.resume(ConsentResult.Success(isFormAvailable))
                    },
                    { formError: FormError ->
                        continuation.resume(ConsentResult.Error(formError.message ?: "Unknown error"))
                    }
                )
            }
        } catch (e: Exception) {
            ConsentResult.Error(e.message ?: "Failed to initialize consent")
        }
    }
    
    suspend fun showConsentFormIfRequired(): ConsentFormResult {
        val consentInfo = consentInformation ?: return ConsentFormResult.NotAvailable
        
        return try {
            if (consentInfo.isConsentFormAvailable && consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                val result = suspendCancellableCoroutine<ConsentFormResult> { continuation ->
                    UserMessagingPlatform.loadConsentForm(
                        context,
                        { form ->
                            form.show(context as? androidx.activity.ComponentActivity) { formError: FormError? ->
                                if (formError != null) {
                                    continuation.resume(ConsentFormResult.Error(formError.message ?: "Form error"))
                                } else {
                                    continuation.resume(ConsentFormResult.Success)
                                }
                            }
                        },
                        { formError: FormError? ->
                            continuation.resume(ConsentFormResult.Error(formError?.message ?: "Failed to load form"))
                        }
                    )
                }
                result
            } else {
                ConsentFormResult.NotAvailable
            }
        } catch (e: Exception) {
            ConsentFormResult.Error(e.message ?: "Failed to show consent form")
        }
    }
    
    fun canRequestAds(): Boolean {
        val consentInfo = consentInformation ?: return false
        return consentInfo.consentStatus == ConsentInformation.ConsentStatus.OBTAINED || 
               consentInfo.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
    }
    
    fun isConsentObtained(): Boolean {
        val consentInfo = consentInformation ?: return false
        return consentInfo.consentStatus == ConsentInformation.ConsentStatus.OBTAINED
    }
}