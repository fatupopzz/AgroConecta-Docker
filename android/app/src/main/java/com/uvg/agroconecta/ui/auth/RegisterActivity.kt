package com.uvg.agroconecta.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.uvg.agroconecta.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val nombre = binding.etNombre.text?.toString()?.trim() ?: ""
            val telefono = binding.etTelefono.text?.toString()?.trim() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""

            if (nombre.isEmpty() || telefono.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.register(nombre, telefono, email, password, this)
        }

        viewModel.registerState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressRegister.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }
                is AuthState.Success -> {
                    Toast.makeText(this, "Cuenta creada. Inicia sesión.", Toast.LENGTH_LONG).show()
                    finish()
                }
                is AuthState.Error -> {
                    binding.progressRegister.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    binding.progressRegister.visibility = View.GONE
                    binding.btnRegister.isEnabled = true
                }
            }
        }
    }
}
