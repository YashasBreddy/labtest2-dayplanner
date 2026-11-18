package com.example.dayplanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dayplanner.databinding.ItemSearchResultBinding
import com.example.dayplanner.models.Place

class SearchResultsAdapter(
    private val onAddClicked: (Place) -> Unit
) : ListAdapter<Place, SearchResultsAdapter.SearchViewHolder>(PlaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(place: Place) {
            binding.tvSearchResultName.text = place.name
            binding.tvSearchResultAddress.text = place.address

            if (place.rating > 0.0) {
                binding.tvRating.text = "★ ${place.rating}"
                binding.tvRating.visibility = View.VISIBLE
            } else {
                binding.tvRating.visibility = View.GONE
            }

            binding.btnAddSearchResult.setOnClickListener {
                onAddClicked(place)
            }
        }
    }

    class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem.name == newItem.name && oldItem.latitude == newItem.latitude
        }

        override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem == newItem
        }
    }
}
