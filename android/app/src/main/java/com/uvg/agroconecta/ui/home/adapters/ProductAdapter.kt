package com.uvg.agroconecta.ui.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uvg.agroconecta.data.models.Product
import com.uvg.agroconecta.databinding.ItemProductCardBinding

class ProductAdapter(
    private val onClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemProductCardBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(product: Product) {
            b.tvProductName.text = product.nombre
            b.tvCategory.text = product.categoria ?: "Producto"
            b.tvDistributorName.text = product.marca ?: "—"

            // Format price
            val price = product.precioDesde
            b.tvPrice.text = if (price != null) "Q %.2f".format(price) else "—"

            // HU-023: Show verified badge when product has distributors
            // (All products from /api/products come from verified distributors
            //  due to the backend filter WHERE estado_verificacion = 'verificado')
            val hasDistributors = (product.numDistribuidores ?: 0) > 0
            b.llVerifiedBadge.visibility = if (hasDistributors) View.VISIBLE else View.GONE

            b.root.setOnClickListener { onClick(product) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
        override fun areContentsTheSame(old: Product, new: Product) = old == new
    }
}
