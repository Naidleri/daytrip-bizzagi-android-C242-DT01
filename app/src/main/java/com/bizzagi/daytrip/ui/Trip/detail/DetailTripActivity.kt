package com.bizzagi.daytrip.ui.Trip.detail

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.bizzagi.daytrip.R
import com.bizzagi.daytrip.databinding.ActivityDetailTripBinding
import com.bizzagi.daytrip.ui.Trip.PlansViewModel
import com.google.android.material.tabs.TabLayoutMediator

class DetailTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailTripBinding

    private val viewModel: PlansViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.initializeTrip("trip1")
        setupViewPagerAndTabs()

        // Observe trip data
        observeTripData()
    }

    private fun setupViewPagerAndTabs() {
        val pagerAdapter = TripDaysPagerAdapter(this, viewModel.getStartDate(), viewModel.getEndDate())

        binding.viewPager.adapter = pagerAdapter

        // Link TabLayout dengan ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewModel.getFormattedDateForPosition(position)
        }.attach()

        // Listen untuk perubahan page
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateCurrentDay(position)
            }
        })
    }

    private fun observeTripData() {
        viewModel.selectedTrip.observe(this) { trip ->
            // Update UI berdasarkan trip data
            binding.viewPager.adapter?.notifyDataSetChanged()
        }
    }
}