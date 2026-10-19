// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ConversionForm from './ConversionForm.vue'

function mountForm(props = {}) {
  return mount(ConversionForm, {
    props: { source: 'EUR', target: 'USD', amount: 100, ...props }
  })
}

describe('ConversionForm', () => {
  it('enables Convert and shows no message for a valid form', () => {
    const wrapper = mountForm()

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('#amount-error').exists()).toBe(false)
    expect(wrapper.get('#amount').attributes('aria-invalid')).toBe('false')
  })

  it('disables Convert and shows the message when the amount is invalid', () => {
    const wrapper = mountForm({ amount: 0, amountError: 'Amount must be greater than 0' })

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('#amount-error').text()).toBe('Amount must be greater than 0')
  })

  it('links the message to the input for screen readers', () => {
    const input = mountForm({ amount: 0, amountError: 'Amount must be greater than 0' }).get('#amount')

    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe('amount-error')
  })

  it('does not allow 0 through the input constraints', () => {
    expect(mountForm().get('#amount').attributes('min')).toBe('0.001')
  })
})
