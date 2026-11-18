package com.example.dayplanner.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dayplanner.R
import com.example.dayplanner.databinding.ItemPlaceBinding
import com.example.dayplanner.models.Place

class PlaceAdapter(
    private val onToggleCompleted: (Place, Boolean) -> Unit,
    private val onDeleteClicked: (Place) -> Unit
) : ListAdapter<Place, PlaceAdapter.PlaceViewHolder>(PlaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaceViewHolder(private val binding: ItemPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(place: Place) {
            val context = binding.root.context
            binding.tvPlaceName.text = place.name
            binding.tvPlaceAddress.text = place.address

            // Strike-through text if completed
            if (place.isCompleted) {
                binding.tvPlaceName.paintFlags =
                    binding.tvPlaceName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvPlaceName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            } else {
                binding.tvPlaceName.paintFlags =
                    binding.tvPlaceName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvPlaceName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }

            // Checkbox state
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = place.isCompleted
            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleCompleted(place, isChecked)
            }

            // Stop type badge & color
            when {
                place.isFoodStop() -> {
                    binding.tvBadgeStopType.text = "FOOD STOP"
                    binding.tvBadgeStopType.setBackgroundColor(ContextCompat.getColor(context, R.color.stop_food))
                    binding.tvBadgeStopType.visibility = View.VISIBLE
                }
                place.isRestStop() -> {
                    binding.tvBadgeStopType.text = "REST STOP"
                    binding.tvBadgeStopType.setBackgroundColor(ContextCompat.getColor(context, R.color.stop_rest))
                    binding.tvBadgeStopType.visibility = View.VISIBLE
                }
                else -> {
                    binding.tvBadgeStopType.text = "SIGHTSEEING"
                    binding.tvBadgeStopType.setBackgroundColor(ContextCompat.getColor(context, R.color.stop_sightseeing))
                    binding.tvBadgeStopType.visibility = View.VISIBLE
                }
            }

            // Visit order badge (if route planned)
            if (place.visitOrder > 0) {
                binding.tvVisitOrder.text = "#${place.visitOrder}"
                binding.tvVisitOrder.visibility = View.VISIBLE
            } else {
                binding.tvVisitOrder.visibility = View.GONE
            }

            // Delete button
            binding.btnDelete.setOnClickListener {
                onDeleteClicked(place)
            }
        }
    }

    class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem == newItem
        }
    }
}
