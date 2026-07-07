import { useState } from 'react'

interface ApiResponse<T> {
  readonly success: boolean
  readonly data: T | null
  readonly error: string | null
}

interface HealthStatus {
  readonly service: string
  readonly status: string
  readonly timestamp: string
}

interface LoginResponse {
  readonly accessToken: string
  readonly tokenType: string
  readonly expiresIn: number
}

interface CurrentUserResponse {
  readonly username: string
  readonly authorities: readonly string[]
}

interface ServiceCheck<T> {
  readonly loading: boolean
  readonly data: T | null
  readonly error: string | null
}

const initialCheck = {
  loading: false,
  data: null,
  error: null,
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const response = await fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })
  const body = (await response.json()) as ApiResponse<T>

  if (!response.ok || !body.success) {
    throw new Error(body.error ?? `Request failed with status ${response.status}`)
  }

  return body
}

export function App() {
  const [backendHealth, setBackendHealth] = useState<ServiceCheck<HealthStatus>>(initialCheck)
  const [loginResult, setLoginResult] = useState<ServiceCheck<LoginResponse>>(initialCheck)
  const [currentUser, setCurrentUser] = useState<ServiceCheck<CurrentUserResponse>>(initialCheck)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const checkBackendHealth = async (): Promise<void> => {
    setBackendHealth({ loading: true, data: null, error: null })

    try {
      const result = await requestJson<HealthStatus>('/api/health')
      setBackendHealth({ loading: false, data: result.data, error: null })
    } catch (error: unknown) {
      setBackendHealth({ loading: false, data: null, error: getErrorMessage(error) })
    }
  }

  const login = async (): Promise<void> => {
    setLoginResult({ loading: true, data: null, error: null })
    setCurrentUser(initialCheck)

    try {
      const result = await requestJson<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      setAccessToken(result.data?.accessToken ?? null)
      setLoginResult({ loading: false, data: result.data, error: null })
    } catch (error: unknown) {
      setAccessToken(null)
      setLoginResult({ loading: false, data: null, error: getErrorMessage(error) })
    }
  }

  const loadCurrentUser = async (): Promise<void> => {
    setCurrentUser({ loading: true, data: null, error: null })

    try {
      if (!accessToken) {
        throw new Error('Login first to receive a bearer token.')
      }

      const result = await requestJson<CurrentUserResponse>('/api/me', {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      })
      setCurrentUser({ loading: false, data: result.data, error: null })
    } catch (error: unknown) {
      setCurrentUser({ loading: false, data: null, error: getErrorMessage(error) })
    }
  }

  return (
    <main className="app-shell">
      <section className="overview">
        <p className="eyebrow">GitOps Deployment Check</p>
        <h1>Deploy History Console</h1>
        <p className="summary">
          Frontend and backend images are built by Jenkins, pushed to Nexus,
          and deployed through Argo CD using the manifest repository.
        </p>
      </section>

      <section className="check-grid" aria-label="Backend checks">
        <article className="check-panel">
          <div>
            <span className="panel-label">Backend</span>
            <h2>Health</h2>
          </div>
          <button type="button" onClick={checkBackendHealth}>
            {backendHealth.loading ? 'Checking...' : 'Check backend'}
          </button>
          <StatusBlock check={backendHealth} />
        </article>

        <article className="check-panel">
          <div>
            <span className="panel-label">Backend</span>
            <h2>JWT login</h2>
          </div>
          <label>
            Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          <button type="button" onClick={login} disabled={!username || !password}>
            {loginResult.loading ? 'Logging in...' : 'Login'}
          </button>
          <StatusBlock check={loginResult} />
        </article>

        <article className="check-panel">
          <div>
            <span className="panel-label">Protected API</span>
            <h2>Current user</h2>
          </div>
          <button type="button" onClick={loadCurrentUser}>
            {currentUser.loading ? 'Loading...' : 'Call /api/me'}
          </button>
          <StatusBlock check={currentUser} />
        </article>
      </section>
    </main>
  )
}

function StatusBlock<T>({ check }: { readonly check: ServiceCheck<T> }) {
  if (check.loading) {
    return <pre className="status-block">Loading</pre>
  }

  if (check.error) {
    return <pre className="status-block error">{check.error}</pre>
  }

  if (check.data) {
    return <pre className="status-block success">{JSON.stringify(check.data, null, 2)}</pre>
  }

  return <pre className="status-block muted">Not checked yet</pre>
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }

  return 'Unexpected error'
}
