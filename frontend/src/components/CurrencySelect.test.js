// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CurrencySelect from './CurrencySelect.vue'

const currencies = [
  { code: 'EUR', name: 'Euro' },
  { code: 'USD', name: 'United States dollar' }
]

function mountSelect(props = {}) {
  return mount(CurrencySelect, {
    props: { id: 'source', label: 'From', currencies, ...props }
  })
}

describe('CurrencySelect', () => {
  it('links the label to the select', () => {
    const wrapper = mountSelect()

    expect(wrapper.get('label').text()).toBe('From')
    expect(wrapper.get('label').attributes('for')).toBe('source')
    expect(wrapper.get('select').attributes('id')).toBe('source')
  })

  it('renders one option per currency', () => {
    const options = mountSelect().findAll('option')

    expect(options.map(o => o.text())).toEqual(['EUR — Euro', 'USD — United States dollar'])
    expect(options.map(o => o.element.value)).toEqual(['EUR', 'USD'])
  })

  it('selects the option matching modelValue', () => {
    const wrapper = mountSelect({ modelValue: 'USD' })

    expect(wrapper.get('select').element.value).toBe('USD')
  })

  it('emits update:modelValue when the user picks a currency', async () => {
    const wrapper = mountSelect({ modelValue: 'EUR' })

    await wrapper.get('select').setValue('USD')

    expect(wrapper.emitted('update:modelValue')).toEqual([['USD']])
  })

  it('shows a loading placeholder while currencies load', () => {
    const wrapper = mountSelect({ currencies: [], loading: true })

    expect(wrapper.findAll('option').map(o => o.text())).toEqual(['Loading...'])
  })

  it('shows an error placeholder when currencies could not be loaded', () => {
    const wrapper = mountSelect({ currencies: [], error: true })

    expect(wrapper.findAll('option').map(o => o.text())).toEqual(['Could not load currencies'])
  })
})
