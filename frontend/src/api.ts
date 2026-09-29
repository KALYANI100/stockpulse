export type Category = 'ELECTRONICS' | 'APPAREL' | 'HOME'
export type ProductStatus = 'ACTIVE' | 'PRICE_REVIEW_PENDING' | 'OUT_OF_STOCK'
export type TriggerReason = 'INITIAL' | 'INVENTORY_LOW' | 'DEMAND_SPIKE' | 'MANUAL'
export type SuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'
export type SuggestionType = 'PRICING' | 'REORDER'
export type AdvisorMode = 'AUTO' | 'AI' | 'RULES'

export interface Product {
  id: string
  sku: string
  name: string
  category: Category
  currentPrice: number
  stockLevel: number
  reorderThreshold: number
  demandVelocity: number
  status: ProductStatus
}

export interface Suggestion {
  id: string
  productId: string
  productName: string
  type: SuggestionType
  status: SuggestionStatus
  triggerReason: TriggerReason
  confidence: number
  reasoning: string
  currentPrice: number | null
  recommendedPrice: number | null
  direction: 'INCREASE' | 'DECREASE' | 'HOLD' | null
  currentStock: number | null
  recommendedQuantity: number | null
  suggestedLeadTimeDays: number | null
  createdAt: string
}

export interface StrategySettings {
  mode: AdvisorMode
  llmConfigured: boolean
}

export interface CreateProductInput {
  sku: string
  name: string
  category: Category
  currentPrice: number
  stockLevel: number
  reorderThreshold: number
}

export interface ProductMetricsInput {
  demandVelocity: number
  reorderThreshold: number
}

const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, {
    ...init,
    headers: {
      ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null
    throw new Error(body?.detail ?? body?.message ?? `Request failed (${response.status})`)
  }

  if (response.status === 202 || response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  listProducts: () => request<Product[]>('/products'),
  listPendingSuggestions: () => request<Suggestion[]>('/suggestions?status=PENDING'),
  getStrategy: () => request<StrategySettings>('/settings/strategy'),
  setStrategy: (mode: AdvisorMode) => request<StrategySettings>('/settings/strategy', {
    method: 'PATCH',
    body: JSON.stringify({ mode }),
  }),
  createProduct: (product: CreateProductInput) => request<Product>('/products', {
    method: 'POST',
    body: JSON.stringify(product),
  }),
  updateProductMetrics: (productId: string, metrics: ProductMetricsInput) => request<Product>(
    `/products/${productId}/metrics`,
    { method: 'PATCH', body: JSON.stringify(metrics) },
  ),
  updateStock: (productId: string, stockLevel: number) => request<Product>(
    `/products/${productId}/stock`,
    { method: 'PATCH', body: JSON.stringify({ stockLevel }) },
  ),
  simulateSale: (productId: string, quantity = 1) => request<Product>(`/products/${productId}/orders`, {
    method: 'POST',
    body: JSON.stringify({ quantity }),
  }),
  requestAdvice: (productId: string, type: SuggestionType) => request<void>(
    `/products/${productId}/suggest-${type === 'PRICING' ? 'pricing' : 'reorder'}`,
    { method: 'POST' },
  ),
  decideSuggestion: (suggestion: Suggestion, decision: 'ACCEPTED' | 'REJECTED') => request<Suggestion>(
    `/${suggestion.type === 'PRICING' ? 'pricing' : 'reorder'}-suggestions/${suggestion.id}`,
    { method: 'PATCH', body: JSON.stringify({ decision }) },
  ),
}
