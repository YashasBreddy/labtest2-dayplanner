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
import com.example.dayplanner.adapters.PlaceAdapter
import com.example.dayplanner.databinding.FragmentRouteBinding
import com.example.dayplanner.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: PlaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = PlaceAdapter(
            onToggleCompleted = { place, isCompleted ->
                viewModel.togglePlaceCompleted(place, isCompleted)
            },
            onDeleteClicked = { place ->
                viewModel.removePlace(place.id)
            }
        )
        binding.rvRouteStops.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRouteStops.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnRecalculateRoute.setOnClickListener {
            viewModel.planMyDay()
        }

        binding.btnClearRouteDetails.setOnClickListener {
            viewModel.clearRoute()
        }

        binding.btnPlanMyDayEmpty.setOnClickListener {
            viewModel.planMyDay()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.routeResultState.collect { route ->
                    if (route != null && route.orderedPlaces.isNotEmpty()) {
                        binding.tvRouteSummaryDetails.text = route.summary
                        adapter.submitList(route.orderedPlaces)
                        binding.rvRouteStops.visibility = View.VISIBLE
                        binding.layoutRouteEmpty.visibility = View.GONE
                        binding.btnClearRouteDetails.visibility = View.VISIBLE
                        binding.btnRecalculateRoute.visibility = View.VISIBLE
                    } else {
                        binding.tvRouteSummaryDetails.text = getString(com.example.dayplanner.R.string.no_route_yet)
                        adapter.submitList(emptyList())
                        binding.rvRouteStops.visibility = View.GONE
                        binding.layoutRouteEmpty.visibility = View.VISIBLE
                        binding.btnClearRouteDetails.visibility = View.GONE
                        binding.btnRecalculateRoute.visibility = View.GONE
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
