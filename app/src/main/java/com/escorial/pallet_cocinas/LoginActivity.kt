package com.escorial.pallet_cocinas

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.widget.Toast
import com.escorial.pallet_cocinas.databinding.ActivityLoginBinding
import com.escorial.pallet_cocinas.di.ViewModelFactory
import com.escorial.pallet_cocinas.di.appContainer
import com.escorial.pallet_cocinas.viewmodel.LoginNavEvent
import com.escorial.pallet_cocinas.viewmodel.LoginUiState
import com.escorial.pallet_cocinas.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory { LoginViewModel(appContainer.loginRepository, appContainer.sessionRepository) }
        )[LoginViewModel::class.java]

        val initialNavEvent = viewModel.initialNavEvent
        if (initialNavEvent != null) {
            navigate(initialNavEvent)
            return
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login(binding.etUsername.text.toString(), binding.etPassword.text.toString())
        }

        binding.btnConfiguracion.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.navEvents.collect { navigate(it) } }
                launch { viewModel.errorEvents.collect { Toast.makeText(this@LoginActivity, it, Toast.LENGTH_LONG).show() } }
            }
        }
    }

    private fun render(state: LoginUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
    }

    private fun navigate(event: LoginNavEvent) {
        val bypass = when (event) {
            is LoginNavEvent.ToMain -> event.bypass
        }
        val intent = if (bypass)
            Intent(this, ExpedicionActivity::class.java)
        else
            Intent(this, AsociarProductoActivity::class.java)
        startActivity(intent)
        finish()
    }
}
