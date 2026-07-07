package com.escorial.pallet_cocinas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.escorial.pallet_cocinas.data.model.Pallet
import com.escorial.pallet_cocinas.databinding.PalletItemBinding

class PalletAdapter(private val pallets: List<Pallet>) :
    RecyclerView.Adapter<PalletAdapter.PalletViewHolder>() {

    class PalletViewHolder(val binding: PalletItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PalletViewHolder {
        val binding = PalletItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PalletViewHolder, position: Int) {
        val pallet = pallets[position]
        holder.binding.tvPalletCode.text = pallet.codigo
        holder.binding.tvProductCode.text = "${pallet.Products?.get(0)?.productCode}"
        holder.binding.tvProductCant.text = "${pallet.Products?.get(0)?.maxCantByPallet}"
    }

    override fun getItemCount() = pallets.size
}
