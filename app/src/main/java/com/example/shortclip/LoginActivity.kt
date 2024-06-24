package com.example.shortclip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.example.shortclip.databinding.ActivityLoginBinding
import com.example.shortclip.util.UiUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseAuth.getInstance().currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.submitButton.setOnClickListener {
            login()
        }

        binding.goToSignupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.submitButton.visibility = View.GONE
        }
        else {
            binding.progressBar.visibility = View.GONE
            binding.submitButton.visibility = View.VISIBLE
        }
    }

    private fun login() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (! Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError("Email not valid")
            return
        }
        if (password.length < 6) {
            binding.passwordInput.setError("Minimum 6 character")
            return
        }

        loginWithFirebase(email, password)
    }

    private fun loginWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
            email, password
        ).addOnSuccessListener {
            UiUtil.showToast(applicationContext, "Login successfully")
            setInProgress(false)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }.addOnFailureListener {exception ->
//            UiUtil.showToast(this, it.localizedMessage?: "Something went wrong")
            val errorMessage = when (exception) {
                is FirebaseAuthInvalidUserException -> "Invalid email address"
                is FirebaseAuthInvalidCredentialsException -> "Invalid password"
                else -> "Login failed: ${exception.message}"
            }
            UiUtil.showToast(applicationContext, errorMessage)
            setInProgress(false)
        }
    }
}