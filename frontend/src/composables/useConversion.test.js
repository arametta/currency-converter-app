import { beforeEach, describe, expect, it, vi } from 'vitest'
import { convert as requestConversion } from '../api/currencyApi'
import { useConversion } from './useConversion'

vi.mock('../api/currencyApi')

function deferred() {
  let resolve
  const promise = new Promise(r => {
    resolve = r
  })
  return { promise, resolve }
}

describe('useConversion', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('sends the form values and stores the result', async () => {
    const response = { convertedAmount: 108, targetCurrency: 'USD' }
    requestConversion.mockResolvedValue(response)
    const conversion = useConversion()
    conversion.source.value = 'EUR'
    conversion.target.value = 'USD'
    conversion.amount.value = 100

    await conversion.convert()

    expect(requestConversion).toHaveBeenCalledWith({
      amount: 100,
      sourceCurrency: 'EUR',
      targetCurrency: 'USD'
    })
    expect(conversion.result.value).toEqual(response)
    expect(conversion.error.value).toBeNull()
    expect(conversion.converting.value).toBe(false)
  })

  it('clears the previous result and flags converting while the request is in flight', async () => {
    const pending = deferred()
    requestConversion.mockReturnValue(pending.promise)
    const conversion = useConversion()
    conversion.result.value = { convertedAmount: 1 }
    conversion.error.value = 'old error'

    const done = conversion.convert()

    expect(conversion.converting.value).toBe(true)
    expect(conversion.result.value).toBeNull()
    expect(conversion.error.value).toBeNull()

    pending.resolve({ convertedAmount: 2 })
    await done
    expect(conversion.converting.value).toBe(false)
  })

  it('stores the error message when the request fails', async () => {
    requestConversion.mockRejectedValue(new Error('Currency not supported: XYZ'))
    const conversion = useConversion()

    await conversion.convert()

    expect(conversion.error.value).toBe('Currency not supported: XYZ')
    expect(conversion.result.value).toBeNull()
    expect(conversion.converting.value).toBe(false)
  })

  it('is valid for the default amount', () => {
    const conversion = useConversion()

    expect(conversion.amountError.value).toBeNull()
    expect(conversion.isValid.value).toBe(true)
  })

  it('flags an invalid amount', () => {
    const conversion = useConversion()

    conversion.amount.value = 0

    expect(conversion.amountError.value).toBe('Amount must be greater than 0')
    expect(conversion.isValid.value).toBe(false)
  })

  it('does not send a request while the amount is invalid', async () => {
    const conversion = useConversion()
    conversion.amount.value = ''

    await conversion.convert()

    expect(requestConversion).not.toHaveBeenCalled()
    expect(conversion.converting.value).toBe(false)
  })

  it('swaps source and target and clears the stale result', () => {
    const conversion = useConversion()
    conversion.source.value = 'EUR'
    conversion.target.value = 'USD'
    conversion.result.value = { convertedAmount: 108 }
    conversion.error.value = 'some error'

    conversion.swap()

    expect(conversion.source.value).toBe('USD')
    expect(conversion.target.value).toBe('EUR')
    expect(conversion.result.value).toBeNull()
    expect(conversion.error.value).toBeNull()
  })
})
