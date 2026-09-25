package com.uvg.agroconecta.ui.help

enum class HelpCategory(val displayName: String) {
    ORDERS("Pedidos"),
    PAYMENTS("Pagos"),
    DELIVERIES("Entregas"),
    ACCOUNT("Cuenta"),
    PESTS("Plagas")
}

data class FrequentlyAskedQuestion(
    val id: String,
    val question: String,
    val answer: String
)

data class FaqSection(
    val category: HelpCategory,
    val questions: List<FrequentlyAskedQuestion>
)

val helpFaqSections = listOf(
    FaqSection(
        category = HelpCategory.ORDERS,
        questions = listOf(
            FrequentlyAskedQuestion(
                id = "orders-create",
                question = "¿Cómo hago un pedido?",
                answer = "Busca el producto que necesitas en el catálogo, agrégalo al carrito y " +
                    "revisa las cantidades. Luego selecciona el tipo de entrega y confirma el pedido."
            ),
            FrequentlyAskedQuestion(
                id = "orders-status",
                question = "¿Dónde consulto el estado de mi pedido?",
                answer = "Abre Pedidos en la barra inferior. Allí encontrarás tu historial y podrás " +
                    "entrar al seguimiento de cada pedido."
            ),
            FrequentlyAskedQuestion(
                id = "orders-urgent",
                question = "¿Cuándo debo usar un pedido urgente?",
                answer = "Úsalo cuando necesites atender una plaga con rapidez. En el carrito elige " +
                    "Entrega urgente, indica la plaga y confirma la dirección."
            )
        )
    ),
    FaqSection(
        category = HelpCategory.PAYMENTS,
        questions = listOf(
            FrequentlyAskedQuestion(
                id = "payments-method",
                question = "¿Qué métodos de pago están disponibles?",
                answer = "Actualmente puedes pagar en efectivo contra entrega. Tigo Money y " +
                    "Banrural Móvil aparecerán como opciones cuando estén disponibles."
            ),
            FrequentlyAskedQuestion(
                id = "payments-moment",
                question = "¿Cuándo debo pagar mi pedido?",
                answer = "El pago se realiza al recibir el pedido o al recogerlo en el punto del " +
                    "distribuidor, según la entrega que seleccionaste."
            ),
            FrequentlyAskedQuestion(
                id = "payments-total",
                question = "¿Dónde reviso el total antes de comprar?",
                answer = "El carrito muestra el subtotal de tus productos. Antes de confirmar verás " +
                    "el resumen del pedido, las cantidades y el total final."
            )
        )
    ),
    FaqSection(
        category = HelpCategory.DELIVERIES,
        questions = listOf(
            FrequentlyAskedQuestion(
                id = "deliveries-types",
                question = "¿Qué tipos de entrega puedo elegir?",
                answer = "Puedes solicitar entrega a domicilio o recoger el pedido en el punto " +
                    "indicado por el distribuidor."
            ),
            FrequentlyAskedQuestion(
                id = "deliveries-address",
                question = "¿Cómo cambio la dirección de entrega?",
                answer = "Durante la confirmación selecciona Entrega a domicilio y escribe la " +
                    "dirección correcta antes de enviar el pedido."
            ),
            FrequentlyAskedQuestion(
                id = "deliveries-tracking",
                question = "¿Cómo doy seguimiento a una entrega?",
                answer = "Entra a Pedidos, abre el pedido que deseas consultar y revisa su etapa " +
                    "actual y la fecha estimada de entrega."
            )
        )
    ),
    FaqSection(
        category = HelpCategory.ACCOUNT,
        questions = listOf(
            FrequentlyAskedQuestion(
                id = "account-edit",
                question = "¿Cómo actualizo mis datos?",
                answer = "Ve a Mi Perfil y selecciona Editar perfil. Revisa la información, realiza " +
                    "los cambios necesarios y presiona Guardar."
            ),
            FrequentlyAskedQuestion(
                id = "account-session",
                question = "¿Cómo cierro sesión?",
                answer = "En Mi Perfil desplázate hasta el final y presiona Cerrar sesión. La app te " +
                    "pedirá confirmar antes de salir."
            ),
            FrequentlyAskedQuestion(
                id = "account-security",
                question = "¿Cómo protejo mi cuenta?",
                answer = "No compartas tu contraseña ni los datos de tu sesión. Cierra sesión cuando " +
                    "uses un dispositivo compartido."
            )
        )
    ),
    FaqSection(
        category = HelpCategory.PESTS,
        questions = listOf(
            FrequentlyAskedQuestion(
                id = "pests-alerts",
                question = "¿Cómo consulto alertas de plagas cercanas?",
                answer = "Desde Inicio abre Alertas de plagas. Permite el acceso a tu ubicación para " +
                    "consultar reportes cercanos en la lista o en el mapa."
            ),
            FrequentlyAskedQuestion(
                id = "pests-report",
                question = "¿Cómo reporto una plaga?",
                answer = "En Alertas de plagas presiona Reportar plaga, selecciona el tipo, agrega " +
                    "una descripción útil y confirma la ubicación del reporte."
            ),
            FrequentlyAskedQuestion(
                id = "pests-notifications",
                question = "¿Por qué no recibo alertas de plagas?",
                answer = "Comprueba que AgroConecta tenga permisos de ubicación y notificaciones. " +
                    "También verifica que el dispositivo tenga conexión a internet."
            )
        )
    )
)
