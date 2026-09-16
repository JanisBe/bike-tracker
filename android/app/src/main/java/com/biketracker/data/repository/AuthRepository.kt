package com.biketracker.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val currentUser: FirebaseUser?
    val authState: Flow<FirebaseUser?>
    suspend fun signInAnonymously(): Result<FirebaseUser>
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser>
    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser>
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun signOut(): Result<Unit>
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInAnonymously(): Result<FirebaseUser> = runCatching {
        val existing = auth.currentUser
        if (existing != null && existing.isAnonymous) {
            return@runCatching existing
        }
        val result = auth.signInAnonymously().await()
        result.user ?: throw Exception("Authentication failed: user is null")
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
        result.user ?: throw Exception("Sign in failed")
    }

    override suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> = runCatching {
        val existing = auth.currentUser
        if (existing != null && existing.isAnonymous) {
            val credential = EmailAuthProvider.getCredential(email.trim(), pass)
            val result = existing.linkWithCredential(credential).await()
            result.user ?: throw Exception("Account linking failed: user is null")
        } else {
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            result.user ?: throw Exception("Sign up failed")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val existing = auth.currentUser
        if (existing != null && existing.isAnonymous) {
            val result = existing.linkWithCredential(credential).await()
            result.user ?: throw Exception("Google linking failed: user is null")
        } else {
            val result = auth.signInWithCredential(credential).await()
            result.user ?: throw Exception("Google sign in failed: user is null")
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }
}
