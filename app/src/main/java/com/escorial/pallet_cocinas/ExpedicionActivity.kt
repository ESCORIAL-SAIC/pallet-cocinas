package com.escorial.pallet_cocinas

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.escorial.pallet_cocinas.databinding.ActivityExpedicionBinding
import com.escorial.pallet_cocinas.di.ViewModelFactory
import com.escorial.pallet_cocinas.di.appContainer
import com.escorial.pallet_cocinas.viewmodel.ExpedicionViewModel

class ExpedicionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpedicionBinding
    private lateinit var viewModel: ExpedicionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityExpedicionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory { ExpedicionViewModel(appContainer.sessionRepository) }
        )[ExpedicionViewModel::class.java]

        loadControls()
    }

    private fun loadControls() {
        val state = viewModel.uiState.value

        binding.topBar.setUserInfo(state.username, state.fullName)
        binding.topBar.setLogoutButtonVisibility(true)
        binding.topBar.setOnLogoutClickListener {
            logout()
            Toast.makeText(this@ExpedicionActivity, "Logout", Toast.LENGTH_SHORT).show()
        }
        binding.btnDesasociar.setOnClickListener { startPickeoPalletActivity("desasociar") }
        binding.btnTransferir.setOnClickListener { startPickeoPalletActivity("transferir") }
    }

    private fun logout() {
        viewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startPickeoPalletActivity(tipo: String) {
        val intent = Intent(this, PickeoPalletActivity::class.java)
        intent.putExtra("tipo", tipo)
        startActivity(intent)
    }
}
