package com.example.financetracker.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.financetracker.R
import com.example.financetracker.databinding.ActivityRegisterBinding
import com.example.financetracker.utils.UserRepository

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Register button
        binding.buttonRegister.setOnClickListener {
            attemptRegister()
        }

        // Login text
        binding.textViewLogin.setOnClickListener {
            finish() // Go back to login screen
        }
    }

    private fun attemptRegister() {
        // Reset errors
        binding.textInputLayoutName.error = null
        binding.textInputLayoutEmail.error = null
        binding.textInputLayoutPassword.error = null
        binding.textInputLayoutConfirmPassword.error = null

        // Get input values
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()

        // Validate inputs
        var focusView: View? = null
        var cancel = false

        // Check for a valid name
        if (name.isEmpty()) {
            binding.textInputLayoutName.error = "Name is required"
            focusView = binding.editTextName
            cancel = true
        }

        // Check for a valid email address
        if (email.isEmpty()) {
            binding.textInputLayoutEmail.error = "Email is required"
            focusView = binding.editTextEmail
            cancel = true
        } else if (!isEmailValid(email)) {
            binding.textInputLayoutEmail.error = "Invalid email address"
            focusView = binding.editTextEmail
            cancel = true
        }

        // Check for a valid password
        if (password.isEmpty()) {
            binding.textInputLayoutPassword.error = "Password is required"
            focusView = binding.editTextPassword
            cancel = true
        } else if (!isPasswordValid(password)) {
            binding.textInputLayoutPassword.error = "Password must be at least 8 characters"
            focusView = binding.editTextPassword
            cancel = true
        }

        // Check if passwords match
        if (confirmPassword.isEmpty()) {
            binding.textInputLayoutConfirmPassword.error = "Confirm your password"
            focusView = binding.editTextConfirmPassword
            cancel = true
        } else if (password != confirmPassword) {
            binding.textInputLayoutConfirmPassword.error = "Passwords don't match"
            focusView = binding.editTextConfirmPassword
            cancel = true
        }

        if (cancel) {
            // There was an error; focus the first form field with an error
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and perform the registration attempt
            performRegistration(name, email, password)
        }
    }

    private fun performRegistration(name: String, email: String, password: String) {
        val success = userRepository.registerUser(name, email, password)

        if (success) {
            Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
            finish() // Go back to login screen
        } else {
            binding.textInputLayoutEmail.error = "Email is already registered"
            binding.editTextEmail.requestFocus()
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8
    }
}
