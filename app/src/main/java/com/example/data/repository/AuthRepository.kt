package com.example.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

data class UserProfile(
    val uid: String = "",
    val displayName: String? = "",
    val email: String? = "",
    val phoneNumber: String? = "",
    val photoUrl: String? = ""
)

class AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Observe active auth state as a Kotlin Flow
    val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw Exception("Google Sign-In returned null user credentials")
        saveUserProfile(user)
        return user
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): FirebaseUser {
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw Exception("Phone OTP Sign-In returned null user credentials")
        saveUserProfile(user)
        return user
    }

    suspend fun saveUserProfile(user: FirebaseUser) {
        val profile = UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: "SearchOps Executive",
            email = user.email ?: "",
            phoneNumber = user.phoneNumber ?: "",
            photoUrl = user.photoUrl?.toString() ?: ""
        )
        try {
            firestore.collection("users")
                .document(user.uid)
                .set(profile)
                .await()
            Log.d("AuthRepository", "User profile successfully saved to Firestore: ${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error storing profile in Firestore: ${e.localizedMessage}")
            // Profile saving failed but authentication succeeded. We don't crash.
        }
    }

    fun logout() {
        auth.signOut()
    }

    // Phone OTP Verification Callbacks wrapper
    fun startPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
