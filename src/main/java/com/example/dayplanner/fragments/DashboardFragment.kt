package com.example.dayplanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dayplanner.R
import com.example.dayplanner.databinding.FragmentDashboardBinding
import com.example.dayplanner.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnPlanMyDay.setOnClickListener {
            viewModel.planMyDay()
            viewModel.setSelectedTab(2) // Jump to Map
        }

        binding.cardAddPlaces.setOnClickListener {
            viewModel.setSelectedTab(1) // Jump to Places List
        }

        binding.cardViewMap.setOnClickListener {
            viewModel.setSelectedTab(2) // Jump to Map
        }

        binding.cardNearbyFood.setOnClickListener {
            viewModel.setSelectedTab(4) // Jump to Nearby
        }

        binding.cardNearbyRest.setOnClickListener {
            viewModel.setSelectedTab(4) // Jump to Nearby
        }

        binding.btnLoadSampleData.setOnClickListener {
            viewModel.loadSamplePlaces()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.placesState.collect { places ->
                        val total = places.size
                        val completed = places.count { it.isCompleted }
                        val remaining = total - completed

                        binding.tvTotalPlacesCount.text = total.toString()
                        binding.tvCompletedPlacesCount.text = completed.toString()
                        binding.tvRemainingPlacesCount.text = remaining.toString()
                    }
                }

                launch {
                    viewModel.routeResultState.collect { route ->
                        if (route != null) {
                            binding.tvRouteBadge.text = getString(R.string.route_calculated)
                            binding.tvRouteBadge.setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.status_green_bg)
                            )
                            binding.tvRouteBadge.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.status_green)
                            )
                        } else {
                            binding.tvRouteBadge.text = getString(R.string.route_not_calculated)
                            binding.tvRouteBadge.setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.status_orange_bg)
                            )
                            binding.tvRouteBadge.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.status_orange)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
