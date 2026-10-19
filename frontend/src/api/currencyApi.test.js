import { afterEach, describe, expect, it, vi } from 'vitest'
import { convert, fetchCurrencies } from './currencyApi'

function jsonResponse(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

function mockFetch(implementation) {
  const fetchMock = vi.fn(implementation)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

describe('fetchCurrencies', () => {
  it('returns the currency list', async () => {
    const currencies = [{ code: 'EUR', name: 'Euro' }]
    const fetchMock = mockFetch(async () => jsonResponse(200, currencies))

    await expect(fetchCurrencies()).resolves.toEqual(currencies)
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/currencies', undefined)
  })

  it('throws the messages from the error envelope', async () => {
    mockFetch(async () => jsonResponse(503, {
      status: 503,
      errors: ['Exchange rate provider is unreachable']
    }))

    await expect(fetchCurrencies()).rejects.toThrow('Exchange rate provider is unreachable')
  })
})

describe('convert', () => {
  it('posts the request as JSON and returns the result', async () => {
    const result = { convertedAmount: 108, targetCurrency: 'USD' }
    const fetchMock = mockFetch(async () => jsonResponse(200, result))

    await expect(convert({ amount: 100, sourceCurrency: 'EUR', targetCurrency: 'USD' }))
      .resolves.toEqual(result)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/convert')
    expect(options.method).toBe('POST')
    expect(options.headers).toEqual({ 'Content-Type': 'application/json' })
    expect(JSON.parse(options.body))
      .toEqual({ amount: 100, sourceCurrency: 'EUR', targetCurrency: 'USD' })
  })

  it('joins all messages from the error envelope', async () => {
    mockFetch(async () => jsonResponse(400, {
      status: 400,
      errors: ['amount: amount must be positive', 'sourceCurrency: sourceCurrency must be alphabetic']
    }))

    await expect(convert({ amount: -1, sourceCurrency: '123', targetCurrency: 'USD' }))
      .rejects.toThrow('amount: amount must be positive; sourceCurrency: sourceCurrency must be alphabetic')
  })

  it('falls back to the status code when the error body is not JSON', async () => {
    mockFetch(async () => new Response('Bad Gateway', { status: 502 }))

    await expect(convert({ amount: 1, sourceCurrency: 'EUR', targetCurrency: 'USD' }))
      .rejects.toThrow('Request failed: 502')
  })

  it('reports a network error when the request never gets a response', async () => {
    mockFetch(async () => {
      throw new TypeError('Failed to fetch')
    })

    await expect(convert({ amount: 1, sourceCurrency: 'EUR', targetCurrency: 'USD' }))
      .rejects.toThrow('Network error — is the API running at http://localhost:8080?')
  })
})

describe('API base URL', () => {
  // API_BASE is read when the module loads, so re-import after stubbing.
  async function importWithEnv(value) {
    vi.stubEnv('VITE_API_BASE', value)
    vi.resetModules()
    return import('./currencyApi')
  }

  it('uses VITE_API_BASE when set', async () => {
    const { fetchCurrencies } = await importWithEnv('https://api.example.com')
    const fetchMock = mockFetch(async () => jsonResponse(200, []))

    await fetchCurrencies()

    expect(fetchMock).toHaveBeenCalledWith('https://api.example.com/api/currencies', undefined)
  })

  it('ignores a trailing slash in VITE_API_BASE', async () => {
    const { fetchCurrencies } = await importWithEnv('https://api.example.com/')
    const fetchMock = mockFetch(async () => jsonResponse(200, []))

    await fetchCurrencies()

    expect(fetchMock).toHaveBeenCalledWith('https://api.example.com/api/currencies', undefined)
  })

  it('falls back to localhost:8080 when VITE_API_BASE is empty', async () => {
    const { fetchCurrencies } = await importWithEnv('')
    const fetchMock = mockFetch(async () => jsonResponse(200, []))

    await fetchCurrencies()

    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/currencies', undefined)
  })
})
