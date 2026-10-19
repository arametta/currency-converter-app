// Mirrors the backend constraints on ConversionRequest.amount
// (@NotNull, @Positive, @DecimalMax), so invalid input is caught before a request.
export const MAX_AMOUNT = 999999999999.99

// Returns an error message, or null when the amount is valid.
export function validateAmount(amount) {
  // v-model.number leaves an empty or unparseable input as a string.
  if (amount === '' || amount === null || amount === undefined) {
    return 'Amount is required'
  }
  if (typeof amount !== 'number' || Number.isNaN(amount)) {
    return 'Amount must be a number'
  }
  if (amount <= 0) {
    return 'Amount must be greater than 0'
  }
  if (amount > MAX_AMOUNT) {
    return 'Amount must be at most 999,999,999,999.99'
  }
  return null
}
