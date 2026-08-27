package com.iortec.calculadoradedespesas

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val value: Double,
    val dateTimestamp: Long = System.currentTimeMillis() // Salva o tempo em milissegundos
) {
    // Data formatada para exibir no Card (ex: 26/07/2026)
    val date: String
        get() {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            return sdf.format(Date(dateTimestamp))
        }

    // Identificador de Mês/Ano para o filtro (ex: "2026-07")
    val monthYearKey: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            return sdf.format(Date(dateTimestamp))
        }
}