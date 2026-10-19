<script setup>
import { computed } from 'vue'
import { formatCurrency } from '../utils/format'

const props = defineProps({
  result: { type: Object, default: null },
  error: { type: String, default: null }
})

const formattedAmount = computed(() =>
  props.result
    ? formatCurrency(props.result.convertedAmount, props.result.targetCurrency)
    : ''
)
</script>

<template>
  <div v-if="result" class="result">
    <p class="amount">{{ formattedAmount }}</p>
    <p class="rate">
      1 {{ result.sourceCurrency }} = {{ result.exchangeRate }} {{ result.targetCurrency }}
    </p>
  </div>

  <div v-if="error" class="error">{{ error }}</div>
</template>

<style scoped>
.result {
  margin-top: 1.5rem;
  padding: 1rem;
  background: #f5f5f5;
  border-radius: 4px;
}

.amount {
  font-size: 1.5rem;
  font-weight: 600;
  margin: 0;
}

.rate {
  margin: 0.5rem 0 0 0;
  color: #666;
  font-size: 0.875rem;
}

.error {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #ffebee;
  color: #c62828;
  border-radius: 4px;
  font-size: 0.875rem;
}
</style>
