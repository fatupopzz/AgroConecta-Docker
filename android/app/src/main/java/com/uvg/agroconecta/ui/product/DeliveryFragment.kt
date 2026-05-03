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
import com.uvg.agroconecta.data.api.RetrofitClient
import com.uvg.agroconecta.data.api.SessionManager
import com.uvg.agroconecta.data.models.CreateOrderRequest
import com.uvg.agroconecta.data.models.OrderProduct
import com.uvg.agroconecta.databinding.FragmentDeliveryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * HU-015: Entrega directa a la finca
 *
 * Permite al agricultor seleccionar el tipo de entrega (domicilio a la finca
 * o punto de recogida) e ingresar la dirección de su finca para recibir los
 * insumos agrícolas sin necesidad de desplazarse.
 */
class DeliveryFragment : Fragment() {

    private var _binding: FragmentDeliveryBinding? = null
    private val binding get() = _binding!!

    // Arguments from ProductDetail
    private var inventarioId: Int = -1
    private var distribuidorId: Int = -1
    private var productId: Int = -1
    private var productName: String = ""
    private var price: String = ""
    private var distributorName: String = ""

    // Delivery type selection
    private var isHomeDelivery = true   // true = finca, false = pickup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read args
        inventarioId = arguments?.getInt("inventario_id", -1) ?: -1
        distribuidorId = arguments?.getInt("distribuidor_id", -1) ?: -1
        productId = arguments?.getInt("product_id", -1) ?: -1
        productName = arguments?.getString("product_name", "Producto") ?: "Producto"
        price = arguments?.getString("price", "Q 0.00") ?: "Q 0.00"
        distributorName = arguments?.getString("distributor_name", "") ?: ""

        populateSummary()
        setupDeliverySelection()
        setupConfirmButton()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun populateSummary() {
        binding.tvProductName.text = productName
        binding.tvProductPrice.text = price
        binding.tvDistributor.text = distributorName
        binding.tvTotal.text = price

        // HU-023: Show verified badge in summary too
        binding.llVerified.visibility = View.VISIBLE
    }

    /**
     * HU-015: Toggle between delivery types.
     * Home delivery = entrega directa a la finca (main feature).
     * Pickup = alternative option.
     */
    private fun setupDeliverySelection() {
        binding.cardDeliveryHome.setOnClickListener {
            isHomeDelivery = true
            binding.rbDeliveryHome.isChecked = true
            binding.rbPickup.isChecked = false
            // Show address section for finca delivery
            binding.llAddressSection.visibility = View.VISIBLE
            // Highlight card
            binding.cardDeliveryHome.cardElevation = 6f
            binding.cardPickup.cardElevation = 1f
        }

        binding.cardPickup.setOnClickListener {
            isHomeDelivery = false
            binding.rbPickup.isChecked = true
            binding.rbDeliveryHome.isChecked = false
            // Hide address section for pickup
            binding.llAddressSection.visibility = View.GONE
            binding.cardPickup.cardElevation = 6f
            binding.cardDeliveryHome.cardElevation = 1f
        }
    }

    /**
     * HU-015: Create order with direct finca delivery.
     * Calls POST /api/orders with tipo_entrega = 'domicilio'
     * and the farmer's finca address.
     */
    private fun setupConfirmButton() {
        binding.btnConfirmOrder.setOnClickListener {
            val token = runBlocking { SessionManager.getToken(requireContext()).first() }
            val farmerId = runBlocking { SessionManager.getFarmerId(requireContext()).first() }

            if (token == null) {
                Toast.makeText(context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate address if home delivery selected
            val address = buildAddress()
            if (isHomeDelivery && address.isBlank()) {
                Toast.makeText(
                    context,
                    "Ingresa la dirección de tu finca",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (distribuidorId == -1 || inventarioId == -1) {
                Toast.makeText(context, "Error: datos del pedido inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use farmerId from session; fallback to 1 for demo if not set
            val resolvedFarmerId = if ((farmerId ?: -1) > 0) farmerId!! else 1

            createOrder(token, resolvedFarmerId, address)
        }
    }

    private fun buildAddress(): String {
        val dept = binding.etDepartment.text?.toString()?.trim() ?: ""
        val addr = binding.etAddress.text?.toString()?.trim() ?: ""
        return if (isHomeDelivery) "$dept, $addr".trim().trimEnd(',') else "Punto de recogida en distribuidora"
    }

    private fun createOrder(token: String, farmerId: Int, address: String) {
        binding.progressOrder.visibility = View.VISIBLE
        binding.btnConfirmOrder.isEnabled = false

        val request = CreateOrderRequest(
            idAgricultor = farmerId,
            idDistribuidor = distribuidorId,
            direccionEntrega = address,
            productos = listOf(OrderProduct(idInventario = inventarioId, cantidad = 1)),
            metodoPago = "contra_entrega"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = RetrofitClient.getService(token)
                val response = api.createOrder(request)

                withContext(Dispatchers.Main) {
                    binding.progressOrder.visibility = View.GONE
                    binding.btnConfirmOrder.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val order = response.body()!!.pedido
                        showOrderSuccess(order.id)
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> "Datos del pedido inválidos"
                            404 -> "Agricultor o distribuidor no encontrado"
                            else -> "Error al crear el pedido (${response.code()})"
                        }
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressOrder.visibility = View.GONE
                    binding.btnConfirmOrder.isEnabled = true
                    Toast.makeText(
                        context,
                        "Error de conexión: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showOrderSuccess(orderId: Int) {
        Toast.makeText(
            context,
            "✓ Pedido #$orderId creado. El distribuidor llegará a tu finca.",
            Toast.LENGTH_LONG
        ).show()
        // Navigate back to home
        findNavController().navigate(R.id.homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
