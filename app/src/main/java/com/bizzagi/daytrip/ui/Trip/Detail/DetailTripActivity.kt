package com.bizzagi.daytrip.ui.Trip.Detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bizzagi.daytrip.data.retrofit.repository.PlansDummyRepository
import com.bizzagi.daytrip.databinding.ActivityDetailTripBinding
import com.bizzagi.daytrip.ui.Trip.PlansViewModel
import com.bizzagi.daytrip.utils.ViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator

class DetailTripActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailTripBinding

    private lateinit var viewModel: PlansViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = PlansDummyRepository// Replace with your actual repository instance
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(PlansViewModel::class.java)

        val tripId = intent.getStringExtra("TRIP_ID") ?: return
        viewModel.initializeTrip(tripId)
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