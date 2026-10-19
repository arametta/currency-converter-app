import { ref } from 'vue'
import { fetchCurrencies } from '../api/currencyApi'

// Currency list for the dropdowns, with loading and error flags.
export function useCurrencies() {
  const currencies = ref([])
  const loading = ref(true)
  const error = ref(false)

  // Resolves to true on success so the caller can decide what to pre-select.
  async function load() {
    loading.value = true
    error.value = false
    try {
      currencies.value = await fetchCurrencies()
      return true
    } catch {
      error.value = true
      return false
    } finally {
      loading.value = false
    }
  }

  return { currencies, loading, error, load }
}
