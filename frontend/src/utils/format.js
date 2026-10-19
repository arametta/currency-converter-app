// Browser-side locale-aware formatting via Intl.NumberFormat.
// undefined locale = use the browser's default.
export function formatCurrency(amount, currency, locale = undefined) {
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amount)
}
