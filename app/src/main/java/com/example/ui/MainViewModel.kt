package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyEntity
import com.example.data.ChatRepository
import com.example.data.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ChatRepository) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiKeys: StateFlow<List<ApiKeyEntity>> = repository.allApiKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _activeProvider = MutableStateFlow<ApiKeyEntity?>(null)
    val activeProvider = _activeProvider.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allApiKeys.collect { keys ->
                _activeProvider.value = keys.find { it.isActive }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            repository.sendMessage(prompt.trim())
            _isGenerating.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun addApiKey(provider: String, key: String) {
        viewModelScope.launch {
            repository.insertApiKey(provider, key)
        }
    }

    fun deleteApiKey(provider: String) {
        viewModelScope.launch {
            repository.deleteApiKey(provider)
        }
    }

    fun setActiveProvider(provider: String) {
        viewModelScope.launch {
            repository.setActiveProvider(provider)
        }
    }
}

class MainViewModelFactory(
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
