package com.escorial.pallet_cocinas

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.escorial.pallet_cocinas.data.model.Product
import com.escorial.pallet_cocinas.databinding.ActivityAsociarProductoBinding
import com.escorial.pallet_cocinas.di.ViewModelFactory
import com.escorial.pallet_cocinas.di.appContainer
import com.escorial.pallet_cocinas.viewmodel.AsociarProductoEvent
import com.escorial.pallet_cocinas.viewmodel.AsociarProductoUiState
import com.escorial.pallet_cocinas.viewmodel.AsociarProductoViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AsociarProductoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsociarProductoBinding
    private lateinit var viewModel: AsociarProductoViewModel
    private lateinit var productAdapter: ProductAdapter

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val item = viewModel.productsList[position]

            if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {
                if (!item.deleted)
                    swipeActionDelete(item)
                else
                    swipeActionRestore(item)
            }
        }
    }

    private fun swipeActionRestore(item: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar restauración")
            .setMessage("Seguro que quieres volver a asociar este elemento?")
            .setPositiveButton("Sí") { _, _ ->
                val restored = viewModel.restoreProduct(item)
                val position = viewModel.productsList.indexOf(item)
                productAdapter.notifyItemChanged(position)
                if (restored) {
                    Snackbar.make(binding.productsRecyclerView, "Ítem restaurado", Snackbar.LENGTH_LONG)
                        .setAction("Deshacer") {
                            viewModel.deleteProduct(item)
                            productAdapter.notifyItemChanged(position)
                        }
                        .show()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                productAdapter.notifyItemChanged(viewModel.productsList.indexOf(item))
            }
            .setCancelable(false)
            .show()
    }

    private fun swipeActionDelete(item: Product) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("Seguro que quieres desasociar este elemento?")
            .setPositiveButton("Sí") { _, _ ->
                viewModel.deleteProduct(item)
                val position = viewModel.productsList.indexOf(item)
                productAdapter.notifyItemChanged(position)
                Snackbar.make(binding.productsRecyclerView, "Ítem eliminado", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        viewModel.restoreProduct(item)
                        productAdapter.notifyItemChanged(position)
                    }
                    .show()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                productAdapter.notifyItemChanged(viewModel.productsList.indexOf(item))
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAsociarProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory {
                AsociarProductoViewModel(
                    appContainer.palletRepository,
                    appContainer.productRepository,
                    appContainer.sessionRepository
                )
            }
        )[AsociarProductoViewModel::class.java]

        loadControls()

        val state = viewModel.uiState.value
        binding.topBar.setUserInfo(state.username, state.fullName)
        binding.topBar.setLogoutButtonVisibility(true)
        binding.topBar.setOnLogoutClickListener {
            logout()
            Toast.makeText(this@AsociarProductoActivity, "Logout", Toast.LENGTH_SHORT).show()
        }

        binding.productEditText.setOnEditorActionListener(createEnterListener("product"))
        binding.palletEditText.setOnEditorActionListener(createEnterListener("pallet"))

        binding.submitButton.setOnClickListener {
            viewModel.onSubmit(binding.palletEditText.text.toString())
        }

        binding.changePalletButton.setOnClickListener { viewModel.onChangePalletClicked() }

        binding.palletEditText.requestFocus()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.events.collect { handleEvent(it) } }
                launch { viewModel.errorEvents.collect { Toast.makeText(this@AsociarProductoActivity, it, Toast.LENGTH_LONG).show() } }
            }
        }
    }

    private fun loadControls() {
        binding.palletEditText.setText(viewModel.initialPalletText)
        binding.productEditText.setText(viewModel.initialProductText)

        binding.productsRecyclerView.layoutManager = LinearLayoutManager(this)

        productAdapter = ProductAdapter(viewModel.productsList)
        binding.productsRecyclerView.adapter = productAdapter
        productAdapter.notifyDataSetChanged()

        productAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                actualizarContadorPickeados()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                actualizarContadorPickeados()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                actualizarContadorPickeados()
            }

            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                actualizarContadorPickeados()
            }
        })

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.productsRecyclerView)

        configProductSpinner()
    }

    private fun configProductSpinner() {
        val options = resources.getStringArray(R.array.tipo_producto)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(R.layout.product_dropdown_item)
        binding.productSpinner.adapter = adapter
        binding.productSpinner.setSelection(viewModel.uiState.value.spinnerSelection)

        binding.productSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                viewModel.onProductTypeSelected(position, selectedOption)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                viewModel.onProductTypeNothingSelected()
            }
        }
    }

    private fun render(state: AsociarProductoUiState) {
        binding.palletEditText.isEnabled = state.palletFieldEnabled
        binding.productEditText.isEnabled = state.productFieldEnabled
        binding.productSpinner.isEnabled = state.productSpinnerEnabled
        if (binding.productSpinner.selectedItemPosition != state.spinnerSelection) {
            binding.productSpinner.setSelection(state.spinnerSelection)
        }
        binding.progressBar.visibility =
            if (state.isLoadingProduct || state.isLoadingPallet || state.isSubmitting) View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: AsociarProductoEvent) {
        when (event) {
            is AsociarProductoEvent.ShowToast ->
                Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
            is AsociarProductoEvent.FocusProductField -> {
                if (event.clearFirst) binding.productEditText.text.clear()
                binding.productEditText.requestFocus()
            }
            is AsociarProductoEvent.ProductsListChanged ->
                productAdapter.notifyDataSetChanged()
            is AsociarProductoEvent.ResetFields -> {
                productAdapter.notifyDataSetChanged()
                binding.palletEditText.text.clear()
                binding.productEditText.text.clear()
                binding.palletEditText.requestFocus()
            }
        }
    }

    private fun actualizarContadorPickeados() {
        val notDeleted = viewModel.productsList.filter { !it.deleted }
        val count = notDeleted.size

        if (viewModel.productsList.isNotEmpty()) {
            val max = viewModel.productsList.first().maxCantByPallet
            binding.pickeadosTextView.text = "Pickeados: $count/$max"

            if (count == max) {
                binding.pickeadosTextView.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                binding.pickeadosTextView.setTextColor(getColor(android.R.color.black))
            }
        } else {
            binding.pickeadosTextView.text = "Pickeados: $count"
            binding.pickeadosTextView.setTextColor(getColor(android.R.color.black))
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause(binding.palletEditText.text.toString(), binding.productEditText.text.toString())
    }

    private fun logout() {
        viewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createEnterListener(type: String): TextView.OnEditorActionListener {
        return TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == 5 ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                when (type) {
                    "product" -> viewModel.onProductSerialSubmitted(binding.productEditText.text.toString())
                    "pallet" -> viewModel.onPalletCodeSubmitted(binding.palletEditText.text.toString())
                }
                v.clearFocus()
                val imm = getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        }
    }
}
