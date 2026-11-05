package com.example.weatherapp.utils

import android.util.Log
import com.example.weatherapp.UserSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object SettingsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun userDoc() = auth.currentUser?.uid
        ?.let { db.collection("users").document(it) }

    suspend fun save(settings: UserSettings): Boolean {
        val doc = userDoc() ?: return false
        return try {
            doc.set(settings, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Settings save failed", e)
            false
        }
    }

    suspend fun load(): UserSettings? {
        val doc = userDoc() ?: return null
        return try {
            val snap = doc.get().await()

            snap.toObject(UserSettings::class.java)

        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "Settings load failed", e)
            null
        }
    }
}