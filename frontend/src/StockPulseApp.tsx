import { useCallback, useEffect, useEffectEvent, useMemo, useState } from 'react'
import {
  Activity,
  ArrowRight,
  ArrowDownRight,
  ArrowUpRight,
  Boxes,
  Check,
  CircleAlert,
  CircleCheck,
  CircleDollarSign,
  Clock3,
  House,
  LoaderCircle,
  Package,
  PackagePlus,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  ShoppingCart,
  SlidersHorizontal,
  Sparkles,
  X,
} from 'lucide-react'
import { api, type AdvisorMode, type Category, type CreateProductInput, type Product, type Suggestion } from './api'
import NewProductDialog from './NewProductDialog'
import './stockpulse.css'

type Notice = { kind: 'success' | 'error'; text: string } | null
type WorkspacePage = 'home' | 'overview' | 'inventory' | 'decisions'

const pageDetails: Record<WorkspacePage, { section: string; title: string; description: string }> = {
  home: {
    section: 'HOME',
    title: 'StockPulse',
    description: 'Inventory signals in. Human-approved pricing and replenishment decisions out.',
  },
  overview: {
    section: 'OVERVIEW',
    title: 'Inventory overview',
    description: 'Stock signals, pricing advice, and replenishment decisions.',
  },
  inventory: {
    section: 'INVENTORY',
    title: 'Product inventory',
    description: 'Catalog levels, demand velocity, and stock actions.',
  },
  decisions: {
    section: 'DECISIONS',
    title: 'Decision queue',
    description: 'Review recommendations before prices or inventory change.',
  },
}

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const categoryLabels: Record<Category, string> = {
  ELECTRONICS: 'Electronics',
  APPAREL: 'Apparel',
  HOME: 'Home',
}
const triggerLabels: Record<Suggestion['triggerReason'], string> = {
  INITIAL: 'Initial',
  INVENTORY_LOW: 'Low stock',
  DEMAND_SPIKE: 'Demand spike',
  MANUAL: 'Manual',
}

