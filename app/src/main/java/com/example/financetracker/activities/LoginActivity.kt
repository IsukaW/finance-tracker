package com.example.financetracker.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.databinding.ActivityLoginBinding
import com.example.financetracker.utils.UserRepository

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(this)

        // Check if user is already logged in
        if (userRepository.isUserLoggedIn()) {
            Log.d(TAG, "User already logged in, navigating to MainActivity")
            navigateToMainActivity()
            return
        }

        // Add a test user if none exists (for testing purposes)
        if (userRepository.getUsers().isEmpty()) {
            Log.d(TAG, "Creating test user")
            userRepository.registerUser("John Doe", "john.doe@example.com", "password123")
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Login button
        binding.buttonLogin.setOnClickListener {
            attemptLogin()
        }

        // Register text
        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot password
        binding.textViewForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "Please contact admin to reset your password",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun attemptLogin() {
        // Reset errors
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null

        // Get input values
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        // Validate inputs
        var focusView: View? = null
        var cancel = false

        // Check for a valid password
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Password is required"
            focusView = binding.editTextPassword
            cancel = true
        }

        // Check for a valid email address
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Email is required"
            focusView = binding.editTextEmail
            cancel = true
        }

        if (cancel) {
            // There was an error; focus the first form field with an error
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and perform the login attempt
            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        val user = userRepository.loginUser(email, password)
        
        if (user != null) {
            Log.d(TAG, "Login successful for: ${user.name}, ${user.email}")
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            navigateToMainActivity()
        } else {
            Log.d(TAG, "Login failed for email: $email")
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Clear the back stack so the user can't go back to login screen
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
