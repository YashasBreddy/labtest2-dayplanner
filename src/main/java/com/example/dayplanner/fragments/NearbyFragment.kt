package com.example.dayplanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dayplanner.R
import com.example.dayplanner.adapters.SearchResultsAdapter
import com.example.dayplanner.databinding.FragmentNearbyBinding
import com.example.dayplanner.models.Place
import com.example.dayplanner.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class NearbyFragment : Fragment() {

    private var _binding: FragmentNearbyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: SearchResultsAdapter

    private var isFoodSelected = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SearchResultsAdapter { place ->
            val stopType = if (isFoodSelected) Place.STOP_TYPE_FOOD else Place.STOP_TYPE_REST
            val stopToAdd = place.copy(stopType = stopType)
            viewModel.addPlace(stopToAdd)
        }
        binding.rvNearbyResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNearbyResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.chipGroupNearbyType.setOnCheckedStateChangeListener { _, checkedIds ->
            isFoodSelected = checkedIds.contains(R.id.chipNearbyFood)
            updateNearbyList()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.nearbyFoodState.collect { _ ->
                        if (isFoodSelected) updateNearbyList()
                    }
                }
                launch {
                    viewModel.nearbyRestState.collect { _ ->
                        if (!isFoodSelected) updateNearbyList()
                    }
                }
            }
        }
    }

    private fun updateNearbyList() {
        if (isFoodSelected) {
            adapter.submitList(viewModel.nearbyFoodState.value)
        } else {
            adapter.submitList(viewModel.nearbyRestState.value)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
