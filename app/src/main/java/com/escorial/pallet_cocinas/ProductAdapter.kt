package com.escorial.pallet_cocinas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.escorial.pallet_cocinas.data.model.Product
import com.escorial.pallet_cocinas.databinding.ProductItemBinding

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ProductItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.tvProductCode.text = "${product.productCode}"
        holder.binding.tvProductSerial.text = "${product.serial}"
        holder.binding.productTypeTextView.text = "${product.type}"

        if (product.deleted) {
            holder.binding.root.setBackgroundResource(R.drawable.item_deleted_background)
        } else {
            holder.binding.root.setBackgroundResource(R.drawable.rounded_input_text)
        }
    }

    override fun getItemCount() = products.size
}
