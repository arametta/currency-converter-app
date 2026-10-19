<script setup>
import { onMounted } from 'vue'
import ConversionForm from './components/ConversionForm.vue'
import ConversionResult from './components/ConversionResult.vue'
import { useConversion } from './composables/useConversion'
import { useCurrencies } from './composables/useCurrencies'

const {
  currencies,
  loading: loadingCurrencies,
  error: currenciesError,
  load: loadCurrencies
} = useCurrencies()

const {
  source,
  target,
  amount,
  converting,
  result,
  error: convertError,
  convert,
  swap
} = useConversion()

// Pre-selecting EUR → USD only happens on success — on failure the dropdowns
// stay empty and the convert button still works (the backend will return a
// 400 we'll surface), so the failure mode is end-to-end visible.
onMounted(async () => {
  if (await loadCurrencies()) {
    source.value = 'EUR'
    target.value = 'USD'
  }
})
</script>

<template>
  <main>
    <h1>Currency Converter</h1>

    <ConversionForm
      v-model:source="source"
      v-model:target="target"
      v-model:amount="amount"
      :currencies="currencies"
      :loading-currencies="loadingCurrencies"
      :currencies-error="currenciesError"
      :converting="converting"
      @submit="convert"
      @swap="swap"
    />

    <ConversionResult :result="result" :error="convertError" />
  </main>
</template>

<style scoped>
main {
  max-width: 480px;
  margin: 2rem auto;
  padding: 0 1rem;
  font-family: system-ui, -apple-system, sans-serif;
  color: #222;
}

h1 {
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
}
</style>
