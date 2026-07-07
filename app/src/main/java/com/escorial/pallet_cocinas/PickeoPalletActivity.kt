package com.escorial.pallet_cocinas

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.escorial.pallet_cocinas.databinding.ActivityPickeoPalletBinding
import com.escorial.pallet_cocinas.di.ViewModelFactory
import com.escorial.pallet_cocinas.di.appContainer
import com.escorial.pallet_cocinas.viewmodel.PickeoPalletEvent
import com.escorial.pallet_cocinas.viewmodel.PickeoPalletUiState
import com.escorial.pallet_cocinas.viewmodel.PickeoPalletViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PickeoPalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickeoPalletBinding
    private lateinit var viewModel: PickeoPalletViewModel
    private lateinit var palletAdapter: PalletAdapter

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {
                swipeActionDelete(position)
            }
        }
    }

    private fun swipeActionDelete(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Seguro que quieres desasociar este elemento?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.eliminarPallet(position)
                palletAdapter.notifyItemRemoved(position)
                Snackbar.make(binding.rvPallets, "Ítem eliminado", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        val restoredPosition = viewModel.restaurarUltimoEliminado()
                        if (restoredPosition != null) palletAdapter.notifyItemInserted(restoredPosition)
                    }
                    .show()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                palletAdapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPickeoPalletBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tipo = intent.getStringExtra("tipo") ?: ""
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory { PickeoPalletViewModel(appContainer.expedicionRepository, appContainer.sessionRepository, tipo) }
        )[PickeoPalletViewModel::class.java]

        loadControls(tipo)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
                launch { viewModel.errorEvents.collect { Toast.makeText(this@PickeoPalletActivity, it, Toast.LENGTH_LONG).show() } }
            }
        }
    }

    private fun loadControls(tipo: String) {
        val state = viewModel.uiState.value

        binding.topBar.setUserInfo(state.username, state.fullName)
        binding.topBar.setLogoutButtonVisibility(true)
        binding.topBar.setOnLogoutClickListener {
            logout()
            Toast.makeText(this@PickeoPalletActivity, "Logout", Toast.LENGTH_SHORT).show()
        }

        binding.lblTitle.text = binding.lblTitle.text.toString()
            .replace("{type}", if (tipo == "transferir") "Transferencia" else "Desasociación")

        binding.btnTransferir.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmar ${if (tipo == "transferir") "transferencia" else "desasociación"}?")
                .setMessage("¿Seguro que quieres ${if (tipo == "transferir") "transferir" else "desasociar"} este elemento?")
                .setPositiveButton("Sí") { _, _ -> viewModel.confirmarAccion() }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        if (tipo == "desasociar") {
            binding.btnTransferir.text = "Desasociar"
        }

        binding.etPallet.setOnEditorActionListener(createEnterListener())
        binding.rvPallets.layoutManager = LinearLayoutManager(this)
        palletAdapter = PalletAdapter(viewModel.palletsList)
        binding.rvPallets.adapter = palletAdapter
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvPallets)
    }

    private fun render(state: PickeoPalletUiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: PickeoPalletEvent) {
        when (event) {
            is PickeoPalletEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            is PickeoPalletEvent.ListChanged -> {
                palletAdapter.notifyDataSetChanged()
                binding.etPallet.text.clear()
                binding.etPallet.requestFocus()
            }
        }
    }

    private fun logout() {
        viewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createEnterListener(): TextView.OnEditorActionListener {
        return TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == 5 ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                viewModel.buscarPallet(binding.etPallet.text.toString())
                v.clearFocus()
                val imm = getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        }
    }
}
