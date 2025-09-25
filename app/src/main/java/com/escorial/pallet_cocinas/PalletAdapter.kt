package com.escorial.pallet_cocinas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PalletAdapter(private val pallets: List<Pallet>) :
    RecyclerView.Adapter<PalletAdapter.PalletViewHolder>() {



    class PalletViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPalletCode = view.findViewById<TextView>(R.id.tvPalletCode)
        val itemLayout: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PalletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.pallet_item, parent, false)
        return PalletViewHolder(view)
    }

    override fun onBindViewHolder(holder: PalletViewHolder, position: Int) {
        val pallet = pallets[position]
        holder.tvPalletCode.text = "${pallet.codigo}"
    }

    override fun getItemCount() = pallets.size


}