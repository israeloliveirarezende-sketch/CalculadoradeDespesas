package com.iortec.calculadoradedespesas

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable // <-- Certifique-se de que esta anotação está aqui!
fun EditExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newValue: Double, newTimestamp: Long) -> Unit
) {
    val context = LocalContext.current

    var titleInput by remember { mutableStateOf(expense.title) }
    var valueInput by remember { mutableStateOf(expense.value.toString().replace(".", ",")) }
    var selectedTimestamp by remember { mutableLongStateOf(expense.dateTimestamp) }

    val calendar = remember(selectedTimestamp) {
        Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    }

    val dateDisplay = remember(selectedTimestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        sdf.format(Date(selectedTimestamp))
    }

    // Função normal para abrir o DatePicker (sem chamar Composables dentro dela)
    fun abrirSeletorDeData() {
        val ano = calendar.get(Calendar.YEAR)
        val mes = calendar.get(Calendar.MONTH)
        val dia = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestamp = newCal.timeInMillis
            },
            ano, mes, dia
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E2024),
        title = {
            Text(
                text = "Editar Despesa",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Campo Título
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Nome da despesa", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF539165),
                        unfocusedBorderColor = Color(0xFF282B30),
                        focusedContainerColor = Color(0xFF141518),
                        unfocusedContainerColor = Color(0xFF141518)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo Valor
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it },
                    label = { Text("Valor (R$)", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF539165),
                        unfocusedBorderColor = Color(0xFF282B30),
                        focusedContainerColor = Color(0xFF141518),
                        unfocusedContainerColor = Color(0xFF141518)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Botão de Data
                OutlinedButton(
                    onClick = { abrirSeletorDeData() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF282B30)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF81C784),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Data:", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(
                            text = dateDisplay,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doubleValue = valueInput.replace(",", ".").toDoubleOrNull()
                    if (titleInput.isNotBlank() && doubleValue != null && doubleValue > 0) {
                        onConfirm(titleInput, doubleValue, selectedTimestamp)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF539165))
            ) {
                Text("Salvar", color = Color.White, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray, fontSize = 12.sp)
            }
        }
    )
}