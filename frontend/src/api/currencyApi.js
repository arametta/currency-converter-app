// The only module that talks to the backend: fetch calls plus handling of
// its uniform error envelope, so callers just get data or an Error.
const API_BASE = 'http://localhost:8080'

export function fetchCurrencies() {
  return request('/api/currencies')
}

export function convert({ amount, sourceCurrency, targetCurrency }) {
  return request('/api/convert', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount, sourceCurrency, targetCurrency })
  })
}

async function request(path, options) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, options)
  } catch {
    // Network failure, CORS rejection, DNS — any pre-response error.
    throw new Error('Network error — is the API running on :8080?')
  }

  // Error bodies are not guaranteed to be JSON (e.g. a proxy error page).
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    // Backend's uniform error envelope: { status, errors: [...] }.
    // Joined so a single field can render them inline.
    const errors = body?.errors ?? [`Request failed: ${response.status}`]
    throw new Error(errors.join('; '))
  }
  return body
}
