package com.escorial.pallet_cocinas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductCode: TextView = view.findViewById(R.id.tvProductCode)
        val tvProductSerial: TextView = view.findViewById(R.id.tvProductSerial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.tvProductCode.text = "Código: ${product.productCode}"
        holder.tvProductSerial.text = "Serie: ${product.serial}"
    }

    override fun getItemCount() = products.size
}