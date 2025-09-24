package com.escorial.pallet_cocinas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PalletAdapter(private val pallets: List<Pallet>) :
    RecyclerView.Adapter<PalletAdapter.ProductViewHolder>() {



    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPalletCode = view.findViewById<TextView>(R.id.tvPalletCode)
        val itemLayout: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val pallet = pallets[position]
        holder.tvPalletCode.text = "${pallet.codigo}"
    }

    override fun getItemCount() = pallets.size


}