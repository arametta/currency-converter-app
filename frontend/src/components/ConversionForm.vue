<script setup>
import CurrencySelect from './CurrencySelect.vue'

defineProps({
  currencies: { type: Array, default: () => [] },
  loadingCurrencies: { type: Boolean, default: false },
  currenciesError: { type: Boolean, default: false },
  converting: { type: Boolean, default: false }
})

defineEmits(['submit', 'swap'])

const source = defineModel('source', { type: String, default: '' })
const target = defineModel('target', { type: String, default: '' })
// String while the number input is empty, so both types are valid.
const amount = defineModel('amount', { type: [Number, String], default: '' })
</script>

<template>
  <form @submit.prevent="$emit('submit')">
    <CurrencySelect
      id="source"
      v-model="source"
      label="From"
      :currencies="currencies"
      :loading="loadingCurrencies"
      :error="currenciesError"
    />

    <button
      type="button"
      class="swap"
      @click="$emit('swap')"
      :disabled="converting || loadingCurrencies"
      aria-label="Swap source and target currencies"
      title="Swap currencies"
    >
      ⇅
    </button>

    <CurrencySelect
      id="target"
      v-model="target"
      label="To"
      :currencies="currencies"
      :loading="loadingCurrencies"
      :error="currenciesError"
    />

    <div class="field">
      <label for="amount">Amount</label>
      <input id="amount" type="number" v-model.number="amount" min="0" max="999999999999.99" step="0.001" />
    </div>

    <button type="submit" :disabled="converting">
      {{ converting ? 'Converting...' : 'Convert' }}
    </button>
  </form>
</template>

<style scoped>
form {
  display: grid;
  gap: 1rem;
}

.field {
  display: grid;
  gap: 0.25rem;
}

label {
  font-size: 0.875rem;
  color: #555;
}

input {
  padding: 0.5rem;
  font-size: 1rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: white;
}

button {
  padding: 0.6rem;
  font-size: 1rem;
  background: #1f6feb;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  background: #aaa;
  cursor: not-allowed;
}

.swap {
  justify-self: center;
  width: 2.25rem;
  height: 2.25rem;
  padding: 0;
  font-size: 1.1rem;
  background: white;
  color: #1f6feb;
  border: 1px solid #ccc;
  border-radius: 50%;
  margin: -0.25rem 0;
}

.swap:hover:not(:disabled) {
  background: #f5f5f5;
}

.swap:disabled {
  background: white;
  color: #aaa;
  border-color: #ddd;
}
</style>
