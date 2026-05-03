package com.uvg.agroconecta.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.uvg.agroconecta.R
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.databinding.FragmentHomeBinding
import com.uvg.agroconecta.ui.home.adapters.DistributorAdapter
import com.uvg.agroconecta.ui.home.adapters.ProductAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var productAdapter: ProductAdapter
    private lateinit var distributorAdapter: DistributorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSearch()
        setupChips()
        setupObservers()

        // Load data
        val token = runBlocking {
            SessionManager.getToken(requireContext()).first()
        }
        val userName = runBlocking {
            SessionManager.getUserName(requireContext()).first()
        }

        binding.tvUsername.text = userName?.substringBefore("@") ?: "Agricultor"
        viewModel.loadData(token)
    }

    private fun setupRecyclerViews() {
        productAdapter = ProductAdapter { product ->
            // Navigate to product detail
            val bundle = Bundle().apply {
                putInt("product_id", product.id)
            }
            findNavController().navigate(R.id.action_home_to_productDetail, bundle)
        }
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }

        // HU-023: Distributor list with verified badge
        distributorAdapter = DistributorAdapter { distributor ->
            // TODO: navigate to distributor profile
        }
        binding.rvDistributors.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = distributorAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val query = binding.etSearch.text?.toString()?.trim()
            if (!query.isNullOrEmpty()) {
                val token = runBlocking { SessionManager.getToken(requireContext()).first() }
                viewModel.searchProducts(token, query)
            }
            true
        }
    }

    private fun setupChips() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val token = runBlocking { SessionManager.getToken(requireContext()).first() }
            when {
                checkedIds.contains(R.id.chip_all) -> viewModel.loadProducts(token)
                checkedIds.contains(R.id.chip_fertilizantes) -> viewModel.loadProductsByCategory(token, 1)
                checkedIds.contains(R.id.chip_pesticidas) -> viewModel.loadProductsByCategory(token, 2)
                checkedIds.contains(R.id.chip_semillas) -> viewModel.loadProductsByCategory(token, 4)
                checkedIds.contains(R.id.chip_herbicidas) -> viewModel.loadProductsByCategory(token, 3)
            }
        }

        binding.tvSeeMore.setOnClickListener {
            findNavController().navigate(R.id.catalogFragment)
        }
    }

    private fun setupObservers() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
        }

        viewModel.distributors.observe(viewLifecycleOwner) { distributors ->
            distributorAdapter.submitList(distributors)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            // Could show/hide shimmer here
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
