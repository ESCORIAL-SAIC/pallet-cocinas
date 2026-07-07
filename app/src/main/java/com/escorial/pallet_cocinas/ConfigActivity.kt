package com.escorial.pallet_cocinas

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.escorial.pallet_cocinas.databinding.ActivityConfigBinding
import com.escorial.pallet_cocinas.di.ViewModelFactory
import com.escorial.pallet_cocinas.di.appContainer
import com.escorial.pallet_cocinas.viewmodel.ConfigViewModel

class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private lateinit var viewModel: ConfigViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory { ConfigViewModel(appContainer.configRepository) }
        )[ConfigViewModel::class.java]

        binding.layoutConfig.visibility = View.GONE

        binding.btnValidar.setOnClickListener {
            if (viewModel.validatePassword(binding.etPassword.text.toString())) {
                binding.layoutPassword.visibility = View.GONE
                binding.layoutConfig.visibility = View.VISIBLE
                binding.etApiUrl.setText(viewModel.uiState.value.apiUrl)
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGuardar.setOnClickListener {
            viewModel.saveApiUrl(binding.etApiUrl.text.toString())
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnCancelar.setOnClickListener {
            finish()
        }
    }
}
