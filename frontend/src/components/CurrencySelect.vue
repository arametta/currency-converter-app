<script setup>
defineProps({
  id: { type: String, required: true },
  label: { type: String, required: true },
  currencies: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: Boolean, default: false }
})

// v-model support: the modelValue prop and update:modelValue event.
const model = defineModel({ type: String, default: '' })
</script>

<template>
  <div class="field">
    <label :for="id">{{ label }}</label>
    <select :id="id" v-model="model">
      <option v-if="loading" value="">Loading...</option>
      <option v-else-if="error" value="">Could not load currencies</option>
      <option v-for="c in currencies" :key="c.code" :value="c.code">
        {{ c.code }} — {{ c.name }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.field {
  display: grid;
  gap: 0.25rem;
}

label {
  font-size: 0.875rem;
  color: #555;
}

select {
  padding: 0.5rem;
  font-size: 1rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: white;
}
</style>
