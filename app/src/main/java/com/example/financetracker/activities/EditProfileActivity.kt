package com.example.financetracker.activities

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.databinding.ActivityEditProfileBinding
import com.example.financetracker.models.User
import com.example.financetracker.utils.UserRepository

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var userRepository: UserRepository
    private var currentUser: User? = null
    private val TAG = "EditProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize user repository
        userRepository = UserRepository(this)
        
        // Load current user
        loadUserData()
        
        // Set up action bar
        supportActionBar?.apply {
            title = "Edit Profile"
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        
        // Set up button listeners
        binding.buttonSaveProfile.setOnClickListener { saveUserProfile() }
        binding.buttonCancel.setOnClickListener { finish() }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun loadUserData() {
        currentUser = userRepository.getCurrentUser()
        
        if (currentUser == null) {
            Log.e(TAG, "No user is logged in")
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Populate UI with user data
        binding.editTextName.setText(currentUser?.name)
        binding.editTextEmail.setText(currentUser?.email)
        
        // Password fields are left empty intentionally
        Log.d(TAG, "Loaded user data: ${currentUser?.name}, ${currentUser?.email}")
    }
    
    private fun saveUserProfile() {
        // Reset errors
        binding.textInputLayoutName.error = null
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutCurrentPassword.error = null
        binding.textInputLayoutNewPassword.error = null
        binding.textInputLayoutConfirmPassword.error = null
        
        // Get input values
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val currentPassword = binding.editTextCurrentPassword.text.toString()
        val newPassword = binding.editTextNewPassword.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()
        
        // Validate inputs
        var hasError = false
        var focusView: View? = null
        
        // Check name
        if (name.isEmpty()) {
            binding.textInputLayoutName.error = "Name is required"
            focusView = binding.editTextName
            hasError = true
        }
        
        // Check email
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Email is required"
            if (focusView == null) focusView = binding.editTextEmail
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputLayoutEmail.error = "Invalid email address"
            if (focusView == null) focusView = binding.editTextEmail
            hasError = true
        }
        
        // Check if user is trying to change password
        var changePassword = false
        if (currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()) {
            changePassword = true
            
            // Validate current password
            if (currentPassword.isEmpty()) {
                binding.textInputLayoutCurrentPassword.error = "Current password is required"
                if (focusView == null) focusView = binding.editTextCurrentPassword
                hasError = true
            } else if (currentUser?.password != currentPassword) {
                binding.textInputLayoutCurrentPassword.error = "Incorrect password"
                if (focusView == null) focusView = binding.editTextCurrentPassword
                hasError = true
            }
            
            // Validate new password
            if (newPassword.isEmpty()) {
                binding.textInputLayoutNewPassword.error = "New password is required"
                if (focusView == null) focusView = binding.editTextNewPassword
                hasError = true
            } else if (newPassword.length < 8) {
                binding.textInputLayoutNewPassword.error = "Password must be at least 8 characters"
                if (focusView == null) focusView = binding.editTextNewPassword
                hasError = true
            }
            
            // Validate confirm password
            if (confirmPassword.isEmpty()) {
                binding.textInputLayoutConfirmPassword.error = "Please confirm your new password"
                if (focusView == null) focusView = binding.editTextConfirmPassword
                hasError = true
            } else if (newPassword != confirmPassword) {
                binding.textInputLayoutConfirmPassword.error = "Passwords do not match"
                if (focusView == null) focusView = binding.editTextConfirmPassword
                hasError = true
            }
        }
        
        if (hasError) {
            focusView?.requestFocus()
            return
        }
        
        // All validations passed, update user
        val updatedPassword = if (changePassword) newPassword else currentUser?.password ?: ""
        updateUserProfile(name, email, updatedPassword)
    }
    
    private fun updateUserProfile(name: String, email: String, password: String) {
        val currentUserId = currentUser?.id ?: return
        
        // Check if email is already taken by another user
        val isEmailTaken = userRepository.isEmailTaken(email, currentUserId)
        if (isEmailTaken) {
            binding.textInputLayoutEmail.error = "Email is already in use"
            binding.editTextEmail.requestFocus()
            return
        }
        
        // Update user
        val updatedUser = User(
            id = currentUserId,
            name = name,
            email = email,
            password = password
        )
        
        val success = userRepository.updateUser(updatedUser)
        if (success) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }
    }
}
