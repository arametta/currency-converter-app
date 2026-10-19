import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCurrencies } from '../api/currencyApi'
import { useCurrencies } from './useCurrencies'

vi.mock('../api/currencyApi')

describe('useCurrencies', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('starts in the loading state', () => {
    const { currencies, loading, error } = useCurrencies()

    expect(loading.value).toBe(true)
    expect(error.value).toBe(false)
    expect(currencies.value).toEqual([])
  })

  it('stores the list and reports success', async () => {
    const list = [{ code: 'EUR', name: 'Euro' }]
    fetchCurrencies.mockResolvedValue(list)
    const { currencies, loading, error, load } = useCurrencies()

    await expect(load()).resolves.toBe(true)

    expect(currencies.value).toEqual(list)
    expect(loading.value).toBe(false)
    expect(error.value).toBe(false)
  })

  it('flags the error and reports failure when the list cannot be loaded', async () => {
    fetchCurrencies.mockRejectedValue(new Error('Exchange rate provider is unreachable'))
    const { currencies, loading, error, load } = useCurrencies()

    await expect(load()).resolves.toBe(false)

    expect(currencies.value).toEqual([])
    expect(loading.value).toBe(false)
    expect(error.value).toBe(true)
  })
})
