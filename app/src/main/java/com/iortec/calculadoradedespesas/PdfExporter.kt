package com.iortec.calculadoradedespesas

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun exportarRelatorioPdf(
    context: Context,
    monthDisplay: String,
    expenses: List<Expense>,
    orcamento: Double = 0.0 // <-- Recebe o Orçamento
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamanho A4
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint()
    val titlePaint = Paint()
    val headerPaint = Paint()

    // Configurações do Título Principal
    titlePaint.color = Color.rgb(40, 43, 48)
    titlePaint.textSize = 20f
    titlePaint.isFakeBoldText = true

    // Cabeçalho Principal
    canvas.drawText("Relatório Financeiro - $monthDisplay", 40f, 50f, titlePaint)

    // Linha Divisória Principal
    paint.color = Color.rgb(83, 145, 101)
    paint.strokeWidth = 2f
    canvas.drawLine(40f, 62f, 555f, 62f, paint)

    // --- BLOCO RESUMO DO ORÇAMENTO (Sua nova seção) ---
    val totalDespesas = expenses.sumOf { it.value }
    val saldoRestante = orcamento - totalDespesas

    val boxPaint = Paint().apply {
        color = Color.rgb(245, 247, 245)
        style = Paint.Style.FILL
    }
    // Desenha um card de fundo para o resumo
    canvas.drawRect(40f, 75f, 555f, 130f, boxPaint)

    val labelPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 10f
    }
    val valuePaint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
        isFakeBoldText = true
    }

    // Coluna 1: Orçamento
    canvas.drawText("ORÇAMENTO", 55f, 95f, labelPaint)
    canvas.drawText(formatarMoeda(orcamento), 55f, 115f, valuePaint)

    // Coluna 2: Total Gasto
    canvas.drawText("TOTAL GASTO", 230f, 95f, labelPaint)
    valuePaint.color = Color.rgb(180, 40, 40) // Vermelho suave para gastos
    canvas.drawText(formatarMoeda(totalDespesas), 230f, 115f, valuePaint)

    // Coluna 3: Saldo Restante
    canvas.drawText("SALDO / SOBRA", 410f, 95f, labelPaint)
    valuePaint.color = if (saldoRestante >= 0) Color.rgb(40, 140, 60) else Color.rgb(200, 0, 0)
    canvas.drawText(formatarMoeda(saldoRestante), 410f, 115f, valuePaint)

    // --- TABELA DE DESPESAS ---
    headerPaint.color = Color.DKGRAY
    headerPaint.textSize = 11f
    headerPaint.isFakeBoldText = true

    var y = 160f
    canvas.drawText("Nome da Despesa", 40f, y, headerPaint)
    canvas.drawText("Data", 350f, y, headerPaint)
    canvas.drawText("Valor", 460f, y, headerPaint)

    // Linha Divisória da Tabela
    paint.color = Color.LTGRAY
    paint.strokeWidth = 1f
    y += 8f
    canvas.drawLine(40f, y, 555f, y, paint)

    paint.color = Color.BLACK
    paint.textSize = 10f

    // Desenha cada despesa da lista
    for (expense in expenses) {
        y += 22f

        val titleText = if (expense.title.length > 35) {
            expense.title.take(32) + "..."
        } else {
            expense.title
        }

        canvas.drawText(titleText, 40f, y, paint)
        canvas.drawText(expense.date, 350f, y, paint)
        canvas.drawText(formatarMoeda(expense.value), 460f, y, paint)

        if (y > 780f) break // Limite de altura A4
    }

    pdfDocument.finishPage(page)

    // Salva o arquivo na memória temporária do dispositivo
    val file = File(context.cacheDir, "Relatorio_Despesas_${monthDisplay.replace(" / ", "_")}.pdf")
    try {
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        abrirOuCompartilharPdf(context, file)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun abrirOuCompartilharPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Compartilhar Relatório PDF")
    context.startActivity(chooser)
}