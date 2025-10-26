// State Management
const state = {
    history: [],
    apiBaseUrl: 'http://localhost:8080'
};

// DOM Elements
const elements = {
    form: null,
    urlInput: null,
    expiresAtInput: null,
    submitBtn: null,
    loadingState: null,
    resultCard: null,
    errorCard: null,
    shortUrlDisplay: null,
    originalUrlDisplay: null,
    createdAtDisplay: null,
    expiresAtDisplay: null,
    expiresAtValue: null,
    errorMessage: null,
    copyBtn: null,
    historyList: null,
    emptyHistory: null,
    clearHistoryBtn: null,
    toast: null,
    toastMessage: null,
    urlError: null
};

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
    initializeElements();
    loadHistory();
    renderHistory();
    setupEventListeners();
});

// Initialize DOM element references
function initializeElements() {
    elements.form = document.getElementById('shortenForm');
    elements.urlInput = document.getElementById('urlInput');
    elements.expiresAtInput = document.getElementById('expiresAt');
    elements.submitBtn = document.getElementById('submitBtn');
    elements.loadingState = document.getElementById('loadingState');
    elements.resultCard = document.getElementById('resultCard');
    elements.errorCard = document.getElementById('errorCard');
    elements.shortUrlDisplay = document.getElementById('shortUrlDisplay');
    elements.originalUrlDisplay = document.getElementById('originalUrlDisplay');
    elements.createdAtDisplay = document.getElementById('createdAtDisplay');
    elements.expiresAtDisplay = document.getElementById('expiresAtDisplay');
    elements.expiresAtValue = document.getElementById('expiresAtValue');
    elements.errorMessage = document.getElementById('errorMessage');
    elements.copyBtn = document.getElementById('copyBtn');
    elements.historyList = document.getElementById('historyList');
    elements.emptyHistory = document.getElementById('emptyHistory');
    elements.clearHistoryBtn = document.getElementById('clearHistoryBtn');
    elements.toast = document.getElementById('toast');
    elements.toastMessage = document.getElementById('toastMessage');
    elements.urlError = document.getElementById('urlError');
}

// Setup event listeners
function setupEventListeners() {
    elements.form.addEventListener('submit', handleSubmit);
    elements.copyBtn.addEventListener('click', copyToClipboard);
    elements.clearHistoryBtn.addEventListener('click', clearHistory);
    elements.urlInput.addEventListener('input', clearValidationError);
}

// Validate URL
function validateUrl(url) {
    if (!url || url.trim() === '') {
        return { valid: false, error: 'URL cannot be blank' };
    }

    if (url.length > 255) {
        return { valid: false, error: 'URL cannot exceed 255 characters' };
    }

    if (!url.match(/^https?:\/\/.+/)) {
        return { valid: false, error: 'URL must start with http:// or https://' };
    }

    return { valid: true };
}

// Clear validation error
function clearValidationError() {
    elements.urlError.classList.add('hidden');
    elements.urlInput.classList.remove('border-red-500');
}

// Show validation error
function showValidationError(error) {
    elements.urlError.textContent = error;
    elements.urlError.classList.remove('hidden');
    elements.urlInput.classList.add('border-red-500');
}

