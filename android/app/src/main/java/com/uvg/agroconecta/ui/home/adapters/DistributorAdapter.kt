package com.uvg.agroconecta.ui.home.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uvg.agroconecta.data.models.Distributor
import com.uvg.agroconecta.databinding.ItemDistributorCardBinding

class DistributorAdapter(
    private val onClick: (Distributor) -> Unit
) : ListAdapter<Distributor, DistributorAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDistributorCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemDistributorCardBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(d: Distributor) {
            b.tvDistributorName.text = d.nombreNegocio
            b.tvLocation.text = d.departamento ?: "Guatemala"
            b.tvRating.text = "★ %.1f".format(d.calificacion)

            // HU-023: Always show verified badge (this list only shows verified=true)
            // Badge is always visible for distributors in this list
            b.tvVerifiedBadge.text = "✓ Verificado"

            b.root.setOnClickListener { onClick(d) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Distributor>() {
        override fun areItemsTheSame(old: Distributor, new: Distributor) = old.id == new.id
        override fun areContentsTheSame(old: Distributor, new: Distributor) = old == new
    }
}
