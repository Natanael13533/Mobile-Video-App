package com.example.shortclip

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.example.shortclip.databinding.ActivitySignupBinding
import com.example.shortclip.model.UserModel
import com.example.shortclip.util.UiUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submitButton.setOnClickListener {
            signup()
        }

        binding.goToLoginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
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

    private fun signup() {
        val email = binding.emailInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError("Email not valid")
            return
        }
        if (password.length < 6) {
            binding.passwordInput.setError("Minimum 6 characters")
            return
        }
        if (password != confirmPassword || confirmPassword.isEmpty()) {
            binding.confirmPasswordInput.setError("Password not matched")
            return
        }

        signupWithFirebase(email, password)
    }

    private fun signupWithFirebase(email: String, password: String) {
        setInProgress(true)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(
            email,password
        ).addOnSuccessListener {
            it.user?.let { user->
                val userModel = UserModel(
                    user.uid,
                    email,
                    email.substringBefore("@")
                )
                Firebase.firestore.collection("users")
                    .document(user.uid)
                    .set(userModel)
                    .addOnSuccessListener {
                        UiUtil.showToast(applicationContext, "Account created succesfully")
                        setInProgress(false)
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                    }
            }
        }.addOnFailureListener {
            UiUtil.showToast(applicationContext, it.localizedMessage?: "Something went wrong")
            setInProgress(false)
        }
    }
}