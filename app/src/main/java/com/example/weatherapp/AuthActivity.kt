package com.example.weatherapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weatherapp.utils.AuthRepository
import com.example.weatherapp.utils.Constants
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.result.contract.ActivityResultContracts

class AuthActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private var isLoginMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        authRepository = AuthRepository()

        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val passwordInput = findViewById<EditText>(R.id.passwordEditText)
        val registerBtn = findViewById<Button>(R.id.registerBtn)
        val tvSwitchMode = findViewById<TextView>(R.id.tvSwitchMode)

        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleSignIn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                registerBtn.isEnabled = false
                registerBtn.text = "Please wait..."

                lifecycleScope.launch {
                    val result = if (isLoginMode) {
                        Log.d(Constants.LOG_TAG, "Attempting LOGIN for: $email")
                        authRepository.login(email, password)
                    } else {
                        Log.d(Constants.LOG_TAG, "Attempting REGISTER for: $email")
                        authRepository.register(email, password)
                    }

                    result.onSuccess {
                        Log.i(Constants.LOG_TAG, "Auth successful.")
                        Toast.makeText(this@AuthActivity, "Success!", Toast.LENGTH_SHORT).show()
                        goToMain()
                    }.onFailure { error ->
                        Log.e(Constants.LOG_TAG, "Auth failed", error)
                        Toast.makeText(this@AuthActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()

                        registerBtn.isEnabled = true
                        registerBtn.text = if (isLoginMode) "Log In" else "Create Account"
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                registerBtn.text = "Log In"
                tvSwitchMode.text = "Don't have an account? Create one"
            } else {
                registerBtn.text = "Create Account"
                tvSwitchMode.text = "Already have an account? Log in"
            }
        }


    }

    private fun goToMain() {
        val intent = Intent(this@AuthActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                lifecycleScope.launch {
                    val authResult = authRepository.signInWithGoogle(idToken)
                    authResult.onSuccess {
                        Toast.makeText(this@AuthActivity, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                        goToMain()
                    }.onFailure {
                        Toast.makeText(this@AuthActivity, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
