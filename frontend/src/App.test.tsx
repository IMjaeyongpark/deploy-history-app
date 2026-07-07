import { render, screen } from '@testing-library/react'
import { App } from './App'

describe('App', () => {
  it('renders the project dashboard', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'Deploy History Console' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Check backend' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Login' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Call /api/me' })).toBeInTheDocument()
  })
})