function StockPulseApp() {
  const [activePage, setActivePage] = useState<WorkspacePage>('home')
  const [products, setProducts] = useState<Product[]>([])
  const [suggestions, setSuggestions] = useState<Suggestion[]>([])
  const [strategy, setStrategy] = useState<AdvisorMode>('AUTO')
  const [llmConfigured, setLlmConfigured] = useState(false)
  const [loading, setLoading] = useState(true)
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<Notice>(null)
  const [busyKey, setBusyKey] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | Category>('ALL')
  const [showCreate, setShowCreate] = useState(false)

  const loadDashboard = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true)
    try {
      const [nextProducts, nextSuggestions, settings] = await Promise.all([
        api.listProducts(),
        api.listPendingSuggestions(),
        api.getStrategy(),
      ])
      setProducts(nextProducts)
      setSuggestions(nextSuggestions)
      setStrategy(settings.mode)
      setLlmConfigured(settings.llmConfigured)
      setConnected(true)
      setError(null)
    } catch (cause) {
      setConnected(false)
      if (!quiet) setError(cause instanceof Error ? cause.message : 'Could not connect to the backend')
    } finally {
      setLoading(false)
    }
  }, [])

  const pollDashboard = useEffectEvent(() => {
    void loadDashboard(true)
  })

  useEffect(() => {
    const initialRefresh = window.setTimeout(pollDashboard, 0)
    const interval = window.setInterval(pollDashboard, 7000)
    return () => {
      window.clearTimeout(initialRefresh)
      window.clearInterval(interval)
    }
  }, [])

  useEffect(() => {
    if (!notice) return
    const timeout = window.setTimeout(() => setNotice(null), 3600)
    return () => window.clearTimeout(timeout)
  }, [notice])

  const visibleProducts = useMemo(() => {
    const query = search.trim().toLowerCase()
    return products.filter((product) => {
      const matchesCategory = categoryFilter === 'ALL' || product.category === categoryFilter
      const matchesSearch = !query || product.name.toLowerCase().includes(query)
        || product.sku.toLowerCase().includes(query)
      return matchesCategory && matchesSearch
    })
  }, [products, search, categoryFilter])

  const lowStockCount = products.filter((product) => product.stockLevel < product.reorderThreshold).length
  const averageVelocity = products.length
    ? (products.reduce((sum, product) => sum + product.demandVelocity, 0) / products.length).toFixed(1)
    : '0.0'
  const averageConfidence = suggestions.length
    ? `${Math.round(suggestions.reduce((sum, suggestion) => sum + suggestion.confidence, 0) / suggestions.length * 100)}%`
    : '--'
  const currentPage = pageDetails[activePage]

  const navigateTo = (page: WorkspacePage) => {
    setActivePage(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const runAction = async (key: string, action: () => Promise<unknown>, successMessage: string) => {
    setBusyKey(key)
    try {
      await action()
      setNotice({ kind: 'success', text: successMessage })
      await loadDashboard(true)
    } catch (cause) {
      setNotice({ kind: 'error', text: cause instanceof Error ? cause.message : 'Action failed' })
    } finally {
      setBusyKey(null)
    }
  }

  const createProduct = async (product: CreateProductInput) => {
    setBusyKey('create')
    try {
      await api.createProduct(product)
      setShowCreate(false)
      setNotice({ kind: 'success', text: 'Product added to the catalog' })
      await loadDashboard(true)
    } catch (cause) {
      setNotice({ kind: 'error', text: cause instanceof Error ? cause.message : 'Could not create product' })
    } finally {
      setBusyKey(null)
    }
  }

  const changeStrategy = async (mode: AdvisorMode) => {
    await runAction('strategy', async () => {
      const settings = await api.setStrategy(mode)
      setStrategy(settings.mode)
      setLlmConfigured(settings.llmConfigured)
    }, `${mode} advisor selected`)
  }

  const approve = (suggestion: Suggestion, decision: 'ACCEPTED' | 'REJECTED') => {
    const label = decision === 'ACCEPTED' ? 'Suggestion accepted' : 'Suggestion rejected'
    void runAction(`decision-${suggestion.id}`, () => api.decideSuggestion(suggestion, decision), label)
  }

  return (
    <div className="app-frame">
      <aside className="sidebar">
        <a className="brand" href="#home" aria-label="StockPulse home" onClick={() => navigateTo('home')}>
          <span className="brand-mark"><Activity size={19} strokeWidth={2.5} /></span>
          <span className="brand-name">stockpulse<span>MERCH OPS</span></span>
        </a>
        <div className="sidebar-caption">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          <a className={`nav-item ${activePage === 'home' ? 'is-active' : ''}`} href="#home" onClick={() => navigateTo('home')}><House size={17} /> Home</a>
          <a className={`nav-item ${activePage === 'overview' ? 'is-active' : ''}`} href="#overview" onClick={() => navigateTo('overview')}><Activity size={17} /> Overview</a>
          <a className={`nav-item ${activePage === 'inventory' ? 'is-active' : ''}`} href="#catalog" onClick={() => navigateTo('inventory')}><Boxes size={17} /> Inventory <span className="nav-count">{products.length}</span></a>
          <a className={`nav-item ${activePage === 'decisions' ? 'is-active' : ''}`} href="#decisions" onClick={() => navigateTo('decisions')}><CircleCheck size={17} /> Decision queue <span className="nav-count">{suggestions.length}</span></a>
        </nav>
        <div className="sidebar-spacer" />
        <div className="connection-box">
          <span className={`connection-dot ${connected ? 'is-connected' : ''}`} />
          <div><strong>{connected ? 'Backend connected' : 'Backend unavailable'}</strong><span>Local workspace</span></div>
        </div>
        <div className="sidebar-footer">STOCKPULSE · INVENTORY ADVISOR</div>
      </aside>

      <main className="main-area" id={activePage}>
        <header className="topbar">
          <div className="breadcrumb">MERCHANDISING <span>/</span> {currentPage.section}</div>
          <div className="topbar-meta"><span className="live-pip" /> LOCAL DEVELOPMENT</div>
        </header>

        <div className="page-content">
          <section className="page-heading">
            <div>
              <div className="eyebrow">MONDAY, SEPTEMBER 28, 2026</div>
              <h1>{currentPage.title}</h1>
              <p className="heading-copy">{currentPage.description}</p>
            </div>
            <div className="heading-actions">
              {activePage !== 'home' && (
                <button className="button button-quiet" type="button" onClick={() => void loadDashboard()} title="Refresh dashboard">
                  <RefreshCw size={16} /> Refresh
                </button>
              )}
              {activePage === 'inventory' && (
                <button className="button button-primary" type="button" onClick={() => setShowCreate(true)}>
                  <Plus size={17} /> Add product
                </button>
              )}
            </div>
          </section>

          {error && !connected && (
            <div className="error-banner" role="alert">
              <CircleAlert size={18} />
              <span>{error}. Start the Spring Boot backend on port 8080 and retry.</span>
              <button className="icon-button" type="button" aria-label="Retry connection" onClick={() => void loadDashboard()}><RefreshCw size={16} /></button>
            </div>
          )}

          {activePage === 'overview' && (
            <section className="metrics" aria-label="Inventory metrics">
              <Metric label="Catalog items" value={String(products.length)} icon={<Package size={18} />} tone="green" />
              <Metric label="Below threshold" value={String(lowStockCount)} icon={<CircleAlert size={18} />} tone="rust" />
              <Metric label="Awaiting review" value={String(suggestions.length)} icon={<Clock3 size={18} />} tone="gold" />
              <Metric label="Avg. demand / day" value={averageVelocity} icon={<Activity size={18} />} tone="blue" />
            </section>
          )}

          <section className={`workspace-grid ${activePage}-view`}>
            {activePage === 'home' && (
              <>
                <section className="platform-purpose" aria-label="What StockPulse does">
                  <div className="purpose-copy">
                    <div className="eyebrow">AI INVENTORY &amp; DYNAMIC PRICING</div>
                    <p>StockPulse watches stock and demand changes, prepares pricing and reorder advice, and gives your merchandising team the final say before anything is applied.</p>
                  </div>
                  <div className="purpose-control"><CircleCheck size={17} /><span><strong>You stay in control</strong><small>Suggestions never change price or stock without approval.</small></span></div>
                </section>
                <section className="workflow-section" aria-labelledby="workflow-title">
                  <div className="workflow-heading">
                    <div>
                      <div className="eyebrow">FROM INVENTORY SIGNAL TO DECISION</div>
                      <h2 id="workflow-title">How StockPulse works</h2>
                    </div>
                    <div className="human-checkpoint"><CircleCheck size={15} /> Human approval before changes</div>
                  </div>
                  <div className="workflow-steps">
                    <WorkflowStep number="01" icon={<Boxes size={18} />} title="Track inventory" description="Add products, update stock, or record a sale. Demand and reorder levels stay editable." tone="green" />
                    <span className="workflow-arrow" aria-hidden="true"><ArrowRight size={17} /></span>
                    <WorkflowStep number="02" icon={<Activity size={18} />} title="Detect a signal" description="Stock below its threshold or demand rising above peers triggers a review." tone="rust" />
                    <span className="workflow-arrow" aria-hidden="true"><ArrowRight size={17} /></span>
                    <WorkflowStep number="03" icon={<Sparkles size={18} />} title="Get recommendations" description="AI suggests a price and reorder quantity; rule-based advice is the fallback." tone="blue" />
                    <span className="workflow-arrow" aria-hidden="true"><ArrowRight size={17} /></span>
                    <WorkflowStep number="04" icon={<CircleCheck size={18} />} title="Review and decide" description="Accept or reject each proposal. Only accepted advice changes price or stock." tone="gold" />
                  </div>
                </section>
                <div className="home-actions" aria-label="Continue to workspace">
                  <button type="button" className="button button-primary" onClick={() => navigateTo('overview')}>Open overview <ArrowRight size={16} /></button>
                  <button type="button" className="button button-quiet" onClick={() => navigateTo('inventory')}>Browse inventory</button>
                </div>
              </>
            )}
            {activePage === 'overview' && (
                <div className="overview-shortcuts" aria-label="Workspace pages">
                  <button type="button" className="overview-shortcut" onClick={() => navigateTo('inventory')}>
                    <span className="shortcut-icon"><Boxes size={19} /></span>
                    <span className="shortcut-copy"><strong>Product inventory</strong><small>{products.length} catalog items</small></span>
                    <span className="shortcut-arrow"><ArrowRight size={17} /></span>
                  </button>
                  <button type="button" className="overview-shortcut" onClick={() => navigateTo('decisions')}>
                    <span className="shortcut-icon queue"><CircleCheck size={19} /></span>
                    <span className="shortcut-copy"><strong>Decision queue</strong><small>{suggestions.length} awaiting review</small></span>
                    <span className="shortcut-arrow"><ArrowRight size={17} /></span>
                  </button>
                </div>
            )}

            <div className={`catalog-panel ${activePage === 'inventory' ? '' : 'page-hidden'}`} id="catalog">
              <div className="section-heading">
                <div>
                  <div className="eyebrow">LIVE CATALOG</div>
                  <h2>Products <span className="sub-count">{products.length}</span></h2>
                </div>
                <button className="icon-button" type="button" onClick={() => void loadDashboard()} aria-label="Refresh products" title="Refresh products">
                  <RefreshCw size={16} />
                </button>
              </div>
              <div className="catalog-controls">
                <label className="search-field">
                  <Search size={16} />
                  <input aria-label="Search products" placeholder="Search SKU or name" value={search} onChange={(event) => setSearch(event.target.value)} />
                </label>
                <label className="filter-select">
                  <SlidersHorizontal size={15} />
                  <select aria-label="Filter category" value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value as 'ALL' | Category)}>
                    <option value="ALL">All categories</option>
                    <option value="ELECTRONICS">Electronics</option>
                    <option value="APPAREL">Apparel</option>
                    <option value="HOME">Home</option>
                  </select>
                </label>
              </div>

              <div className="table-wrap">
                <table className="product-table">
                  <thead><tr><th>Product</th><th>Stock</th><th>Demand</th><th>Price</th><th>Actions</th></tr></thead>
                  <tbody>
                    {loading && products.length === 0 && <tr><td colSpan={5} className="table-message"><LoaderCircle className="spin" size={18} /> Loading catalog</td></tr>}
                    {!loading && visibleProducts.length === 0 && (
                      <tr><td colSpan={5} className="empty-cell">
                        <PackagePlus size={25} />
                        <strong>{products.length === 0 ? 'Your catalog is empty' : 'No matching products'}</strong>
                        <span>{products.length === 0 ? 'Add a product to begin tracking inventory.' : 'Try a different search or category.'}</span>
                        {products.length === 0 && <button className="text-button" type="button" onClick={() => setShowCreate(true)}>Add your first product <span>→</span></button>}
                      </td></tr>
                    )}
                    {visibleProducts.map((product) => (
                      <ProductRow
                        key={`${product.id}-${product.stockLevel}-${product.demandVelocity}-${product.reorderThreshold}`}
                        product={product}
                        busy={busyKey === `sale-${product.id}` || busyKey === `stock-${product.id}` || busyKey === `price-${product.id}` || busyKey === `reorder-${product.id}` || busyKey === `metrics-${product.id}`}
                        onSell={() => void runAction(`sale-${product.id}`, () => api.simulateSale(product.id), 'Sale recorded; stock signals are being evaluated')}
                        onSaveStock={(stockLevel) => void runAction(`stock-${product.id}`, () => api.updateStock(product.id, stockLevel), 'Stock level updated')}
                        onSaveDemand={(demandVelocity) => void runAction(
                          `metrics-${product.id}`,
                          () => api.updateProductMetrics(product.id, { demandVelocity, reorderThreshold: product.reorderThreshold }),
                          'Demand updated',
                        )}
                        onSaveThreshold={(reorderThreshold) => void runAction(
                          `metrics-${product.id}`,
                          () => api.updateProductMetrics(product.id, { demandVelocity: product.demandVelocity, reorderThreshold }),
                          'Reorder threshold updated',
                        )}
                        onRequestPricing={() => void runAction(`price-${product.id}`, () => api.requestAdvice(product.id, 'PRICING'), 'Pricing advice requested')}
                        onRequestReorder={() => void runAction(`reorder-${product.id}`, () => api.requestAdvice(product.id, 'REORDER'), 'Reorder advice requested')}
                      />
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="catalog-foot"><span><span className="key-dot low" /> Below threshold</span><span><span className="key-dot healthy" /> Healthy</span></div>
            </div>

            <aside className={`decision-panel ${activePage === 'decisions' ? '' : 'page-hidden'}`} id="decisions">
              <div className="section-heading decision-heading">
                <div>
                  <div className="eyebrow">HUMAN CHECKPOINT</div>
                  <h2>Decision queue <span className="sub-count">{suggestions.length}</span></h2>
                </div>
                <span className="queue-mark"><Clock3 size={16} /></span>
              </div>
              <div className="strategy-strip">
                <div className="strategy-label"><Sparkles size={15} /><span>ADVISOR</span></div>
                <div className="strategy-segments" role="group" aria-label="Recommendation strategy">
                  {(['AUTO', 'AI', 'RULES'] as const).map((mode) => (
                    <button key={mode} type="button" className={strategy === mode ? 'selected' : ''} disabled={busyKey === 'strategy'} onClick={() => void changeStrategy(mode)}>{mode}</button>
                  ))}
                </div>
              </div>
              <div className={`llm-status ${llmConfigured ? 'configured' : ''}`}>
                <span className="status-dot" />
                {llmConfigured ? 'LLM gateway configured' : 'Rules fallback · LLM key not set'}
              </div>
              <div className="suggestion-list">
                {suggestions.length === 0 && (
                  <div className="queue-empty">
                    <div className="empty-icon"><CircleDollarSign size={21} /></div>
                    <strong>Nothing to review</strong>
                    <span>Low-stock and demand signals will appear here as suggestions for approval.</span>
                  </div>
                )}
                {suggestions.map((suggestion) => (
                  <SuggestionItem
                    key={`${suggestion.type}-${suggestion.id}`}
                    suggestion={suggestion}
                    busy={busyKey === `decision-${suggestion.id}`}
                    onAccept={() => approve(suggestion, 'ACCEPTED')}
                    onReject={() => approve(suggestion, 'REJECTED')}
                  />
                ))}
              </div>
              <div className="queue-summary"><span><span className="key-dot gold" /> Avg. confidence</span><strong>{averageConfidence}</strong></div>
            </aside>
          </section>

          <footer className="page-footer">
            <span>StockPulse Inventory Advisor</span>
            <span>Suggestions change inventory only after approval.</span>
          </footer>
        </div>
      </main>

      {notice && <div className={`toast ${notice.kind}`} role="status">{notice.kind === 'success' ? <CircleCheck size={17} /> : <CircleAlert size={17} />}{notice.text}</div>}
      {showCreate && <NewProductDialog busy={busyKey === 'create'} onClose={() => setShowCreate(false)} onCreate={createProduct} />}
    </div>
  )
}

function Metric({ label, value, icon, tone }: { label: string; value: string; icon: React.ReactNode; tone: string }) {
  return (
    <div className="metric-cell">
      <span className={`metric-icon ${tone}`}>{icon}</span>
      <div><span className="metric-value">{value}</span><span className="metric-label">{label}</span></div>
    </div>
  )
}

function WorkflowStep({ number, icon, title, description, tone }: {
  number: string
  icon: React.ReactNode
  title: string
  description: string
  tone: string
}) {
  return (
    <article className="workflow-step">
      <div className="workflow-step-meta"><span className={`workflow-icon ${tone}`}>{icon}</span><span className="workflow-number">{number}</span></div>
      <h3>{title}</h3>
      <p>{description}</p>
    </article>
  )
}

function ProductRow({ product, busy, onSell, onRequestPricing, onRequestReorder, onSaveStock, onSaveDemand, onSaveThreshold }: {
  product: Product
  busy: boolean
  onSell: () => void
  onRequestPricing: () => void
  onRequestReorder: () => void
  onSaveStock: (stockLevel: number) => void
  onSaveDemand: (demandVelocity: number) => void
  onSaveThreshold: (reorderThreshold: number) => void
}) {
  const low = product.stockLevel < product.reorderThreshold
  return (
    <tr>
      <td>
        <div className="product-cell">
          <span className={`product-glyph ${product.category.toLowerCase()}`}><Package size={17} /></span>
          <div className="product-identity"><strong>{product.name}</strong><span>{product.sku} <i>·</i> {categoryLabels[product.category]}</span></div>
        </div>
      </td>
      <td>
        <div className="stock-cell">
          <InlineNumberEditor label="In stock" value={product.stockLevel} busy={busy} onSave={onSaveStock} />
          <span className={`stock-state ${low ? 'low' : 'healthy'}`}>{low ? 'LOW' : product.status === 'OUT_OF_STOCK' ? 'EMPTY' : 'OK'}</span>
        </div>
        <InlineNumberEditor label="Reorder at" value={product.reorderThreshold} busy={busy} onSave={onSaveThreshold} />
      </td>
      <td>
        <InlineNumberEditor label="Demand/day" value={product.demandVelocity} busy={busy} onSave={onSaveDemand} />
      </td>
      <td><span className="price-value">{currency.format(product.currentPrice)}</span></td>
      <td>
        <div className="row-actions">
          <button type="button" className="small-action sale-action" onClick={onSell} disabled={busy || product.stockLevel < 1} title="Record one sale" aria-label={`Record one sale for ${product.name}`}><ShoppingCart size={15} /></button>
          <button type="button" className="small-action" onClick={onRequestPricing} disabled={busy} title="Request pricing advice" aria-label={`Request pricing advice for ${product.name}`}><CircleDollarSign size={15} /></button>
          <button type="button" className="small-action" onClick={onRequestReorder} disabled={busy} title="Request reorder advice" aria-label={`Request reorder advice for ${product.name}`}><PackagePlus size={15} /></button>
        </div>
      </td>
    </tr>
  )
}

function InlineNumberEditor({ label, value, busy, onSave }: {
  label: string
  value: number
  busy: boolean
  onSave: (value: number) => void
}) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(String(value))
  const valid = /^\d+$/.test(draft)
  const changed = Number(draft) !== value

  const cancel = () => {
    setDraft(String(value))
    setEditing(false)
  }

  const save = () => {
    if (valid && changed && !busy) onSave(Number(draft))
    if (valid && !changed) setEditing(false)
  }

  if (!editing) {
    return (
      <div className="inline-metric">
        <span>{label}</span>
        <strong>{value}</strong>
        <button type="button" className="metric-edit-button" onClick={() => { setDraft(String(value)); setEditing(true) }} disabled={busy} title={`Edit ${label.toLowerCase()}`} aria-label={`Edit ${label.toLowerCase()}`}>
          <Pencil size={12} />
        </button>
      </div>
    )
  }

  return (
    <div className="inline-metric is-editing">
      <span>{label}</span>
      <input aria-label={label} type="number" min="0" step="1" inputMode="numeric" value={draft} disabled={busy} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => {
        if (event.key === 'Enter') save()
        if (event.key === 'Escape') cancel()
      }} autoFocus />
      <button type="button" className="metric-edit-button metric-save-button" onClick={save} disabled={busy || !valid || !changed} title={`Save ${label.toLowerCase()}`} aria-label={`Save ${label.toLowerCase()}`}>
        {busy ? <LoaderCircle className="spin" size={13} /> : <Save size={13} />}
      </button>
      <button type="button" className="metric-edit-button metric-cancel-button" onClick={cancel} disabled={busy} title="Cancel edit" aria-label="Cancel edit"><X size={13} /></button>
    </div>
  )
}

