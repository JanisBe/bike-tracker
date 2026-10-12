package com.biketracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.biketracker.data.model.ActivityType
import com.biketracker.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun getUserProfileFlow(): Flow<UserProfile>
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit>
    fun getSpeedThreshold(activityType: String): Double
    fun getUserWeightKg(): Double
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getUserProfileFlow(): Flow<UserProfile> = callbackFlow {
        val user = auth.currentUser
        if (user == null) {
            trySend(loadFromPrefs())
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("users").document(user.uid)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(loadFromPrefs())
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val profile = UserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    gender = snapshot.getString("gender") ?: "male",
                    weightKg = snapshot.getDouble("weightKg") ?: 75.0,
                    heightCm = snapshot.getDouble("heightCm") ?: 175.0,
                    cyclingSpeedThresholdKmh = snapshot.getDouble("cyclingSpeedThresholdKmh") ?: 1.5,
                    walkingSpeedThresholdKmh = snapshot.getDouble("walkingSpeedThresholdKmh") ?: 0.5,
                    updatedAt = snapshot.getTimestamp("updatedAt")?.toDate() ?: Date()
                )
                saveToPrefs(profile)
                trySend(profile)
            } else {
                val defaultProfile = loadFromPrefs().copy(
                    userId = user.uid,
                    email = user.email ?: ""
                )
                trySend(defaultProfile)
            }
        }

        awaitClose { registration.remove() }
    }

    override suspend fun getUserProfile(): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: return@runCatching loadFromPrefs()
            val doc = firestore.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                val profile = UserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    gender = doc.getString("gender") ?: "male",
                    weightKg = doc.getDouble("weightKg") ?: 75.0,
                    heightCm = doc.getDouble("heightCm") ?: 175.0,
                    cyclingSpeedThresholdKmh = doc.getDouble("cyclingSpeedThresholdKmh") ?: 1.5,
                    walkingSpeedThresholdKmh = doc.getDouble("walkingSpeedThresholdKmh") ?: 0.5,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate() ?: Date()
                )
                saveToPrefs(profile)
                profile
            } else {
                loadFromPrefs().copy(
                    userId = user.uid,
                    email = user.email ?: ""
                )
            }
        }
    }

    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val user = auth.currentUser ?: throw IllegalStateException("User not authenticated")
            val profileWithUser = profile.copy(
                userId = user.uid,
                email = user.email ?: profile.email,
                updatedAt = Date()
            )

            // 1. Save locally for instantaneous offline access by tracking service
            saveToPrefs(profileWithUser)

            // 2. Save to Firestore
            val data = mapOf(
                "userId" to profileWithUser.userId,
                "email" to profileWithUser.email,
                "gender" to profileWithUser.gender,
                "weightKg" to profileWithUser.weightKg,
                "heightCm" to profileWithUser.heightCm,
                "cyclingSpeedThresholdKmh" to profileWithUser.cyclingSpeedThresholdKmh,
                "walkingSpeedThresholdKmh" to profileWithUser.walkingSpeedThresholdKmh,
                "updatedAt" to Timestamp(profileWithUser.updatedAt)
            )

            firestore.collection("users")
                .document(user.uid)
                .set(data, SetOptions.merge())
                .await()
            Unit
        }
    }

    override fun getSpeedThreshold(activityType: String): Double {
        return if (activityType.equals(ActivityType.WALKING.id, ignoreCase = true)) {
            prefs.getFloat(KEY_WALKING_THRESHOLD, 0.5f).toDouble()
        } else {
            prefs.getFloat(KEY_CYCLING_THRESHOLD, 1.5f).toDouble()
        }
    }

    override fun getUserWeightKg(): Double {
        return prefs.getFloat(KEY_WEIGHT, 75.0f).toDouble()
    }

    private fun loadFromPrefs(): UserProfile {
        val user = auth.currentUser
        return UserProfile(
            userId = user?.uid ?: "",
            email = user?.email ?: "",
            gender = prefs.getString(KEY_GENDER, "male") ?: "male",
            weightKg = prefs.getFloat(KEY_WEIGHT, 75.0f).toDouble(),
            heightCm = prefs.getFloat(KEY_HEIGHT, 175.0f).toDouble(),
            cyclingSpeedThresholdKmh = prefs.getFloat(KEY_CYCLING_THRESHOLD, 1.5f).toDouble(),
            walkingSpeedThresholdKmh = prefs.getFloat(KEY_WALKING_THRESHOLD, 0.5f).toDouble(),
            updatedAt = Date(prefs.getLong(KEY_UPDATED_AT, System.currentTimeMillis()))
        )
    }

    private fun saveToPrefs(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_GENDER, profile.gender)
            .putFloat(KEY_WEIGHT, profile.weightKg.toFloat())
            .putFloat(KEY_HEIGHT, profile.heightCm.toFloat())
            .putFloat(KEY_CYCLING_THRESHOLD, profile.cyclingSpeedThresholdKmh.toFloat())
            .putFloat(KEY_WALKING_THRESHOLD, profile.walkingSpeedThresholdKmh.toFloat())
            .putLong(KEY_UPDATED_AT, profile.updatedAt.time)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "bike_tracker_user_profile"
        private const val KEY_GENDER = "pref_gender"
        private const val KEY_WEIGHT = "pref_weight"
        private const val KEY_HEIGHT = "pref_height"
        private const val KEY_CYCLING_THRESHOLD = "pref_cycling_threshold"
        private const val KEY_WALKING_THRESHOLD = "pref_walking_threshold"
        private const val KEY_UPDATED_AT = "pref_updated_at"
    }
}
