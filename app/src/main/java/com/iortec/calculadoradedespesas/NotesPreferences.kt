package com.iortec.calculadoradedespesas

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Declaração do DataStore ligada ao Context do Android
val Context.dataStore by preferencesDataStore(name = "user_notes_prefs")

class NotesPreferences(private val appContext: Context) {

    private val gson = Gson()

    companion object {
        private val ANOTACOES_KEY = stringPreferencesKey("anotacoes_texto")
        private val DESPESAS_KEY = stringPreferencesKey("despesas_lista")
        private val ORCAMENTO_KEY = stringPreferencesKey("orcamento_mensal")
        private val COR_TEMA_KEY = stringPreferencesKey("cor_tema_hex")
        private val MODO_EXIBICAO_KEY = stringPreferencesKey("modo_exibicao_app")
        private val TEMA_CLARO_KEY = booleanPreferencesKey("is_tema_claro") // Chave do Tema Claro no companion object
    }

    // --- ANOTAÇÕES ---
    val anotacoesFlow: Flow<String> = appContext.dataStore.data
        .map { preferences ->
            preferences[ANOTACOES_KEY] ?: ""
        }

    suspend fun salvarAnotacoes(texto: String) {
        appContext.dataStore.edit { preferences ->
            preferences[ANOTACOES_KEY] = texto
        }
    }

    // --- DESPESAS ---
    val despesasFlow: Flow<List<Expense>> = appContext.dataStore.data
        .map { preferences ->
            val json = preferences[DESPESAS_KEY] ?: ""
            if (json.isEmpty()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<Expense>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun salvarDespesas(lista: List<Expense>) {
        val json = gson.toJson(lista)
        appContext.dataStore.edit { preferences ->
            preferences[DESPESAS_KEY] = json
        }
    }

    // --- ORÇAMENTO ---
    val orcamentoFlow: Flow<Double> = appContext.dataStore.data
        .map { preferences ->
            preferences[ORCAMENTO_KEY]?.toDoubleOrNull() ?: 0.0
        }

    suspend fun salvarOrcamento(valor: Double) {
        appContext.dataStore.edit { preferences ->
            preferences[ORCAMENTO_KEY] = valor.toString()
        }
    }

    // --- COR DO TEMA ---
    val corTemaFlow: Flow<Long> = appContext.dataStore.data
        .map { preferences ->
            preferences[COR_TEMA_KEY]?.toLongOrNull() ?: 0xFF539165
        }

    suspend fun salvarCorTema(corHex: Long) {
        appContext.dataStore.edit { preferences ->
            preferences[COR_TEMA_KEY] = corHex.toString()
        }
    }

    // --- MODO DE EXIBIÇÃO ---
    val modoExibicaoFlow: Flow<String> = appContext.dataStore.data
        .map { preferences ->
            preferences[MODO_EXIBICAO_KEY] ?: "AMBAS"
        }

    suspend fun salvarModoExibicao(modo: String) {
        appContext.dataStore.edit { preferences ->
            preferences[MODO_EXIBICAO_KEY] = modo
        }
    }

    // --- TEMA CLARO ---
    val temaClaroFlow: Flow<Boolean> = appContext.dataStore.data
        .map { preferences ->
            preferences[TEMA_CLARO_KEY] ?: false
        }

    suspend fun salvarTemaClaro(isClaro: Boolean) {
        appContext.dataStore.edit { preferences ->
            preferences[TEMA_CLARO_KEY] = isClaro
        }
    }
}