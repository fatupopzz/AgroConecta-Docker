package com.uvg.agroconecta.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.uvg.agroconecta.R
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.databinding.FragmentProductDetailBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProductViewModel by viewModels()

    private var productId: Int = -1
    private var currentInventarioId: Int = -1
    private var currentDistributorId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productId = arguments?.getInt("product_id", -1) ?: -1

        setupListeners()
        setupObservers()

        if (productId != -1) {
            val token = runBlocking { SessionManager.getToken(requireContext()).first() }
            viewModel.loadProduct(productId, token)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Navigate to compare prices screen
        binding.btnCompare.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("product_id", productId)
            }
            findNavController().navigate(R.id.action_productDetail_to_comparePrice, bundle)
        }

        // HU-015: Trigger delivery flow
        binding.btnAddToCart.setOnClickListener {
            val token = runBlocking { SessionManager.getToken(requireContext()).first() }
            val farmerId = runBlocking { SessionManager.getFarmerId(requireContext()).first() }

            if (token == null) {
                Toast.makeText(context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentInventarioId == -1) {
                Toast.makeText(context, "No hay stock disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Navigate to delivery fragment (HU-015)
            val bundle = Bundle().apply {
                putInt("inventario_id", currentInventarioId)
                putInt("distribuidor_id", currentDistributorId)
                putInt("product_id", productId)
                putString("product_name", binding.tvProductName.text.toString())
                putString("price", binding.tvPrice.text.toString())
                putString("distributor_name", binding.tvDistributor.text.toString())
            }
            findNavController().navigate(R.id.action_productDetail_to_delivery, bundle)
        }
    }

    private fun setupObservers() {
        viewModel.productDetail.observe(viewLifecycleOwner) { product ->
            if (product == null) return@observe

            binding.tvProductName.text = product.nombre
            binding.tvCategory.text = product.categoria ?: "Producto"
            binding.tvDescription.text = product.descripcion ?: "Sin descripción disponible"
            binding.tvInstrucciones.text = product.instrucciones ?: "Ver etiqueta del producto"
            binding.tvComposicion.text = product.composicion ?: "—"
            binding.tvDosis.text = product.dosis ?: "—"
            binding.tvRating.text = "%.1f".format(product.calificacion)

            // Get best offer (lowest price)
            val bestOffer = product.ofertas.minByOrNull { it.precio }
            if (bestOffer != null) {
                binding.tvPrice.text = "Q %.2f".format(bestOffer.precio)
                binding.tvUnit.text = "por ${bestOffer.unidadMedida ?: "unidad"}"
                binding.tvDistributor.text = bestOffer.distribuidor
                binding.tvStock.text = "${bestOffer.stock}"
                binding.tvUnidad.text = bestOffer.unidadMedida ?: "—"

                currentInventarioId = bestOffer.idInventario
                currentDistributorId = bestOffer.idDistribuidor

                // HU-023: Show verified badge if distributor is verified
                val isVerified = bestOffer.estadoVerificacion == "verificado"
                binding.llVerifiedBadge.visibility = if (isVerified) View.VISIBLE else View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            // Could show shimmer/skeleton
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
