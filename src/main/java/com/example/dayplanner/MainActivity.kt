package com.example.dayplanner

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dayplanner.databinding.ActivityMainBinding
import com.example.dayplanner.fragments.DashboardFragment
import com.example.dayplanner.fragments.MapFragment
import com.example.dayplanner.fragments.NearbyFragment
import com.example.dayplanner.fragments.PlacesListFragment
import com.example.dayplanner.fragments.RouteFragment
import com.example.dayplanner.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var isLandscape: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
                binding.root.findViewById<View>(R.id.rightMapPanel) != null

        setupNavigation()
        observeViewModel()
    }

    private fun setupNavigation() {
        if (!isLandscape) {
            // Portrait mode navigation via BottomNavigationView
            binding.bottomNavigation?.setOnItemSelectedListener { menuItem ->
                val tabPos = when (menuItem.itemId) {
                    R.id.nav_dashboard -> 0
                    R.id.nav_places -> 1
                    R.id.nav_map -> 2
                    R.id.nav_route -> 3
                    R.id.nav_nearby -> 4
                    else -> 0
                }
                viewModel.setSelectedTab(tabPos)
                true
            }
        } else {
            // Landscape mode navigation via left panel TabLayout
            val tabLayout = binding.root.findViewById<TabLayout>(R.id.landTabLayout)
            tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val position = tab?.position ?: 0
                    // In landscape tab layout: 0=Dashboard, 1=Places, 2=Route, 3=Nearby
                    val realTabPos = when (position) {
                        0 -> 0
                        1 -> 1
                        2 -> 3 // Route
                        3 -> 4 // Nearby
                        else -> 0
                    }
                    viewModel.setSelectedTab(realTabPos)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun loadFragmentForTab(tabPosition: Int) {
        val targetFragment: Fragment = when (tabPosition) {
            0 -> DashboardFragment()
            1 -> PlacesListFragment()
            2 -> MapFragment()
            3 -> RouteFragment()
            4 -> NearbyFragment()
            else -> DashboardFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, targetFragment)
            .commit()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedTabState.collect { tabPos ->
                        // Synchronize UI tab selection
                        if (!isLandscape) {
                            val navMenuId = when (tabPos) {
                                0 -> R.id.nav_dashboard
                                1 -> R.id.nav_places
                                2 -> R.id.nav_map
                                3 -> R.id.nav_route
                                4 -> R.id.nav_nearby
                                else -> R.id.nav_dashboard
                            }
                            if (binding.bottomNavigation?.selectedItemId != navMenuId) {
                                binding.bottomNavigation?.selectedItemId = navMenuId
                            }
                        } else {
                            val tabLayout = binding.root.findViewById<TabLayout>(R.id.landTabLayout)
                            val landTabPos = when (tabPos) {
                                0 -> 0
                                1 -> 1
                                3 -> 2
                                4 -> 3
                                else -> 0
                            }
                            if (tabLayout?.selectedTabPosition != landTabPos) {
                                tabLayout?.getTabAt(landTabPos)?.select()
                            }
                        }

                        // Load fragment
                        loadFragmentForTab(tabPos)
                    }
                }

                launch {
                    viewModel.userMessageState.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearUserMessage()
                        }
                    }
                }

                launch {
                    viewModel.isLoadingState.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
}
