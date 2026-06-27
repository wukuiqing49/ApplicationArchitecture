package com.wkq.google.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.wkq.google.model.GoogleAccountPayload
import com.wkq.google.model.GoogleIdTokenClaims
import java.nio.charset.StandardCharsets
import java.util.UUID

object GoogleLoginType {
    const val GOOGLE = 1001
}

private const val GOOGLE_API_CONSOLE_CONFIG_ERROR =
    "Google sign-in is not correctly registered in Google Cloud/Firebase. Check the package name, signing certificate SHA-1/SHA-256, Android OAuth client, and Web Client ID."

object GoogleSignInCoordinator {

    private val gson = Gson()

    suspend fun signIn(
        activity: Activity,
        serverClientId: String
    ): Result<GoogleAccountPayload> {
        return runCatching {
            require(serverClientId.isNotBlank()) { "Google Server Client ID is empty." }
            val credentialManager = CredentialManager.create(activity)
            val googleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .setNonce(UUID.randomUUID().toString())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )
            val credential = result.credential
            require(credential is CustomCredential) { "Unsupported credential result." }
            require(
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) { "Unsupported Google credential type." }

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val tokenClaims = parseIdTokenClaims(googleCredential.idToken)
            val email = tokenClaims.email
                ?: googleCredential.id
                ?: throw IllegalStateException("Google account email is unavailable.")
            val subject = tokenClaims.subject
            val displayName = tokenClaims.name
                ?: googleCredential.displayName
                ?: email.substringBefore("@")
            val avatarUrl = tokenClaims.picture
                ?: googleCredential.profilePictureUri?.toString()
                .orEmpty()
            val userId = subject?.takeIf { it.isNotBlank() }?.let { "google_$it" }
                ?: "google_$email"

            GoogleAccountPayload(
                userId = userId,
                email = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                idToken = googleCredential.idToken,
                subject = subject
            )
        }.mapErrorMessage()
    }

    suspend fun signOut(context: Context) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    fun buildExtraJson(payload: GoogleAccountPayload, subscriptionStatus: String): String {
        val json = JsonObject()
        json.addProperty("provider", "google")
        json.addProperty("email", payload.email)
        json.addProperty("subject", payload.subject)
        json.addProperty("avatarUrl", payload.avatarUrl)
        json.addProperty("subscriptionStatus", subscriptionStatus)
        return gson.toJson(json)
    }

    private fun parseIdTokenClaims(idToken: String): GoogleIdTokenClaims {
        val segments = idToken.split(".")
        if (segments.size < 2) {
            return GoogleIdTokenClaims()
        }
        return runCatching {
            val payloadBytes = Base64.decode(
                segments[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            val payloadJson = String(payloadBytes, StandardCharsets.UTF_8)
            gson.fromJson(payloadJson, GoogleIdTokenClaims::class.java) ?: GoogleIdTokenClaims()
        }.getOrDefault(GoogleIdTokenClaims())
    }
}

private fun Result<GoogleAccountPayload>.mapErrorMessage(): Result<GoogleAccountPayload> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { throwable ->
            Result.failure(IllegalStateException(throwable.toReadableMessage(), throwable))
        }
    )
}

private fun Throwable.toReadableMessage(): String {
    if (hasGoogleApiConsoleConfigError()) {
        return GOOGLE_API_CONSOLE_CONFIG_ERROR
    }
    return when (this) {
        is IllegalArgumentException -> message.orEmpty().ifBlank { "Google sign-in parameters are invalid." }
        is NoCredentialException -> "No Google account is available on this device. Sign in to a Google account in system settings and try again."
        is GetCredentialCancellationException -> "Google sign-in was canceled, or the account chooser could not be opened."
        is GetCredentialInterruptedException -> "Google sign-in was interrupted. Please try again later."
        is GetCredentialProviderConfigurationException -> {
            "The credential provider is not ready. Update Google Play services and try again."
        }
        is GetCredentialUnsupportedException -> {
            "This device does not support Google Credential Manager sign-in."
        }
        is GetCredentialCustomException -> {
            message.orEmpty().ifBlank { GOOGLE_API_CONSOLE_CONFIG_ERROR }
        }
        is GetCredentialUnknownException -> {
            message.orEmpty().ifBlank { GOOGLE_API_CONSOLE_CONFIG_ERROR }
        }
        is GetCredentialException -> {
            message.orEmpty().ifBlank { "Google sign-in failed. Please try again later." }
        }
        is GoogleIdTokenParsingException -> "Could not parse the identity token returned by Google. Please try again later."
        else -> message.orEmpty().ifBlank { "Google sign-in is temporarily unavailable. Please try again later." }
    }
}

private fun Throwable.hasGoogleApiConsoleConfigError(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val text = "${current::class.java.name} ${current.message.orEmpty()}"
        if (
            text.contains("UNREGISTERED_ON_API_CONSOLE", ignoreCase = true) ||
            text.contains("API_CONSOLE", ignoreCase = true) ||
            text.contains("Account reauth failed", ignoreCase = true)
        ) {
            return true
        }
        current = current.cause
    }
    return false
}
