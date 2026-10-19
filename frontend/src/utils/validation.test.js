import { describe, expect, it } from 'vitest'
import { MAX_AMOUNT, validateAmount } from './validation'

describe('validateAmount', () => {
  it.each(['', null, undefined])('requires an amount (%s)', amount => {
    expect(validateAmount(amount)).toBe('Amount is required')
  })

  it('rejects a value that is not a number', () => {
    expect(validateAmount('abc')).toBe('Amount must be a number')
    expect(validateAmount(Number.NaN)).toBe('Amount must be a number')
  })

  it.each([0, -1])('rejects %s, which is not greater than 0', amount => {
    expect(validateAmount(amount)).toBe('Amount must be greater than 0')
  })

  it('rejects an amount above the cap', () => {
    expect(validateAmount(1000000000000)).toBe('Amount must be at most 999,999,999,999.99')
  })

  it.each([0.001, 100, MAX_AMOUNT])('accepts %s', amount => {
    expect(validateAmount(amount)).toBeNull()
  })
})
