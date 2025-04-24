package com.example.financetracker.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.financetracker.models.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "user_preferences", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_USER = "current_user"
        private const val TAG = "UserRepository"
    }

    // Get all users
    fun getUsers(): List<User> {
        val json = sharedPreferences.getString(KEY_USERS, null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing users: ${e.message}")
            emptyList()
        }
    }

    // Save users
    private fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        sharedPreferences.edit().putString(KEY_USERS, json).apply()
    }

    // Register a new user
    fun registerUser(name: String, email: String, password: String): Boolean {
        val users = getUsers().toMutableList()
        
        // Check if email already exists
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return false
        }
        
        // Create new user
        val newUser = User(name = name, email = email, password = password)
        users.add(newUser)
        
        // Save updated user list
        saveUsers(users)
        
        // Log for debugging
        Log.d(TAG, "Registered user: $name, $email")
        
        return true
    }

    // Login user
    fun loginUser(email: String, password: String): User? {
        val user = getUsers().find { 
            it.email.equals(email, ignoreCase = true) && it.password == password 
        }
        
        // Save current user if login successful
        if (user != null) {
            val userJson = gson.toJson(user)
            sharedPreferences.edit().putString(KEY_CURRENT_USER, userJson).apply()
            Log.d(TAG, "User logged in: ${user.name}, ${user.email}")
        } else {
            Log.d(TAG, "Login failed for email: $email")
        }
        
        return user
    }

    // Get current logged in user
    fun getCurrentUser(): User? {
        val json = sharedPreferences.getString(KEY_CURRENT_USER, null)
        
        // Log the raw JSON for debugging
        Log.d(TAG, "Current user JSON: $json")
        
        if (json == null) {
            Log.d(TAG, "No current user found")
            return null
        }
        
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing current user: ${e.message}")
            null
        }
    }

    // Logout user
    fun logoutUser() {
        Log.d(TAG, "User logged out")
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply()
    }

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = getCurrentUser() != null
        Log.d(TAG, "Is user logged in: $isLoggedIn")
        return isLoggedIn
    }

    /**
     * Checks if an email is already taken by another user
     * @param email The email to check
     * @param currentUserId The ID of the current user (to exclude from the check)
     * @return true if the email is taken by another user, false otherwise
     */
    fun isEmailTaken(email: String, currentUserId: String): Boolean {
        return getUsers().any { 
            it.id != currentUserId && 
            it.email.equals(email, ignoreCase = true) 
        }
    }
    
    /**
     * Updates a user's information
     * @param updatedUser The updated user object
     * @return true if the update was successful, false otherwise
     */
    fun updateUser(updatedUser: User): Boolean {
        try {
            val users = getUsers().toMutableList()
            
            // Find the user to update
            val index = users.indexOfFirst { it.id == updatedUser.id }
            if (index == -1) {
                Log.e(TAG, "User not found for update: ${updatedUser.id}")
                return false
            }
            
            // Update the user
            users[index] = updatedUser
            
            // Save all users
            saveUsers(users)
            
            // Update current user if needed
            val currentUser = getCurrentUser()
            if (currentUser?.id == updatedUser.id) {
                val userJson = gson.toJson(updatedUser)
                sharedPreferences.edit().putString(KEY_CURRENT_USER, userJson).apply()
            }
            
            Log.d(TAG, "User updated: ${updatedUser.name}, ${updatedUser.email}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user: ${e.message}", e)
            return false
        }
    }
}