function SuggestionItem({ suggestion, busy, onAccept, onReject }: {
  suggestion: Suggestion
  busy: boolean
  onAccept: () => void
  onReject: () => void
}) {
  const isPricing = suggestion.type === 'PRICING'
  const directionIcon = suggestion.direction === 'INCREASE' ? <ArrowUpRight size={15} />
    : suggestion.direction === 'DECREASE' ? <ArrowDownRight size={15} /> : null
  return (
    <article className="suggestion-item">
      <header className="suggestion-topline">
        <span className={`suggestion-type ${isPricing ? 'pricing' : 'reorder'}`}>{isPricing ? <CircleDollarSign size={14} /> : <PackagePlus size={14} />}{isPricing ? 'PRICING' : 'REORDER'}</span>
        <span className={`trigger-badge ${suggestion.triggerReason.toLowerCase().replace('_', '-')}`}>{triggerLabels[suggestion.triggerReason]}</span>
      </header>
      <div className="suggestion-product">{suggestion.productName}</div>
      <div className="recommendation-line">
        {isPricing ? (
          <><strong>{currency.format(suggestion.recommendedPrice ?? 0)}</strong><span className={`direction-label ${(suggestion.direction ?? '').toLowerCase()}`}>{directionIcon}{suggestion.direction ?? 'HOLD'}</span></>
        ) : (
          <><strong>{suggestion.recommendedQuantity ?? 0}<small> units</small></strong><span className="lead-time">~{suggestion.suggestedLeadTimeDays ?? 0}d lead</span></>
        )}
        <span className="confidence">{Math.round(suggestion.confidence * 100)}%</span>
      </div>
      {isPricing && <div className="current-value">Current {currency.format(suggestion.currentPrice ?? 0)}</div>}
      <p className="reasoning">{suggestion.reasoning}</p>
      <div className="suggestion-actions">
        <button type="button" className="decision-button reject" onClick={onReject} disabled={busy} title="Reject suggestion"><X size={15} /> Reject</button>
        <button type="button" className="decision-button accept" onClick={onAccept} disabled={busy} title="Accept suggestion">{busy ? <LoaderCircle className="spin" size={15} /> : <Check size={15} />} Accept</button>
      </div>
    </article>
  )
}

export default StockPulseApp
