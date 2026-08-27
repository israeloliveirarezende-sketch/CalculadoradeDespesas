package com.iortec.calculadoradedespesas

import android.app.DatePickerDialog
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iortec.calculadoradedespesas.ui.theme.DarkBackground
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Define tamanhos maiores para tablets
    val tamanhoFonteVisor = if (isTablet) 32.sp else 22.sp
    val tamanhoFonteBotoes = if (isTablet) 20.sp else 14.sp
    val tamanhoFonteLista = if (isTablet) 14.sp else 10.sp
    val context = LocalContext.current
    val notesPreferences = remember { NotesPreferences(context) }
    val scope = rememberCoroutineScope()

    val anotacoesSalvas by notesPreferences.anotacoesFlow.collectAsState(initial = null)
    val despesasSalvas by notesPreferences.despesasFlow.collectAsState(initial = emptyList())
    val orcamentoSalvo by notesPreferences.orcamentoFlow.collectAsState(initial = 0.0)
    val corTemaSalva by notesPreferences.corTemaFlow.collectAsState(initial = 0xFF539165)
    val modoExibicaoSalvo by notesPreferences.modoExibicaoFlow.collectAsState(initial = "AMBAS")

    // Leitura do estado do Tema Claro (Padrão: false/Escuro)
    val isTemaClaro by notesPreferences.temaClaroFlow.collectAsState(initial = false)

    // =======================================================
    // VARIÁVEIS DE COR DINÂMICAS (TEMA CLARO x ESCURO)
    // =======================================================
    val fundoTela = if (isTemaClaro) Color(0xFFF2F4F8) else DarkBackground
    val fundoPainel = if (isTemaClaro) Color(0xFFFFFFFF) else Color(0xFF1E2024)
    val fundoCaixaInterna = if (isTemaClaro) Color(0xFFE8ECEF) else Color(0xFF141518)
    val fundoItemLista = if (isTemaClaro) Color(0xFFE0E5EA) else Color(0xFF282B30)
    val textoPrincipal = if (isTemaClaro) Color(0xFF1A1A1A) else Color.White
    val textoSecundario = if (isTemaClaro) Color(0xFF555555) else Color.Gray
    val divisorCor = if (isTemaClaro) Color(0xFFD0D5DD) else Color(0xFF282B30)

    var textoAnotacao by remember { mutableStateOf("") }
    var expensesList by remember { mutableStateOf(listOf<Expense>()) }
    var expenseTitleInput by remember { mutableStateOf("") }

    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    var showMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val currentMonthKey = remember(selectedCalendar.timeInMillis) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedCalendar.time)
    }

    val currentMonthDisplay = remember(selectedCalendar.timeInMillis) {
        val sdf = SimpleDateFormat("MMMM / yyyy", Locale("pt", "BR"))
        sdf.format(selectedCalendar.time).replaceFirstChar { it.uppercase() }
    }

    val filteredExpenses = remember(expensesList, currentMonthKey) {
        expensesList.filter { it.monthYearKey == currentMonthKey }
    }

    LaunchedEffect(anotacoesSalvas) {
        if (anotacoesSalvas != null && textoAnotacao.isEmpty()) {
            textoAnotacao = anotacoesSalvas!!
        }
    }

    LaunchedEffect(despesasSalvas) {
        expensesList = despesasSalvas
    }

    // Diálogos Modais
    if (showSettingsDialog) {
        SettingsDialog(
            corAtual = corTemaSalva,
            modoAtual = modoExibicaoSalvo,
            isTemaClaro = isTemaClaro,
            onDismiss = { showSettingsDialog = false },
            onCorChange = { novaCor ->
                scope.launch { notesPreferences.salvarCorTema(novaCor) }
            },
            onModoChange = { novoModo ->
                scope.launch { notesPreferences.salvarModoExibicao(novoModo) }
            },
            onTemaClaroChange = { novoEstado ->
                scope.launch { notesPreferences.salvarTemaClaro(novoEstado) }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false }, isTemaClaro = isTemaClaro)
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false }, isTemaClaro = isTemaClaro)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Calculadora", color = textoPrincipal)
                        Text(
                            text = "e Despesas",
                            color = Color(corTemaSalva),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções",
                            tint = textoPrincipal
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(fundoPainel)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Exportar Relatório PDF", color = textoPrincipal, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                if (filteredExpenses.isNotEmpty()) {
                                    exportarRelatorioPdf(context, currentMonthDisplay, filteredExpenses, orcamentoSalvo)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Configurações", color = textoPrincipal, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showSettingsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sobre", color = textoPrincipal, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Política de privacidade", color = textoPrincipal, fontSize = 13.sp) },
                            onClick = {
                                showMenu = false
                                showPrivacyDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = fundoTela)
            )
        },
        containerColor = fundoTela
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Painel Esquerdo - Lista
                if (modoExibicaoSalvo == "AMBAS" || modoExibicaoSalvo == "DESPESAS") {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(fundoPainel, shape = RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        ExpenseListWidget(
                            expenses = filteredExpenses,
                            orcamento = orcamentoSalvo,
                            corTema = corTemaSalva,
                            monthDisplay = currentMonthDisplay,
                            selectedCalendar = selectedCalendar,
                            titleInput = expenseTitleInput,
                            textoPrincipal = textoPrincipal,
                            textoSecundario = textoSecundario,
                            fundoCaixaInterna = fundoCaixaInterna,
                            fundoItemLista = fundoItemLista,
                            fundoPainel = fundoPainel,
                            onTitleChange = { expenseTitleInput = it },
                            onOrcamentoChange = { novoVal ->
                                scope.launch { notesPreferences.salvarOrcamento(novoVal) }
                            },
                            onDaySelected = { dia ->
                                val cal = selectedCalendar.clone() as Calendar
                                cal.set(Calendar.DAY_OF_MONTH, dia)
                                selectedCalendar = cal
                            },
                            onPreviousMonth = {
                                val cal = selectedCalendar.clone() as Calendar
                                cal.add(Calendar.MONTH, -1)
                                selectedCalendar = cal
                            },
                            onNextMonth = {
                                val cal = selectedCalendar.clone() as Calendar
                                cal.add(Calendar.MONTH, 1)
                                selectedCalendar = cal
                            },
                            onRemoveExpense = { id ->
                                val novaLista = expensesList.filter { it.id != id }
                                expensesList = novaLista
                                scope.launch { notesPreferences.salvarDespesas(novaLista) }
                            },
                            onEditExpense = { updatedExpense ->
                                val novaLista = expensesList.map { if (it.id == updatedExpense.id) updatedExpense else it }
                                expensesList = novaLista
                                scope.launch { notesPreferences.salvarDespesas(novaLista) }
                            }
                        )
                    }
                }

                // Painel Direito - Calculadora
                if (modoExibicaoSalvo == "AMBAS" || modoExibicaoSalvo == "CALCULADORA") {
                    Box(
                        modifier = Modifier
                            .weight(if (modoExibicaoSalvo == "AMBAS") 1.1f else 1f)
                            .fillMaxHeight()
                            .background(fundoPainel, shape = RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        CalculadoraWidget(
                            corTema = corTemaSalva,
                            expenseCount = filteredExpenses.size,
                            totalMonth = filteredExpenses.sumOf { it.value },
                            orcamento = orcamentoSalvo, // <-- NOVO PARÂMETRO PASSADO AQUI
                            textoPrincipal = textoPrincipal,
                            textoSecundario = textoSecundario,
                            fundoCaixaInterna = fundoCaixaInterna,
                            divisorCor = divisorCor,
                            isTemaClaro = isTemaClaro,
                            onCalculateAndAddExpense = { calculatedValue ->
                                if (expenseTitleInput.isNotBlank() && calculatedValue > 0) {
                                    val newExpense = Expense(
                                        title = expenseTitleInput,
                                        value = calculatedValue,
                                        dateTimestamp = selectedCalendar.timeInMillis
                                    )
                                    val novaLista = expensesList + newExpense
                                    expensesList = novaLista
                                    expenseTitleInput = ""
                                    scope.launch { notesPreferences.salvarDespesas(novaLista) }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de Anotações
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(fundoPainel, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = textoAnotacao,
                    onValueChange = { novoTexto ->
                        textoAnotacao = novoTexto
                        scope.launch { notesPreferences.salvarAnotacoes(novoTexto) }
                    },
                    placeholder = { Text("Anotações...", color = textoSecundario) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textoPrincipal,
                        unfocusedTextColor = textoPrincipal,
                        focusedBorderColor = Color(corTemaSalva),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Color(corTemaSalva)
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// =======================================================
// WIDGET COM SELETOR MENSAL
// =======================================================
@Composable
fun ExpenseListWidget(
    expenses: List<Expense>,
    orcamento: Double,
    corTema: Long,
    monthDisplay: String,
    selectedCalendar: Calendar,
    titleInput: String,
    textoPrincipal: Color,
    textoSecundario: Color,
    fundoCaixaInterna: Color,
    fundoItemLista: Color,
    fundoPainel: Color,
    onTitleChange: (String) -> Unit,
    onOrcamentoChange: (Double) -> Unit,
    onDaySelected: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRemoveExpense: (String) -> Unit,
    onEditExpense: (updatedExpense: Expense) -> Unit
) {
    val totalDespesas = expenses.sumOf { it.value }
    val context = LocalContext.current

    var selectedExpenseToEdit by remember { mutableStateOf<Expense?>(null) }

    selectedExpenseToEdit?.let { expenseToEdit ->
        EditExpenseDialog(
            expense = expenseToEdit,
            onDismiss = { selectedExpenseToEdit = null },
            onConfirm = { newTitle, newValue, newTimestamp ->
                val updated = expenseToEdit.copy(
                    title = newTitle,
                    value = newValue,
                    dateTimestamp = newTimestamp
                )
                onEditExpense(updated)
                selectedExpenseToEdit = null
            }
        )
    }

    fun abrirSeletorDeDia() {
        val ano = selectedCalendar.get(Calendar.YEAR)
        val mes = selectedCalendar.get(Calendar.MONTH)
        val diaAtual = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, _, _, dayOfMonth -> onDaySelected(dayOfMonth) },
            ano, mes, diaAtual
        ).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BudgetWidget(
            orcamento = orcamento,
            totalGasto = totalDespesas,
            corTema = corTema,
            textoPrincipal = textoPrincipal,
            textoSecundario = textoSecundario,
            fundoCaixaInterna = fundoCaixaInterna,
            fundoPainel = fundoPainel,
            onOrcamentoChange = onOrcamentoChange
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(fundoCaixaInterna, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(22.dp)) {
                Text("◄", color = Color(corTema), fontSize = 12.sp)
            }

            Text(
                text = monthDisplay,
                color = textoPrincipal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            IconButton(onClick = onNextMonth, modifier = Modifier.size(22.dp)) {
                Text("►", color = Color(corTema), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        OutlinedTextField(
            value = titleInput,
            onValueChange = onTitleChange,
            placeholder = { Text("Despesa +", color = textoSecundario, fontSize = 12.sp, maxLines = 1) },
            singleLine = true,
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .clickable { abrirSeletorDeDia() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Dia ${selectedCalendar.get(Calendar.DAY_OF_MONTH)}",
                        color = Color(corTema),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textoPrincipal,
                unfocusedTextColor = textoPrincipal,
                focusedBorderColor = Color(corTema),
                unfocusedBorderColor = fundoItemLista,
                focusedContainerColor = fundoCaixaInterna,
                unfocusedContainerColor = fundoCaixaInterna
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma despesa neste mês", color = textoSecundario, fontSize = 10.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(expenses, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(fundoItemLista, shape = RoundedCornerShape(6.dp))
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedExpenseToEdit = item }
                            ) {
                                Text(
                                    text = item.title,
                                    color = textoPrincipal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.date,
                                    color = textoSecundario,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = formatarMoeda(item.value),
                                    color = Color(corTema),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { onRemoveExpense(item.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Text("✕", color = Color(0xFFFF8A8A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(fundoCaixaInterna, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total:", color = textoSecundario, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                text = formatarMoeda(totalDespesas),
                color = Color(corTema),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

fun formatarMoeda(valor: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(valor)
}

// =======================================================
// PAINEL DIREITO: CALCULADORA
// =======================================================
@Composable
fun CalculadoraWidget(
    corTema: Long = 0xFF539165,
    expenseCount: Int = 0,
    totalMonth: Double = 0.0,
    orcamento: Double = 0.0, // <-- NOVO PARÂMETRO
    textoPrincipal: Color,
    textoSecundario: Color,
    fundoCaixaInterna: Color,
    divisorCor: Color,
    isTemaClaro: Boolean,
    onCalculateAndAddExpense: (Double) -> Unit
) {
    var visorText by remember { mutableStateOf("0") }
    var expressaoText by remember { mutableStateOf("") }
    var primeiroNumero by remember { mutableStateOf<Double?>(null) }
    var operador by remember { mutableStateOf<String?>(null) }
    var novoNumero by remember { mutableStateOf(true) }

    fun onNumeroClick(numero: String) {
        if (novoNumero || visorText == "0" || visorText == "Erro") {
            visorText = if (numero == ",") "0," else numero
            novoNumero = false
        } else {
            if (numero == "," && visorText.contains(",")) return
            visorText += numero
        }
    }

    fun onOperadorClick(op: String) {
        val valAtual = visorText.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        primeiroNumero = valAtual
        operador = op
        expressaoText = "$visorText $op"
        novoNumero = true
    }

    fun onBackspaceClick() {
        if (novoNumero || visorText == "Erro") return

        if (visorText.length > 1) {
            visorText = visorText.dropLast(1)
            if (visorText == "-" || visorText.isEmpty()) {
                visorText = "0"
                novoNumero = true
            }
        } else {
            visorText = "0"
            novoNumero = true
        }
    }

    fun onIgualClick() {
        val segundoNumero = visorText.replace(".", "").replace(",", ".").toDoubleOrNull()
        var resultadoFinal = 0.0

        if (primeiroNumero != null && segundoNumero != null && operador != null) {
            expressaoText = "$expressaoText $visorText"
            resultadoFinal = when (operador) {
                "+" -> primeiroNumero!! + segundoNumero
                "-" -> primeiroNumero!! - segundoNumero
                "×" -> primeiroNumero!! * segundoNumero
                "÷" -> if (segundoNumero != 0.0) primeiroNumero!! / segundoNumero else Double.NaN
                else -> 0.0
            }

            visorText = if (resultadoFinal.isNaN()) {
                "Erro"
            } else if (resultadoFinal % 1 == 0.0) {
                resultadoFinal.toLong().toString()
            } else {
                String.format("%.2f", resultadoFinal).replace(".", ",")
            }

            primeiroNumero = null
            operador = null
            novoNumero = true
        } else {
            resultadoFinal = visorText.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        }

        if (!resultadoFinal.isNaN() && resultadoFinal > 0) {
            onCalculateAndAddExpense(resultadoFinal)
        }
    }

    fun onClearClick() {
        visorText = "0"
        expressaoText = ""
        primeiroNumero = null
        operador = null
        novoNumero = true
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ÁREA SUPERIOR: MINI DASHBOARD + VISOR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f)
                .background(fundoCaixaInterna, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Qtd. Itens", color = textoSecundario, fontSize = 10.sp)
                        Text(
                            text = "$expenseCount",
                            color = textoPrincipal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // EXIBIÇÃO DO SALDO NO LUGAR DA MÉDIA
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saldo", color = textoSecundario, fontSize = 10.sp)
                        val saldo = orcamento - totalMonth
                        val corSaldo = if (saldo >= 0) Color(0xFF81C784) else Color(0xFFFF8A8A)
                        Text(
                            text = formatarMoeda(saldo),
                            color = corSaldo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                HorizontalDivider(color = divisorCor, thickness = 1.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expressaoText,
                        color = textoSecundario,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Text(
                        text = visorText,
                        color = textoPrincipal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TECLADO DA CALCULADORA
        val botoes = listOf(
            listOf("MC", "MR", "M+", "M-"),
            listOf("C", "E", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ",", "=")
        )

        Column(
            modifier = Modifier.weight(0.75f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            botoes.forEach { linha ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    linha.forEach { textoBotao ->
                        val isOperador = textoBotao in listOf("÷", "×", "-", "+", "=")
                        val isClear = textoBotao == "C"
                        val isBackspace = textoBotao == "E"
                        val isMemoria = textoBotao in listOf("MC", "MR", "M+", "M-")

                        val corFundo = when {
                            isOperador -> Color(corTema)
                            isClear -> if (isTemaClaro) Color(0xFFFFCDD2) else Color(0xFF8B3A3A)
                            isBackspace -> if (isTemaClaro) Color(0xFFFFE0B2) else Color(0xFF3B4048)
                            isMemoria -> if (isTemaClaro) Color(0xFFE0E0E0) else Color(0xFF23252A)
                            else -> if (isTemaClaro) Color(0xFFD6DBE1) else Color(0xFF282B30)
                        }

                        val pesoBotao = if (textoBotao == "=") 2f else 1f

                        Button(
                            onClick = {
                                when (textoBotao) {
                                    "C" -> onClearClick()
                                    "E" -> onBackspaceClick()
                                    "=" -> onIgualClick()
                                    "+", "-", "×", "÷" -> onOperadorClick(textoBotao)
                                    else -> onNumeroClick(textoBotao)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = corFundo),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(pesoBotao)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = textoBotao,
                                color = when {
                                    isOperador -> Color.White
                                    isClear -> if (isTemaClaro) Color(0xFFD32F2F) else Color(0xFFFF8A8A)
                                    isBackspace -> if (isTemaClaro) Color(0xFFE65100) else Color(0xFFFFB74D)
                                    else -> if (isTemaClaro) Color(0xFF1A1A1A) else Color.White
                                },
                                fontSize = if (isMemoria) 11.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- DIÁLOGO POLÍTICA DE PRIVACIDADE ---
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit, isTemaClaro: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isTemaClaro) Color.White else Color(0xFF1E2024),
        title = {
            Text(
                text = "Política de Privacidade",
                color = if (isTemaClaro) Color.Black else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sua privacidade é nossa prioridade.",
                    color = Color(0xFF81C784),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Todos os dados de despesas e anotações são armazenados localmente no seu dispositivo através do DataStore.\n\n" +
                            "• Não coletamos, transmitimos nem compartilhamos dados pessoais com servidores externos.\n\n" +
                            "• Os relatórios gerados em PDF permanecem estritamente sob o controle do usuário.",
                    color = if (isTemaClaro) Color.DarkGray else Color.White,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF539165))
            ) {
                Text("Entendi", color = Color.White, fontSize = 12.sp)
            }
        }
    )
}

// --- DIÁLOGO SOBRE ---
@Composable
fun AboutDialog(onDismiss: () -> Unit, isTemaClaro: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isTemaClaro) Color.White else Color(0xFF1E2024),
        title = {
            Text(
                text = "Sobre o App",
                color = if (isTemaClaro) Color.Black else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Calculadora e Despesas",
                    color = Color(0xFF81C784),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Versão 1.0.0",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                HorizontalDivider(color = if (isTemaClaro) Color.LightGray else Color(0xFF282B30), thickness = 1.dp)

                Text(
                    text = "Aplicativo desenvolvido para facilitar a gestão financeira diária com calculadora integrada, controle de lançamentos e exportação de relatórios.",
                    color = if (isTemaClaro) Color.DarkGray else Color.White,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Desenvolvido por:",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = "IORTec",
                    color = if (isTemaClaro) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Contato: israeloliveirarezende@gmail.com",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF539165))
            ) {
                Text("Fechar", color = Color.White, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun BudgetWidget(
    orcamento: Double,
    totalGasto: Double, // Mantido na assinatura para compatibilidade se necessário
    corTema: Long,
    textoPrincipal: Color,
    textoSecundario: Color,
    fundoCaixaInterna: Color,
    fundoPainel: Color,
    onOrcamentoChange: (Double) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var tempInput by remember { mutableStateOf(if (orcamento > 0) orcamento.toString().replace(".", ",") else "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = fundoPainel,
            title = { Text("Definir Orçamento", color = textoPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempInput,
                    onValueChange = { tempInput = it },
                    placeholder = { Text("Ex: 3000,00", color = textoSecundario, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textoPrincipal,
                        unfocusedTextColor = textoPrincipal,
                        focusedBorderColor = Color(corTema),
                        unfocusedBorderColor = textoSecundario,
                        focusedContainerColor = fundoCaixaInterna,
                        unfocusedContainerColor = fundoCaixaInterna
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val novoVal = tempInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onOrcamentoChange(novoVal)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(corTema))
                ) {
                    Text("Salvar", color = Color.White, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = textoSecundario, fontSize = 11.sp)
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fundoCaixaInterna, shape = RoundedCornerShape(6.dp))
            .clickable { showEditDialog = true }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Orçamento Mensal", color = textoSecundario, fontSize = 10.sp)
            Text(
                text = if (orcamento > 0) formatarMoeda(orcamento) else "Toque para definir",
                color = textoPrincipal,
                fontSize = 14.sp, // <-- FONTE AUMENTADA PARA DAR DESTAQUE
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

// --- DIÁLOGO CONFIGURAÇÕES MODIFICADO ---
@Composable
fun SettingsDialog(
    corAtual: Long,
    modoAtual: String,
    isTemaClaro: Boolean,
    onDismiss: () -> Unit,
    onCorChange: (Long) -> Unit,
    onModoChange: (String) -> Unit,
    onTemaClaroChange: (Boolean) -> Unit
) {
    val coresDisponiveis = listOf(
        0xFF539165 to "Verde",
        0xFF3B82F6 to "Azul",
        0xFF8B5CF6 to "Roxo",
        0xFFF97316 to "Laranja"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isTemaClaro) Color.White else Color(0xFF1E2024),
        title = {
            Text("Configurações", color = if (isTemaClaro) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Opção de Tema (Claro / Escuro)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Aparência do App", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isTemaClaro,
                            onClick = { onTemaClaroChange(false) },
                            label = { Text("Escuro") }
                        )
                        FilterChip(
                            selected = isTemaClaro,
                            onClick = { onTemaClaroChange(true) },
                            label = { Text("Claro") }
                        )
                    }
                }

                HorizontalDivider(color = if (isTemaClaro) Color.LightGray else Color(0xFF282B30), thickness = 1.dp)

                // Modo de Exibição
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Modo de Exibição da Tela", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    val modos = listOf(
                        "AMBAS" to "Ambas (Dividido)",
                        "CALCULADORA" to "Somente Calculadora",
                        "DESPESAS" to "Somente Despesas"
                    )

                    modos.forEach { (chave, rotulo) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModoChange(chave) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (modoAtual == chave),
                                onClick = { onModoChange(chave) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(corAtual))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(rotulo, color = if (isTemaClaro) Color.Black else Color.White, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = if (isTemaClaro) Color.LightGray else Color(0xFF282B30), thickness = 1.dp)

                // Cor de Destaque
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cor de Destaque", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        coresDisponiveis.forEach { (hexColor, _) ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(hexColor), shape = RoundedCornerShape(19.dp))
                                    .clickable { onCorChange(hexColor) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (corAtual == hexColor) {
                                    Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(corAtual))
            ) {
                Text("Concluir", color = Color.White, fontSize = 12.sp)
            }
        }
    )
}