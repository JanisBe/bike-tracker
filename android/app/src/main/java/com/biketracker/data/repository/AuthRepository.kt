package com.biketracker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
        val result = auth.signInAnonymously().await()
        result.user ?: throw Exception("Authentication failed: user is null")
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
        result.user ?: throw Exception("Sign in failed")
    }

    override suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
        result.user ?: throw Exception("Sign up failed")
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }
}
