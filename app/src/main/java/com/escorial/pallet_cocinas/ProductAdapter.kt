package com.escorial.pallet_cocinas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductCode: TextView = view.findViewById(R.id.tvProductCode)
        val tvProductSerial: TextView = view.findViewById(R.id.tvProductSerial)
        val tvProductType: TextView = view.findViewById(R.id.productTypeTextView)
        val itemLayout: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.tvProductCode.text = "${product.productCode}"
        holder.tvProductSerial.text = "${product.serial}"
        holder.tvProductType.text = "${product.type}"

        if (product.deleted) {
            holder.itemLayout.setBackgroundResource(R.drawable.item_deleted_background)
        } else {
            //holder.itemLayout.setBackgroundColor(Color.WHITE)
            holder.itemLayout.setBackgroundResource(R.drawable.rounded_input_text)
        }
    }

    override fun getItemCount() = products.size
}