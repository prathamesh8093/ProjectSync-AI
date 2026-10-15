import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { LogOut, Menu, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import '../styles/dashboard.css'

function DashboardLayout({ navItems, children }) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  const initials = user.fullName
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <div className="dashboard-shell">
      <aside className={`dashboard-sidebar ${mobileNavOpen ? 'is-open' : ''}`}>
        <div className="sidebar-logo">
          <span className="sidebar-logo-mark">P</span>
          <span>ProjectSync AI</span>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = item.path === location.pathname

            if (!item.path) {
              return (
                <div
                  key={item.label}
                  className="sidebar-nav-item is-disabled"
                >
                  <Icon size={18} strokeWidth={1.75} />
                  <span>{item.label}</span>
                  <span className="sidebar-soon-badge">Soon</span>
                </div>
              )
            }

            return (
              <Link
                key={item.label}
                to={item.path}
                className={`sidebar-nav-item ${isActive ? 'is-active' : ''}`}
                onClick={() => setMobileNavOpen(false)}
              >
                <Icon size={18} strokeWidth={1.75} />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>
      </aside>

      {mobileNavOpen && (
        <div
          className="sidebar-backdrop"
          onClick={() => setMobileNavOpen(false)}
        />
      )}

      <div className="dashboard-main">
        <header className="dashboard-header">
          <button
            className="mobile-nav-toggle"
            onClick={() => setMobileNavOpen((open) => !open)}
            aria-label="Toggle navigation"
          >
            {mobileNavOpen ? <X size={20} /> : <Menu size={20} />}
          </button>

          <div className="header-user-info">
            <div className="header-user-avatar">{initials}</div>

            <div>
              <div className="header-user-name">{user.fullName}</div>
              <div className="header-user-role">{user.role}</div>
            </div>
          </div>

          <button
            className="header-logout-button"
            onClick={logout}
          >
            <LogOut size={16} strokeWidth={1.75} />
            <span>Logout</span>
          </button>
        </header>

        <main className="dashboard-content">
          {children}
        </main>
      </div>
    </div>
  )
}

export default DashboardLayout