// Handle form submission
async function handleSubmit(e) {
    e.preventDefault();

    // Clear previous states
    hideResult();
    hideError();
    clearValidationError();

    // Get form values
    const url = elements.urlInput.value.trim();
    const expiresAt = elements.expiresAtInput.value;

    // Validate URL
    const validation = validateUrl(url);
    if (!validation.valid) {
        showValidationError(validation.error);
        return;
    }

    // Show loading state
    showLoading();

    // Prepare request
    const requestBody = {
        url: url
    };

    if (expiresAt) {
        requestBody.expiresAt = expiresAt;
    }

    try {
        // Call API
        const response = await fetch(`${state.apiBaseUrl}/api/shorten`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        const data = await response.json();

        if (!response.ok) {
            // Handle validation errors from backend
            if (data.errors && data.errors.url) {
                showValidationError(data.errors.url);
            } else {
                showError(data.message || 'Failed to shorten URL');
            }
            return;
        }

        // Success - show result
        showResult(data);

        // Add to history
        addToHistory(data);

        // Reset form
        elements.form.reset();

    } catch (error) {
        console.error('Error:', error);
        showError('Network error. Please check if the backend is running.');
    } finally {
        hideLoading();
    }
}

// Show loading state
function showLoading() {
    elements.submitBtn.disabled = true;
    elements.loadingState.classList.remove('hidden');
}

// Hide loading state
function hideLoading() {
    elements.submitBtn.disabled = false;
    elements.loadingState.classList.add('hidden');
}

// Show result
function showResult(data) {
    elements.shortUrlDisplay.value = data.shortUrl;
    elements.originalUrlDisplay.textContent = truncateUrl(data.originalUrl, 50);
    elements.createdAtDisplay.textContent = formatDateTime(data.createdAt);

    if (data.expiresAt) {
        elements.expiresAtValue.textContent = formatDateTime(data.expiresAt);
        elements.expiresAtDisplay.classList.remove('hidden');
    } else {
        elements.expiresAtDisplay.classList.add('hidden');
    }

    elements.resultCard.classList.remove('hidden');
}

// Hide result
function hideResult() {
    elements.resultCard.classList.add('hidden');
}

// Show error
function showError(message) {
    elements.errorMessage.textContent = message;
    elements.errorCard.classList.remove('hidden');
}

// Hide error
function hideError() {
    elements.errorCard.classList.add('hidden');
}

// Copy to clipboard
async function copyToClipboard() {
    const shortUrl = elements.shortUrlDisplay.value;

    try {
        await navigator.clipboard.writeText(shortUrl);
        showToast('Copied to clipboard!');
    } catch (error) {
        console.error('Failed to copy:', error);
        // Fallback for older browsers
        elements.shortUrlDisplay.select();
        document.execCommand('copy');
        showToast('Copied to clipboard!');
    }
}

// Show toast notification
function showToast(message) {
    elements.toastMessage.textContent = message;
    elements.toast.classList.remove('hidden');

    setTimeout(() => {
        elements.toast.classList.add('hidden');
    }, 3000);
}

// Add to history
function addToHistory(data) {
    const historyItem = {
        shortCode: data.shortCode,
        shortUrl: data.shortUrl,
        originalUrl: data.originalUrl,
        createdAt: data.createdAt,
        expiresAt: data.expiresAt,
        timestamp: new Date().toISOString()
    };

    // Add to beginning of array
    state.history.unshift(historyItem);

    // Limit to 10 items
    if (state.history.length > 10) {
        state.history = state.history.slice(0, 10);
    }

    // Save to localStorage
    saveHistory();

    // Re-render
    renderHistory();
}

// Load history from localStorage
function loadHistory() {
    try {
        const saved = localStorage.getItem('shortifier_history');
        if (saved) {
            state.history = JSON.parse(saved);
        }
    } catch (error) {
        console.error('Failed to load history:', error);
        state.history = [];
    }
}

// Save history to localStorage
function saveHistory() {
    try {
        localStorage.setItem('shortifier_history', JSON.stringify(state.history));
    } catch (error) {
        console.error('Failed to save history:', error);
    }
}

// Clear history
function clearHistory() {
    if (confirm('Are you sure you want to clear all history?')) {
        state.history = [];
        saveHistory();
        renderHistory();
        showToast('History cleared');
    }
}

// Render history
function renderHistory() {
    if (state.history.length === 0) {
        elements.emptyHistory.classList.remove('hidden');
        elements.historyList.classList.add('hidden');
        elements.historyList.innerHTML = '';
        return;
    }

    elements.emptyHistory.classList.add('hidden');
    elements.historyList.classList.remove('hidden');

    elements.historyList.innerHTML = state.history.map((item, index) => {
        const isExpired = item.expiresAt && new Date(item.expiresAt) < new Date();

        return `
            <div class="border border-gray-200 rounded-lg p-4 hover:border-blue-300 transition ${isExpired ? 'opacity-60' : ''}">
                <div class="flex items-start justify-between">
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center space-x-2 mb-2">
                            <span class="font-mono text-sm text-blue-600 truncate">${item.shortUrl}</span>
                            <button
                                onclick="copyHistoryUrl('${item.shortUrl}')"
                                class="text-gray-400 hover:text-blue-600 transition"
                                title="Copy"
                            >
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                                </svg>
                            </button>
                        </div>
                        <p class="text-sm text-gray-600 truncate mb-1">
                            ${truncateUrl(item.originalUrl, 60)}
                        </p>
                        <div class="flex items-center space-x-4 text-xs text-gray-500">
                            <span>${formatRelativeTime(item.createdAt)}</span>
                            ${item.expiresAt ? `
                                <span class="${isExpired ? 'text-red-600 font-semibold' : 'text-orange-600'}">
                                    ${isExpired ? 'Expired' : 'Expires ' + formatRelativeTime(item.expiresAt)}
                                </span>
                            ` : ''}
                        </div>
                    </div>
                    <button
                        onclick="deleteHistoryItem(${index})"
                        class="ml-4 text-gray-400 hover:text-red-600 transition"
                        title="Delete"
                    >
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                        </svg>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

// Copy history URL
function copyHistoryUrl(url) {
    navigator.clipboard.writeText(url)
        .then(() => showToast('Copied to clipboard!'))
        .catch(err => console.error('Failed to copy:', err));
}

// Delete history item
function deleteHistoryItem(index) {
    state.history.splice(index, 1);
    saveHistory();
    renderHistory();
    showToast('Item deleted');
}

// Utility: Format date/time
function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Utility: Format relative time
function formatRelativeTime(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 30) return `${diffDays}d ago`;

    return formatDateTime(dateString);
}

// Utility: Truncate URL
function truncateUrl(url, maxLength) {
    if (url.length <= maxLength) return url;
    return url.substring(0, maxLength) + '...';
}

// Make functions globally available for inline onclick handlers
window.copyHistoryUrl = copyHistoryUrl;
window.deleteHistoryItem = deleteHistoryItem;
