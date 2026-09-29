import { useState, type FormEvent } from 'react'
import { X } from 'lucide-react'
import type { Category, CreateProductInput } from './api'

type Props = {
  busy: boolean
  onClose: () => void
  onCreate: (product: CreateProductInput) => Promise<void>
}

const initialValues: CreateProductInput = {
  sku: '',
  name: '',
  category: 'ELECTRONICS',
  currentPrice: 0,
  stockLevel: 0,
  reorderThreshold: 0,
}

function NewProductDialog({ busy, onClose, onCreate }: Props) {
  const [values, setValues] = useState(initialValues)

  const update = <Key extends keyof CreateProductInput>(key: Key, value: CreateProductInput[Key]) => {
    setValues((current) => ({ ...current, [key]: value }))
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await onCreate({ ...values, sku: values.sku.trim(), name: values.name.trim() })
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.currentTarget === event.target && !busy) onClose()
    }}>
      <section className="product-dialog" role="dialog" aria-modal="true" aria-labelledby="new-product-title">
        <header className="dialog-header">
          <div>
            <span className="eyebrow">CATALOG</span>
            <h2 id="new-product-title">Add product</h2>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Close" title="Close">
            <X size={18} />
          </button>
        </header>
        <form onSubmit={(event) => void submit(event)}>
          <label>
            SKU
            <input required maxLength={80} value={values.sku} onChange={(event) => update('sku', event.target.value)} autoFocus />
          </label>
          <label>
            Product name
            <input required maxLength={160} value={values.name} onChange={(event) => update('name', event.target.value)} />
          </label>
          <label>
            Category
            <select value={values.category} onChange={(event) => update('category', event.target.value as Category)}>
              <option value="ELECTRONICS">Electronics</option>
              <option value="APPAREL">Apparel</option>
              <option value="HOME">Home</option>
            </select>
          </label>
          <div className="form-grid">
            <label>
              Current price
              <input required type="number" min="0.01" step="0.01" value={values.currentPrice || ''} onChange={(event) => update('currentPrice', Number(event.target.value))} />
            </label>
            <label>
              Units in stock
              <input required type="number" min="0" step="1" value={values.stockLevel} onChange={(event) => update('stockLevel', Number(event.target.value))} />
            </label>
          </div>
          <label>
            Reorder threshold
            <input required type="number" min="0" step="1" value={values.reorderThreshold} onChange={(event) => update('reorderThreshold', Number(event.target.value))} />
          </label>
          <footer className="dialog-actions">
            <button className="button button-quiet" type="button" onClick={onClose} disabled={busy}>Cancel</button>
            <button className="button button-primary" type="submit" disabled={busy}>
              {busy ? 'Adding…' : 'Add product'}
            </button>
          </footer>
        </form>
      </section>
    </div>
  )
}

export default NewProductDialog
