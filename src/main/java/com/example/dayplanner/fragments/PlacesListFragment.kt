package com.example.dayplanner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dayplanner.R
import com.example.dayplanner.adapters.PlaceAdapter
import com.example.dayplanner.adapters.SearchResultsAdapter
import com.example.dayplanner.databinding.FragmentPlacesListBinding
import com.example.dayplanner.models.Place
import com.example.dayplanner.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class PlacesListFragment : Fragment() {

    private var _binding: FragmentPlacesListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var placeAdapter: PlaceAdapter
    private lateinit var searchResultsAdapter: SearchResultsAdapter

    private var currentFilter: String = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlacesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        placeAdapter = PlaceAdapter(
            onToggleCompleted = { place, isCompleted ->
                viewModel.togglePlaceCompleted(place, isCompleted)
            },
            onDeleteClicked = { place ->
                viewModel.removePlace(place.id)
            }
        )

        binding.rvPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaces.adapter = placeAdapter

        searchResultsAdapter = SearchResultsAdapter { placeToAdd ->
            viewModel.addPlace(placeToAdd)
            binding.etSearchQuery.setText("")
            binding.layoutSearchResults.visibility = View.GONE
        }

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = searchResultsAdapter
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }

        binding.etSearchQuery.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.btnClearSearch.visibility = View.GONE
                binding.layoutSearchResults.visibility = View.GONE
            } else {
                binding.btnClearSearch.visibility = View.VISIBLE
            }
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchQuery.setText("")
            binding.layoutSearchResults.visibility = View.GONE
        }

        binding.btnPlanMyDayList.setOnClickListener {
            viewModel.planMyDay()
            viewModel.setSelectedTab(2) // Map tab
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipSightseeing) -> Place.STOP_TYPE_SIGHTSEEING
                checkedIds.contains(R.id.chipFood) -> Place.STOP_TYPE_FOOD
                checkedIds.contains(R.id.chipRest) -> Place.STOP_TYPE_REST
                else -> "ALL"
            }
            applyFilter()
        }
    }

    private fun performSearch() {
        val query = binding.etSearchQuery.text.toString().trim()
        if (query.isNotEmpty()) {
            viewModel.searchPlaces(query)
            binding.layoutSearchResults.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.placesState.collect { _ ->
                        applyFilter()
                    }
                }

                launch {
                    viewModel.searchResultsState.collect { results ->
                        searchResultsAdapter.submitList(results)
                        if (results.isNotEmpty() && binding.etSearchQuery.text.isNotBlank()) {
                            binding.layoutSearchResults.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun applyFilter() {
        val allPlaces = viewModel.placesState.value
        val filtered = when (currentFilter) {
            Place.STOP_TYPE_SIGHTSEEING -> allPlaces.filter { it.isSightseeing() }
            Place.STOP_TYPE_FOOD -> allPlaces.filter { it.isFoodStop() }
            Place.STOP_TYPE_REST -> allPlaces.filter { it.isRestStop() }
            else -> allPlaces
        }

        placeAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvPlaces.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvPlaces.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
