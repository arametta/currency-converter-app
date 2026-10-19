import { ref } from 'vue'
import { convert as requestConversion } from '../api/currencyApi'

// Form values plus the state of the latest conversion request.
export function useConversion() {
  const source = ref('')
  const target = ref('')
  const amount = ref(100)

  const converting = ref(false)
  const result = ref(null)
  const error = ref(null)

  async function convert() {
    // Clear any previous result/error so the UI doesn't show stale state
    // while the new request is in flight.
    converting.value = true
    result.value = null
    error.value = null

    try {
      result.value = await requestConversion({
        amount: amount.value,
        sourceCurrency: source.value,
        targetCurrency: target.value
      })
    } catch (e) {
      error.value = e.message
    } finally {
      converting.value = false
    }
  }

  // Flip source ↔ target. Common UX pattern in currency converters — after
  // seeing USD→EUR a user often wants EUR→USD next. The previous result is
  // cleared because it's now for the opposite direction and showing it
  // alongside the new dropdown values would be misleading.
  function swap() {
    [source.value, target.value] = [target.value, source.value]
    result.value = null
    error.value = null
  }

  return { source, target, amount, converting, result, error, convert, swap }
}
