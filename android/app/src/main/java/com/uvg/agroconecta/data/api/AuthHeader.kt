package com.uvg.agroconecta.data.api

/**
 * Arma la cabecera Authorization para los endpoints que la reciben como @Header.
 *
 * Devuelve null cuando no hay token: Retrofit omite la cabecera y el mismo
 * endpoint sirve para invitado y para sesion iniciada. Esta centralizado a
 * proposito: cuando cada ViewModel armaba la cabecera a su manera terminaron
 * saliendo Authorization duplicadas (KAN-69).
 */
internal fun String?.toAuthHeader(): String? =
    if (isNullOrBlank()) null else "Bearer $this"
