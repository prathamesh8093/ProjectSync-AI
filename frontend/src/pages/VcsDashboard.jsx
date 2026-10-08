import { useState, useEffect, useCallback } from 'react'
import {
  GitBranch,
  GitCommit,
  GitPullRequest,
  FolderGit2,
  RefreshCw,
  Plus,
  Trash2,
  FolderOpen,
  ArrowUpCircle,
  ArrowDownCircle,
  FileText,
  CheckCircle2,
  AlertCircle,
  Eye,
  X,
  ShieldCheck,
  Send,
  Layers,
  Download,
  Terminal,
  Database,
  ChevronDown,
  ChevronUp,
  LayoutDashboard,
  UserRound,
  Users,
  ListChecks,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import vcsService from '../services/vcsService.js'
import { useAuth } from '../context/AuthContext.jsx'
import '../styles/dashboard.css'
import '../styles/vcs.css'

// Navigation items matching Student Dashboard navigation
const studentNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/student/dashboard' },
  { label: 'My Profile', icon: UserRound, path: '/student/profile' },
  { label: 'My Team', icon: Users, path: '/student/team' },
  { label: 'Project Proposal', icon: FileText, path: '/student/proposal' },
  { label: 'Tasks / Projects', icon: ListChecks, path: '/student/projects' },
  { label: 'Version Control (VCS)', icon: GitBranch, path: '/student/vcs' },
]

const facultyNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/faculty/dashboard' },
  { label: 'Faculty Profile', icon: UserRound, path: '/faculty/profile' },
  { label: 'Project Proposals', icon: FileText, path: '/faculty/proposals' },
  { label: 'Project Monitoring', icon: ListChecks, path: '/faculty/projects' },
  { label: 'Version Control (VCS)', icon: GitBranch, path: '/student/vcs' },
]

export default function VcsDashboard() {
  const { isAuthenticated, user, isDevMode } = useAuth()
  const [repositories, setRepositories] = useState([])
  const [activeRepoId, setActiveRepoId] = useState('')
  const [activeRepo, setActiveRepo] = useState(null)
  const [status, setStatus] = useState(null)
  const [activeTab, setActiveTab] = useState('changes')
  const [loading, setLoading] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const [backendOffline, setBackendOffline] = useState(false)

  // Commit composer
  const [commitMessage, setCommitMessage] = useState('')
  const [authorName, setAuthorName] = useState('')
  const [authorEmail, setAuthorEmail] = useState('')

  // Diff modal
  const [diffModal, setDiffModal] = useState({ open: false, path: '', staged: false, data: null })

  // History & commit details
  const [commits, setCommits] = useState([])
  const [selectedCommit, setSelectedCommit] = useState(null)

  // Branches
  const [branches, setBranches] = useState([])
  const [newBranchModal, setNewBranchModal] = useState(false)
  const [newBranchName, setNewBranchName] = useState('')

  // Create repo modal
  const [createRepoModal, setCreateRepoModal] = useState(false)
  const [newRepoData, setNewRepoData] = useState({ name: '', description: '', defaultBranch: 'main', initReadme: true })

  // Register existing repo modal
  const [registerRepoModal, setRegisterRepoModal] = useState(false)
  const [regRepoData, setRegRepoData] = useState({ name: '', path: '', description: '' })

  // Clone repo modal
  const [cloneRepoModal, setCloneRepoModal] = useState(false)
  const [cloneSourceType, setCloneSourceType] = useState('internal')
  const [cloneData, setCloneData] = useState({
    remoteUrl: '',
    name: '',
    branch: '',
    description: '',
    username: '',
    personalAccessToken: '',
    isNameAutoDerived: true,
  })
  const [cloneLoading, setCloneLoading] = useState(false)
  const [cloneResult, setCloneResult] = useState(null)
  const [cloneError, setCloneError] = useState(null)
  const [showCloneAuth, setShowCloneAuth] = useState(false)

  // Remote & Sync modal
  const [remoteModal, setRemoteModal] = useState(false)
  const [remoteUrl, setRemoteUrl] = useState('')
  const [syncModal, setSyncModal] = useState({ open: false, op: 'PULL' })
  const [githubAuth, setGithubAuth] = useState({ username: '', token: '' })
  const [syncResult, setSyncResult] = useState(null)

  const showToast = (type, message) => {
    setFeedback({ type, message })
    setTimeout(() => setFeedback(null), 5000)
  }

  // Load repositories list
  const loadRepositories = useCallback(async () => {
    try {
      setLoading(true)
      setBackendOffline(false)
      const list = await vcsService.listRepositories()
      setRepositories(list)
      if (list.length > 0 && !activeRepoId) {
        setActiveRepoId(list[0].id)
      } else if (list.length === 0) {
        setActiveRepoId('')
        setActiveRepo(null)
        setStatus(null)
      }
    } catch (err) {
      const isNetworkErr = !err.response || err.code === 'ERR_NETWORK' || err.message === 'Network Error'
      if (isNetworkErr) {
        setBackendOffline(true)
        showToast('error', 'Spring Boot backend is unreachable on port 8081. Please ensure backend is running.')
      } else {
        showToast('error', 'Failed to load repositories: ' + (err.response?.data?.message || err.message))
      }
    } finally {
      setLoading(false)
    }
  }, [activeRepoId])

  // Load active repository details and working tree status
  const loadActiveRepo = useCallback(async () => {
    if (!activeRepoId) return
    try {
      const details = await vcsService.getRepository(activeRepoId)
      setActiveRepo(details)
      const st = await vcsService.getStatus(activeRepoId)
      setStatus(st)

      if (details.remoteUrl) {
        setRemoteUrl(details.remoteUrl)
      }
    } catch (err) {
      showToast('error', 'Failed to load repository details: ' + (err.response?.data?.message || err.message))
    }
  }, [activeRepoId])

  // Load commits when history tab is active
  const loadCommits = useCallback(async () => {
    if (!activeRepoId) return
    try {
      const list = await vcsService.getCommitHistory(activeRepoId, '', 50)
      setCommits(list)
    } catch (err) {
      showToast('error', 'Failed to load commit history: ' + (err.response?.data?.message || err.message))
    }
  }, [activeRepoId])

  // Load branches when branch tab is active
  const loadBranches = useCallback(async () => {
    if (!activeRepoId) return
    try {
      const list = await vcsService.listBranches(activeRepoId)
      setBranches(list)
    } catch (err) {
      showToast('error', 'Failed to load branches: ' + (err.response?.data?.message || err.message))
    }
  }, [activeRepoId])

  useEffect(() => {
    loadRepositories()
  }, [])

  // Automatically probe health every 3s when backend is offline to reconnect seamlessly
  useEffect(() => {
    if (!backendOffline) return
    const interval = setInterval(() => {
      vcsService.getHealth()
        .then(() => {
          setBackendOffline(false)
          loadRepositories()
          showToast('success', 'Connected to local Spring Boot backend!')
        })
        .catch(() => {
          // Backend still starting, keep probing
        })
    }, 3000)
    return () => clearInterval(interval)
  }, [backendOffline, loadRepositories])

  useEffect(() => {
    if (activeRepoId) {
      loadActiveRepo()
      if (activeTab === 'history') loadCommits()
      if (activeTab === 'branches') loadBranches()
    }
  }, [activeRepoId, activeTab, loadActiveRepo, loadCommits, loadBranches])

  // Refresh current repository view
  const handleRefresh = async () => {
    await loadRepositories()
    await loadActiveRepo()
    if (activeTab === 'history') await loadCommits()
    if (activeTab === 'branches') await loadBranches()
    showToast('success', 'Repository refreshed')
  }

  // Staging handlers
  const handleStageFile = async (filePath) => {
    try {
      await vcsService.stageFiles(activeRepoId, [filePath])
      await loadActiveRepo()
      showToast('success', `Staged ${filePath}`)
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  const handleStageAll = async () => {
    try {
      await vcsService.stageAll(activeRepoId)
      await loadActiveRepo()
      showToast('success', 'Staged all changes')
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  const handleUnstageFile = async (filePath) => {
    try {
      await vcsService.unstageFiles(activeRepoId, [filePath])
      await loadActiveRepo()
      showToast('success', `Unstaged ${filePath}`)
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  const handleUnstageAll = async () => {
    try {
      await vcsService.unstageAll(activeRepoId)
      await loadActiveRepo()
      showToast('success', 'Unstaged all changes')
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  const handleDiscard = async (filePath) => {
    if (!window.confirm(`Discard changes to ${filePath}? This action cannot be undone.`)) return
    try {
      await vcsService.discardChanges(activeRepoId, [filePath])
      await loadActiveRepo()
      showToast('success', `Discarded changes in ${filePath}`)
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  // Commit handler
  const handleCommit = async (e) => {
    e.preventDefault()
    if (!commitMessage.trim()) {
      showToast('error', 'Please enter a commit message')
      return
    }
    try {
      await vcsService.createCommit(activeRepoId, {
        message: commitMessage,
        authorName: authorName.trim() || undefined,
        authorEmail: authorEmail.trim() || undefined,
      })
      setCommitMessage('')
      await loadActiveRepo()
      showToast('success', 'Committed changes successfully!')
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  // Diff inspection
  const handleViewDiff = async (filePath, staged) => {
    try {
      const data = await vcsService.getDiff(activeRepoId, filePath, staged)
      setDiffModal({ open: true, path: filePath, staged, data })
    } catch (err) {
      showToast('error', 'Failed to generate diff: ' + (err.response?.data?.message || err.message))
    }
  }

  // Branch handlers
  const handleCreateBranch = async (e) => {
    e.preventDefault()
    if (!newBranchName.trim()) return
    try {
      await vcsService.createBranch(activeRepoId, newBranchName.trim())
      setNewBranchName('')
      setNewBranchModal(false)
      await loadBranches()
      showToast('success', `Branch '${newBranchName}' created`)
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  const handleCheckoutBranch = async (name) => {
    try {
      await vcsService.checkoutBranch(activeRepoId, name)
      await loadActiveRepo()
      await loadBranches()
      showToast('success', `Switched to branch '${name}'`)
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  const handleDeleteBranch = async (name) => {
    if (!window.confirm(`Are you sure you want to delete branch '${name}'?`)) return
    try {
      await vcsService.deleteBranch(activeRepoId, name, false)
      await loadBranches()
      showToast('success', `Deleted branch '${name}'`)
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  // Create repository
  const handleCreateRepo = async (e) => {
    e.preventDefault()
    if (!isAuthenticated && !isDevMode) {
      showToast('error', 'Authentication required. Please log in before creating a repository.')
      return
    }
    try {
      const created = await vcsService.createRepository(newRepoData)
      setCreateRepoModal(false)
      setNewRepoData({ name: '', description: '', defaultBranch: 'main', initReadme: true })
      await loadRepositories()
      setActiveRepoId(created.id)
      showToast('success', `Repository '${created.name}' initialized successfully`)
    } catch (err) {
      if (err.response?.status === 401) {
        showToast('error', 'Authentication failed or session expired. Please log in through the Login page.')
      } else {
        showToast('error', err.response?.data?.message || err.message)
      }
    }
  }

  // Register existing local repository
  const handleRegisterRepo = async (e) => {
    e.preventDefault()
    if (!isAuthenticated && !isDevMode) {
      showToast('error', 'Authentication required. Please log in before registering a repository.')
      return
    }
    try {
      const registered = await vcsService.registerRepository(regRepoData)
      setRegisterRepoModal(false)
      setRegRepoData({ name: '', path: '', description: '' })
      await loadRepositories()
      setActiveRepoId(registered.id)
      showToast('success', `Repository '${registered.name}' registered successfully`)
    } catch (err) {
      if (err.response?.status === 401) {
        showToast('error', 'Authentication failed or session expired. Please log in through the Login page.')
      } else {
        showToast('error', err.response?.data?.message || err.message)
      }
    }
  }

  // Clone URL input change helper
  const handleCloneUrlChange = (url) => {
    let suggestedName = cloneData.name
    const cleanUrl = url.trim().replace(/\/+$/, '')
    const withoutGit = cleanUrl.endsWith('.git') ? cleanUrl.slice(0, -4) : cleanUrl
    const lastSlash = Math.max(withoutGit.lastIndexOf('/'), withoutGit.lastIndexOf('\\'))
    const extracted = lastSlash >= 0 ? withoutGit.slice(lastSlash + 1) : ''

    if (!cloneData.name || cloneData.isNameAutoDerived) {
      suggestedName = extracted
    }

    setCloneData({
      ...cloneData,
      remoteUrl: url,
      name: suggestedName,
      isNameAutoDerived: true,
    })
  }

  // Clone remote repository
  const handleCloneRepo = async (e) => {
    e.preventDefault()
    if (!cloneData.remoteUrl.trim()) {
      setCloneError('Remote repository URL is required.')
      return
    }

    if (!isAuthenticated && !isDevMode) {
      showToast('error', 'Authentication required. Please log in before cloning a repository.')
      return
    }

    try {
      setCloneLoading(true)
      setCloneError(null)
      setCloneResult(null)

      const payload = {
        remoteUrl: cloneData.remoteUrl.trim(),
        name: cloneData.name.trim() || undefined,
        branch: cloneData.branch.trim() || undefined,
        description: cloneData.description.trim() || undefined,
        username: cloneData.username.trim() || undefined,
        personalAccessToken: cloneData.personalAccessToken.trim() || undefined,
      }

      const res = await vcsService.cloneRepository(payload)
      setCloneResult(res)
      showToast('success', `Repository '${res.repository.name}' cloned successfully!`)

      const list = await vcsService.listRepositories()
      setRepositories(list)
      setActiveRepoId(res.repository.id)
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Clone failed'
      setCloneError(msg)
      showToast('error', 'Git clone failed: ' + msg)
    } finally {
      setCloneLoading(false)
    }
  }

  // Delete / unregister repo
  const handleDeleteRepo = async (repoId, deleteFiles) => {
    const promptMsg = deleteFiles
      ? 'Are you sure you want to permanently delete this repository and its files from disk?'
      : 'Are you sure you want to unregister this repository from ProjectSync AI?'
    if (!window.confirm(promptMsg)) return

    try {
      await vcsService.deleteRepository(repoId, deleteFiles)
      if (activeRepoId === repoId) setActiveRepoId('')
      await loadRepositories()
      showToast('success', 'Repository removed')
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  // Remote configuration
  const handleSaveRemote = async (e) => {
    e.preventDefault()
    try {
      await vcsService.setRemote(activeRepoId, { name: 'origin', fetchUrl: remoteUrl })
      setRemoteModal(false)
      await loadActiveRepo()
      showToast('success', 'Remote origin updated')
    } catch (err) {
      showToast('error', err.response?.data?.message || err.message)
    }
  }

  // Sync actions (Fetch, Pull, Push)
  const handleExecuteSync = async (e) => {
    e.preventDefault()
    const op = syncModal.op
    setLoading(true)
    try {
      const payload = {
        remoteName: 'origin',
        username: githubAuth.username || undefined,
        personalAccessToken: githubAuth.token || undefined,
      }
      let res
      if (op === 'FETCH') res = await vcsService.fetchRemote(activeRepoId, payload)
      else if (op === 'PULL') res = await vcsService.pullRemote(activeRepoId, payload)
      else if (op === 'PUSH') res = await vcsService.pushRemote(activeRepoId, payload)

      setSyncResult(res)
      if (res.success) {
        showToast('success', `${op} succeeded: ${res.message}`)
        await loadActiveRepo()
      } else {
        showToast('error', `${op} notice: ${res.message}`)
      }
    } catch (err) {
      showToast('error', `${op} failed: ` + (err.response?.data?.message || err.message))
    } finally {
      setLoading(false)
    }
  }

  const stagedCount = status?.staged?.length || 0
  const unstagedCount = (status?.unstaged?.length || 0) + (status?.untracked?.length || 0)

  return (
    <DashboardLayout navItems={user?.role === 'FACULTY' ? facultyNavItems : studentNavItems}>
      <div className="vcs-container">
        {/* Toast Feedback Alert */}
        {feedback && (
          <div className={`vcs-alert vcs-alert-${feedback.type}`}>
            {feedback.type === 'success' && <CheckCircle2 size={18} />}
            {feedback.type === 'error' && <AlertCircle size={18} />}
            <span>{feedback.message}</span>
          </div>
        )}

        {/* Backend Offline Warning Banner */}
        {backendOffline && (
          <div className="vcs-offline-banner">
            <div className="vcs-offline-icon">
              <AlertCircle size={22} color="#f87171" />
            </div>
            <div className="vcs-offline-info">
              <h4>Local Spring Boot Backend Offline (Port 8081)</h4>
              <p>
                Could not connect to <code>http://localhost:8081/api/vcs</code>. The VCS dashboard needs the local Spring Boot backend running with H2 and JGit.
              </p>
              <div className="vcs-offline-command">
                <code>cd backend ; .\mvnw.cmd spring-boot:run</code>
              </div>
            </div>
            <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={loadRepositories}>
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
              <span>Retry</span>
            </button>
          </div>
        )}

        {/* Unauthenticated Dev Access Notice */}
        {!isAuthenticated && (
          <div
            className="vcs-auth-warning"
            style={{
              background: '#fef3c7',
              border: '1px solid #f59e0b',
              color: '#92400e',
              padding: '0.75rem 1rem',
              borderRadius: '6px',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '1rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <AlertCircle size={18} />
              <span style={{ fontSize: '0.88rem' }}>
                <strong>Local Dev Access (Unauthenticated):</strong> You can inspect the VCS dashboard, but initializing or modifying repositories requires logging in with an authenticated user.
              </span>
            </div>
            <Link
              to="/login"
              className="vcs-btn-sm vcs-btn-primary-sm"
              style={{ textDecoration: 'none', padding: '0.35rem 0.85rem', whiteSpace: 'nowrap' }}
            >
              Log In Now
            </Link>
          </div>
        )}

        {/* Topbar: Repository Selector & Quick Actions */}
        <div className="vcs-topbar">
          <div className="vcs-repo-selector-group">
            <FolderGit2 size={20} color="var(--primary)" />
            <select
              className="vcs-select"
              value={activeRepoId}
              onChange={(e) => setActiveRepoId(e.target.value)}
              disabled={repositories.length === 0}
            >
              {repositories.length === 0 ? (
                <option value="">No repositories registered</option>
              ) : (
                repositories.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name} ({r.currentBranch})
                  </option>
                ))
              )}
            </select>

            {activeRepo && (
              <>
                <div className="vcs-branch-badge">
                  <GitBranch size={14} />
                  <span>{status?.currentBranch || activeRepo.defaultBranch}</span>
                </div>

                <div className={`vcs-status-indicator ${status?.isClean ? 'vcs-status-clean' : 'vcs-status-dirty'}`}>
                  {status?.isClean ? (
                    <>
                      <CheckCircle2 size={13} />
                      <span>Working tree clean</span>
                    </>
                  ) : (
                    <>
                      <AlertCircle size={13} />
                      <span>{stagedCount + unstagedCount} changes</span>
                    </>
                  )}
                </div>
              </>
            )}
          </div>

          <div className="vcs-topbar-actions">
            <button className="vcs-btn-sm" onClick={handleRefresh} title="Refresh Git status">
              <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
              <span>Refresh</span>
            </button>

            {activeRepo && (
              <>
                <button
                  className="vcs-btn-sm"
                  onClick={() => {
                    setSyncModal({ open: true, op: 'FETCH' })
                    setSyncResult(null)
                  }}
                  title="Fetch from remote"
                >
                  <RefreshCw size={14} />
                  <span>Fetch</span>
                </button>

                <button
                  className="vcs-btn-sm"
                  onClick={() => {
                    setSyncModal({ open: true, op: 'PULL' })
                    setSyncResult(null)
                  }}
                  title="Pull changes from remote"
                >
                  <ArrowDownCircle size={14} />
                  <span>Pull</span>
                </button>

                <button
                  className="vcs-btn-sm"
                  onClick={() => {
                    setSyncModal({ open: true, op: 'PUSH' })
                    setSyncResult(null)
                  }}
                  title="Push commits to remote"
                >
                  <ArrowUpCircle size={14} />
                  <span>Push</span>
                </button>

                <button className="vcs-btn-sm" onClick={() => setNewBranchModal(true)}>
                  <GitBranch size={14} />
                  <span>New Branch</span>
                </button>
              </>
            )}

            {isAuthenticated && user && (
              <span
                style={{
                  fontSize: '0.82rem',
                  color: '#475569',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  background: '#f1f5f9',
                  padding: '0.25rem 0.6rem',
                  borderRadius: '4px',
                  border: '1px solid #e2e8f0',
                }}
                title={`Authenticated as ${user.email} (${user.role})`}
              >
                <ShieldCheck size={14} color="#10b981" />
                <span>{user.email || user.fullName}</span>
              </span>
            )}

            <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={() => setCreateRepoModal(true)}>
              <Plus size={14} />
              <span>New Repo</span>
            </button>

            <button
              className="vcs-btn-sm"
              onClick={() => {
                setCloneRepoModal(true)
                setCloneResult(null)
                setCloneError(null)
              }}
            >
              <Download size={14} />
              <span>Clone Repo</span>
            </button>

            <button className="vcs-btn-sm" onClick={() => setRegisterRepoModal(true)}>
              <FolderOpen size={14} />
              <span>Open Local</span>
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="vcs-tabs">
          <button
            className={`vcs-tab ${activeTab === 'changes' ? 'is-active' : ''}`}
            onClick={() => setActiveTab('changes')}
          >
            <GitPullRequest size={16} />
            <span>Changes & Staging</span>
            {(stagedCount > 0 || unstagedCount > 0) && (
              <span className="vcs-tab-count">{stagedCount + unstagedCount}</span>
            )}
          </button>

          <button
            className={`vcs-tab ${activeTab === 'branches' ? 'is-active' : ''}`}
            onClick={() => setActiveTab('branches')}
          >
            <GitBranch size={16} />
            <span>Branches</span>
            {branches.length > 0 && <span className="vcs-tab-count">{branches.length}</span>}
          </button>

          <button
            className={`vcs-tab ${activeTab === 'history' ? 'is-active' : ''}`}
            onClick={() => setActiveTab('history')}
          >
            <GitCommit size={16} />
            <span>Commit History</span>
            {activeRepo?.commitCount > 0 && <span className="vcs-tab-count">{activeRepo.commitCount}</span>}
          </button>

          <button
            className={`vcs-tab ${activeTab === 'remotes' ? 'is-active' : ''}`}
            onClick={() => setActiveTab('remotes')}
          >
            <ArrowUpCircle size={16} />
            <span>GitHub & Remotes</span>
          </button>

          <button
            className={`vcs-tab ${activeTab === 'repositories' ? 'is-active' : ''}`}
            onClick={() => setActiveTab('repositories')}
          >
            <FolderGit2 size={16} />
            <span>All Repositories</span>
            <span className="vcs-tab-count">{repositories.length}</span>
          </button>
        </div>

        {/* TAB 1: CHANGES & STAGING */}
        {activeTab === 'changes' && (
          <div>
            {!activeRepo ? (
              <div className="vcs-empty-state">
                <FolderGit2 size={48} color="var(--text-muted)" />
                <h3>No Repository Selected</h3>
                <div style={{ display: 'flex', gap: '0.6rem', marginTop: '0.5rem', flexWrap: 'wrap', justifyContent: 'center' }}>
                  <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={() => setCreateRepoModal(true)}>
                    <Plus size={14} />
                    <span>Create New Repository</span>
                  </button>
                  <button
                    className="vcs-btn-sm"
                    onClick={() => {
                      setCloneRepoModal(true)
                      setCloneResult(null)
                      setCloneError(null)
                    }}
                  >
                    <Download size={14} />
                    <span>Clone Remote Repository</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="vcs-staging-grid">
                {/* STAGED CHANGES & COMMIT PANEL */}
                <div className="vcs-panel">
                  <div className="vcs-panel-header">
                    <h3 className="vcs-panel-title">
                      <span>Staged Changes</span>
                      <span className="vcs-badge badge-added">{stagedCount}</span>
                    </h3>
                    {stagedCount > 0 && (
                      <button className="vcs-btn-sm" onClick={handleUnstageAll}>
                        Unstage All
                      </button>
                    )}
                  </div>

                  <div className="vcs-panel-body">
                    {stagedCount === 0 ? (
                      <div className="vcs-empty-state" style={{ padding: '1.5rem 0' }}>
                        <p style={{ margin: 0 }}>No staged changes.</p>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          Stage modified files from the Changes panel to commit them.
                        </span>
                      </div>
                    ) : (
                      <div className="vcs-file-list">
                        {status.staged.map((f) => (
                          <div key={f.filePath} className="vcs-file-item">
                            <div className="vcs-file-info">
                              <span className={`vcs-badge badge-${f.status.toLowerCase()}`}>{f.status}</span>
                              <span className="vcs-file-path">{f.filePath}</span>
                            </div>
                            <div className="vcs-file-actions">
                              <button
                                className="vcs-btn-sm"
                                onClick={() => handleViewDiff(f.filePath, true)}
                                title="View diff"
                              >
                                <Eye size={12} />
                                <span>Diff</span>
                              </button>
                              <button
                                className="vcs-btn-sm"
                                onClick={() => handleUnstageFile(f.filePath)}
                                title="Unstage file"
                              >
                                Unstage
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Commit Composer */}
                    <form className="vcs-commit-box" onSubmit={handleCommit}>
                      <textarea
                        className="vcs-commit-textarea"
                        placeholder="Write a clear commit message (e.g. feat: add task collaboration dashboard)..."
                        value={commitMessage}
                        onChange={(e) => setCommitMessage(e.target.value)}
                        rows={3}
                      />
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
                        <input
                          className="vcs-input"
                          placeholder="Author Name (optional)"
                          value={authorName}
                          onChange={(e) => setAuthorName(e.target.value)}
                        />
                        <input
                          className="vcs-input"
                          placeholder="Author Email (optional)"
                          value={authorEmail}
                          onChange={(e) => setAuthorEmail(e.target.value)}
                        />
                      </div>
                      <div className="vcs-commit-footer">
                        <span className="vcs-char-count">{commitMessage.length} characters</span>
                        <button
                          type="submit"
                          className="vcs-btn-sm vcs-btn-primary-sm"
                          disabled={stagedCount === 0 || !commitMessage.trim()}
                        >
                          <Send size={14} />
                          <span>Commit to {status?.currentBranch || 'main'}</span>
                        </button>
                      </div>
                    </form>
                  </div>
                </div>

                {/* UNSTAGED CHANGES PANEL */}
                <div className="vcs-panel">
                  <div className="vcs-panel-header">
                    <h3 className="vcs-panel-title">
                      <span>Working Tree Changes</span>
                      <span className="vcs-badge badge-modified">{unstagedCount}</span>
                    </h3>
                    {unstagedCount > 0 && (
                      <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={handleStageAll}>
                        Stage All Changes
                      </button>
                    )}
                  </div>

                  <div className="vcs-panel-body">
                    {unstagedCount === 0 ? (
                      <div className="vcs-empty-state" style={{ padding: '2rem 0' }}>
                        <CheckCircle2 size={36} color="var(--success)" />
                        <h4 style={{ margin: '0.5rem 0 0.2rem' }}>Working Tree Clean</h4>
                        <p style={{ margin: 0, fontSize: '0.85rem' }}>
                          No unstaged or modified files in this repository.
                        </p>
                      </div>
                    ) : (
                      <div className="vcs-file-list">
                        {/* Modified / Deleted files */}
                        {status.unstaged.map((f) => (
                          <div key={f.filePath} className="vcs-file-item">
                            <div className="vcs-file-info">
                              <span className={`vcs-badge badge-${f.status.toLowerCase()}`}>{f.status}</span>
                              <span className="vcs-file-path">{f.filePath}</span>
                            </div>
                            <div className="vcs-file-actions">
                              <button
                                className="vcs-btn-sm"
                                onClick={() => handleViewDiff(f.filePath, false)}
                                title="View diff"
                              >
                                <Eye size={12} />
                                <span>Diff</span>
                              </button>
                              <button
                                className="vcs-btn-sm vcs-btn-primary-sm"
                                onClick={() => handleStageFile(f.filePath)}
                                title="Stage changes"
                              >
                                Stage
                              </button>
                              <button
                                className="vcs-btn-sm"
                                onClick={() => handleDiscard(f.filePath)}
                                title="Discard changes"
                              >
                                Discard
                              </button>
                            </div>
                          </div>
                        ))}

                        {/* Untracked files */}
                        {status.untracked.map((path) => (
                          <div key={path} className="vcs-file-item">
                            <div className="vcs-file-info">
                              <span className="vcs-badge badge-untracked">UNTRACKED</span>
                              <span className="vcs-file-path">{path}</span>
                            </div>
                            <div className="vcs-file-actions">
                              <button
                                className="vcs-btn-sm vcs-btn-primary-sm"
                                onClick={() => handleStageFile(path)}
                                title="Stage file"
                              >
                                Stage
                              </button>
                              <button
                                className="vcs-btn-sm"
                                onClick={() => handleDiscard(path)}
                                title="Delete untracked file"
                              >
                                Delete
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: BRANCHES */}
        {activeTab === 'branches' && (
          <div className="vcs-panel">
            <div className="vcs-panel-header">
              <h3 className="vcs-panel-title">
                <GitBranch size={18} />
                <span>Branches in {activeRepo?.name}</span>
              </h3>
              <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={() => setNewBranchModal(true)}>
                <Plus size={14} />
                <span>Create Branch</span>
              </button>
            </div>
            <div className="vcs-panel-body">
              {branches.length === 0 ? (
                <div className="vcs-empty-state">
                  <p>No branches found or repository has no commits yet.</p>
                </div>
              ) : (
                <div className="vcs-branch-list">
                  {branches.map((b) => (
                    <div key={b.name} className={`vcs-branch-card ${b.isCurrent ? 'is-active' : ''}`}>
                      <div className="vcs-branch-header">
                        <div className="vcs-branch-name">
                          <GitBranch size={16} color={b.isCurrent ? 'var(--primary)' : 'var(--text-secondary)'} />
                          <span>{b.name}</span>
                          {b.isCurrent && <span className="vcs-badge badge-added">Active</span>}
                        </div>
                      </div>
                      {b.headCommitSha && (
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                          Latest commit:{' '}
                          <span className="vcs-sha-badge">{b.headCommitSha}</span>{' '}
                          <span style={{ fontStyle: 'italic' }}>{b.headCommitMessage}</span>
                        </div>
                      )}
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                        {!b.isCurrent && (
                          <>
                            <button
                              className="vcs-btn-sm vcs-btn-primary-sm"
                              onClick={() => handleCheckoutBranch(b.name)}
                            >
                              Checkout
                            </button>
                            <button
                              className="vcs-btn-sm"
                              style={{ color: 'var(--danger)' }}
                              onClick={() => handleDeleteBranch(b.name)}
                            >
                              Delete
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 3: COMMIT HISTORY */}
        {activeTab === 'history' && (
          <div className="vcs-panel">
            <div className="vcs-panel-header">
              <h3 className="vcs-panel-title">
                <GitCommit size={18} />
                <span>Commit History</span>
              </h3>
              <button className="vcs-btn-sm" onClick={loadCommits}>
                <RefreshCw size={14} />
                <span>Reload</span>
              </button>
            </div>

            <div className="vcs-panel-body">
              {commits.length === 0 ? (
                <div className="vcs-empty-state">
                  <GitCommit size={40} color="var(--text-muted)" />
                  <h4>No Commits Yet</h4>
                  <p>Stage some files and create your first commit in the Changes tab.</p>
                </div>
              ) : (
                <div className="vcs-history-list">
                  {commits.map((c) => (
                    <div key={c.hash} className="vcs-commit-card">
                      <div className="vcs-commit-header">
                        <h4 className="vcs-commit-message">{c.message}</h4>
                        <span className="vcs-sha-badge">{c.shortHash}</span>
                      </div>
                      <div className="vcs-commit-meta">
                        <span>
                          <strong>{c.authorName}</strong> ({c.authorEmail})
                        </span>
                        <span>•</span>
                        <span>{new Date(c.timestamp).toLocaleString()}</span>
                        <button
                          className="vcs-btn-sm"
                          style={{ marginLeft: 'auto' }}
                          onClick={async () => {
                            try {
                              const details = await vcsService.getCommitDetails(activeRepoId, c.hash)
                              setSelectedCommit(details)
                            } catch (err) {
                              showToast('error', 'Failed to load commit details: ' + err.message)
                            }
                          }}
                        >
                          <Eye size={12} />
                          <span>View Details</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 4: GITHUB & REMOTES */}
        {activeTab === 'remotes' && (
          <div className="vcs-panel">
            <div className="vcs-panel-header">
              <h3 className="vcs-panel-title">
                <ArrowUpCircle size={18} />
                <span>Remote & GitHub Synchronization</span>
              </h3>
              <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={() => setRemoteModal(true)}>
                Configure Remote URL
              </button>
            </div>
            <div className="vcs-panel-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              <div style={{ background: 'var(--surface-muted)', padding: '1rem', borderRadius: 'var(--radius-sm)' }}>
                <h4 style={{ margin: '0 0 0.5rem' }}>Configured Remote (origin)</h4>
                <p style={{ margin: 0, fontFamily: 'monospace', fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                  {activeRepo?.remoteUrl || 'No remote URL configured yet for this repository.'}
                </p>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                <div className="vcs-branch-card">
                  <h4 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <RefreshCw size={16} />
                    <span>Fetch</span>
                  </h4>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0 }}>
                    Fetch latest commits and branches from remote without modifying local working tree.
                  </p>
                  <button
                    className="vcs-btn-sm"
                    onClick={() => {
                      setSyncModal({ open: true, op: 'FETCH' })
                      setSyncResult(null)
                    }}
                  >
                    Run Fetch
                  </button>
                </div>

                <div className="vcs-branch-card">
                  <h4 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <ArrowDownCircle size={16} />
                    <span>Pull</span>
                  </h4>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0 }}>
                    Fetch and integrate remote changes into your active branch.
                  </p>
                  <button
                    className="vcs-btn-sm vcs-btn-primary-sm"
                    onClick={() => {
                      setSyncModal({ open: true, op: 'PULL' })
                      setSyncResult(null)
                    }}
                  >
                    Run Pull
                  </button>
                </div>

                <div className="vcs-branch-card">
                  <h4 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <ArrowUpCircle size={16} />
                    <span>Push</span>
                  </h4>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: 0 }}>
                    Push local commits to your personal GitHub repository branch.
                  </p>
                  <button
                    className="vcs-btn-sm vcs-btn-primary-sm"
                    onClick={() => {
                      setSyncModal({ open: true, op: 'PUSH' })
                      setSyncResult(null)
                    }}
                  >
                    Run Push
                  </button>
                </div>
              </div>

              <div className="vcs-alert vcs-alert-info">
                <ShieldCheck size={18} />
                <span>
                  <strong>Security Note:</strong> Your Personal Access Token (PAT) is used strictly in-memory
                  for authenticated GitHub requests and is never stored on disk or committed into source control.
                </span>
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: ALL REPOSITORIES */}
        {activeTab === 'repositories' && (
          <div className="vcs-panel">
            <div className="vcs-panel-header">
              <h3 className="vcs-panel-title">
                <FolderGit2 size={18} />
                <span>Managed Local Repositories ({repositories.length})</span>
              </h3>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <button className="vcs-btn-sm vcs-btn-primary-sm" onClick={() => setCreateRepoModal(true)}>
                  <Plus size={14} />
                  <span>Create Repo</span>
                </button>
                <button
                  className="vcs-btn-sm"
                  onClick={() => {
                    setCloneRepoModal(true)
                    setCloneResult(null)
                    setCloneError(null)
                  }}
                >
                  <Download size={14} />
                  <span>Clone Repo</span>
                </button>
                <button className="vcs-btn-sm" onClick={() => setRegisterRepoModal(true)}>
                  <FolderOpen size={14} />
                  <span>Open Local</span>
                </button>
              </div>
            </div>
            <div className="vcs-panel-body">
              {repositories.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-secondary)' }}>
                  <FolderGit2 size={40} style={{ opacity: 0.35, margin: '0 auto 0.75rem' }} />
                  <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                    {user?.role === 'STUDENT' ? 'No Repositories Found for Your Account' : 'No Repositories Registered'}
                  </h3>
                  <p style={{ maxWidth: '420px', margin: '0 auto 1.5rem', fontSize: '0.88rem' }}>
                    {user?.role === 'STUDENT'
                      ? 'You can only view repositories belonging to your account. Click Initialize Repo, Clone Repo, or Open Local above to create your own repository.'
                      : 'No repositories exist in the system yet. Click Initialize Repo or Clone Repo above to get started.'}
                  </p>
                  <button className="vcs-btn vcs-btn-primary" onClick={() => setCreateRepoModal(true)} style={{ margin: '0 auto' }}>
                    <Plus size={15} />
                    <span>Initialize New Repository</span>
                  </button>
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
                  {repositories.map((r) => (
                    <div
                      key={r.id}
                      className={`vcs-branch-card ${r.id === activeRepoId ? 'is-active' : ''}`}
                      style={{ cursor: 'pointer' }}
                      onClick={() => {
                        setActiveRepoId(r.id)
                        setActiveTab('changes')
                      }}
                    >
                      <div className="vcs-branch-header">
                        <div className="vcs-branch-name">
                          <FolderGit2 size={18} color="var(--primary)" />
                          <span>{r.name}</span>
                        </div>
                        <span className="vcs-sha-badge">{r.currentBranch}</span>
                      </div>
                      <p style={{ margin: '0.25rem 0', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        {r.description || 'No description provided.'}
                      </p>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', margin: '0.35rem 0', flexWrap: 'wrap' }}>
                        <span style={{ fontSize: '0.72rem', background: '#f1f5f9', color: '#475569', padding: '0.15rem 0.45rem', borderRadius: '4px', fontWeight: 500 }}>
                          Owner: {r.ownerUsername || 'You'}
                        </span>
                        {r.remoteUrl && (
                          <span style={{ fontSize: '0.72rem', background: '#e0f2fe', color: '#0369a1', padding: '0.15rem 0.45rem', borderRadius: '4px', fontWeight: 500 }}>
                            Linked Remote
                          </span>
                        )}
                      </div>
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                        {r.diskPath}
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem' }}>
                        <span style={{ fontSize: '0.8rem', fontWeight: 600 }}>
                          {r.commitCount} {r.commitCount === 1 ? 'commit' : 'commits'}
                        </span>
                        <button
                          className="vcs-btn-sm"
                          style={{ color: 'var(--danger)' }}
                          onClick={(e) => {
                            e.stopPropagation()
                            handleDeleteRepo(r.id, false)
                          }}
                        >
                          <Trash2 size={12} />
                          <span>Unregister</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* MODAL: UNIFIED DIFF VIEWER */}
        {diffModal.open && (
          <div className="vcs-modal-backdrop" onClick={() => setDiffModal({ open: false, path: '', staged: false, data: null })}>
            <div className="vcs-modal" style={{ maxWidth: '800px' }} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">
                  Diff: {diffModal.path} {diffModal.staged ? '(Staged)' : '(Working Tree)'}
                </h3>
                <button
                  className="vcs-btn-sm"
                  onClick={() => setDiffModal({ open: false, path: '', staged: false, data: null })}
                >
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                {diffModal.data?.patch ? (
                  <pre className="vcs-diff-viewer">
                    {diffModal.data.patch.split('\n').map((line, idx) => {
                      let lineClass = ''
                      if (line.startsWith('+++') || line.startsWith('---')) lineClass = 'diff-line-meta'
                      else if (line.startsWith('+')) lineClass = 'diff-line-add'
                      else if (line.startsWith('-')) lineClass = 'diff-line-del'
                      else if (line.startsWith('@@')) lineClass = 'diff-line-header'
                      return (
                        <span key={idx} className={lineClass}>
                          {line}
                          {'\n'}
                        </span>
                      )
                    })}
                  </pre>
                ) : (
                  <p style={{ textAlign: 'center', color: 'var(--text-muted)' }}>No textual diff available for this file.</p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* MODAL: COMMIT DETAILS */}
        {selectedCommit && (
          <div className="vcs-modal-backdrop" onClick={() => setSelectedCommit(null)}>
            <div className="vcs-modal" style={{ maxWidth: '750px' }} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">Commit {selectedCommit.commit.shortHash}</h3>
                <button className="vcs-btn-sm" onClick={() => setSelectedCommit(null)}>
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                <div>
                  <h4 style={{ margin: '0 0 0.5rem' }}>{selectedCommit.commit.message}</h4>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    <span>Author: {selectedCommit.commit.authorName} ({selectedCommit.commit.authorEmail})</span>
                    <br />
                    <span>Date: {new Date(selectedCommit.commit.timestamp).toLocaleString()}</span>
                  </div>
                </div>

                <div style={{ margin: '0.5rem 0' }}>
                  <strong>Files Changed ({selectedCommit.filesChanged.length}):</strong>
                  <div style={{ marginTop: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {selectedCommit.filesChanged.map((fc, i) => (
                      <div key={i} style={{ background: 'var(--surface-muted)', padding: '0.5rem', borderRadius: '4px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                          <span>{fc.newPath || fc.oldPath}</span>
                          <span>
                            <strong style={{ color: 'var(--success)' }}>+{fc.additions}</strong> /{' '}
                            <strong style={{ color: 'var(--danger)' }}>-{fc.deletions}</strong>
                          </span>
                        </div>
                        {fc.patch && (
                          <pre className="vcs-diff-viewer" style={{ maxHeight: '180px', marginTop: '0.5rem' }}>
                            {fc.patch}
                          </pre>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* MODAL: NEW BRANCH */}
        {newBranchModal && (
          <div className="vcs-modal-backdrop" onClick={() => setNewBranchModal(false)}>
            <form className="vcs-modal" onSubmit={handleCreateBranch} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">Create New Branch</h3>
                <button type="button" className="vcs-btn-sm" onClick={() => setNewBranchModal(false)}>
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                <div className="vcs-form-group">
                  <label className="vcs-label">Branch Name</label>
                  <input
                    className="vcs-input"
                    placeholder="e.g. feature/auth-flow or bugfix/task-modal"
                    value={newBranchName}
                    onChange={(e) => setNewBranchName(e.target.value)}
                    required
                  />
                  <span className="vcs-help-text">
                    Will branch from current HEAD ({status?.currentBranch || 'main'}).
                  </span>
                </div>
              </div>
              <div className="vcs-modal-footer">
                <button type="button" className="vcs-btn-sm" onClick={() => setNewBranchModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="vcs-btn-sm vcs-btn-primary-sm">
                  Create Branch
                </button>
              </div>
            </form>
          </div>
        )}

        {/* MODAL: CREATE REPOSITORY */}
        {createRepoModal && (
          <div className="vcs-modal-backdrop" onClick={() => setCreateRepoModal(false)}>
            <form className="vcs-modal" onSubmit={handleCreateRepo} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">Create New Git Repository</h3>
                <button type="button" className="vcs-btn-sm" onClick={() => setCreateRepoModal(false)}>
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                <div className="vcs-form-group">
                  <label className="vcs-label">Repository Name</label>
                  <input
                    className="vcs-input"
                    placeholder="e.g. my-awesome-project"
                    value={newRepoData.name}
                    onChange={(e) => setNewRepoData({ ...newRepoData, name: e.target.value })}
                    required
                  />
                </div>
                <div className="vcs-form-group">
                  <label className="vcs-label">Description (optional)</label>
                  <input
                    className="vcs-input"
                    placeholder="Brief description of the repository"
                    value={newRepoData.description}
                    onChange={(e) => setNewRepoData({ ...newRepoData, description: e.target.value })}
                  />
                </div>
                <div className="vcs-form-group">
                  <label className="vcs-label">Default Branch</label>
                  <input
                    className="vcs-input"
                    value={newRepoData.defaultBranch}
                    onChange={(e) => setNewRepoData({ ...newRepoData, defaultBranch: e.target.value })}
                    required
                  />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
                  <input
                    type="checkbox"
                    id="initReadme"
                    checked={newRepoData.initReadme}
                    onChange={(e) => setNewRepoData({ ...newRepoData, initReadme: e.target.checked })}
                  />
                  <label htmlFor="initReadme" style={{ fontSize: '0.88rem' }}>
                    Initialize with a README.md and initial commit
                  </label>
                </div>
              </div>
              <div className="vcs-modal-footer">
                <button type="button" className="vcs-btn-sm" onClick={() => setCreateRepoModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="vcs-btn-sm vcs-btn-primary-sm">
                  Initialize Repository
                </button>
              </div>
            </form>
          </div>
        )}

        {/* MODAL: REGISTER LOCAL REPO */}
        {registerRepoModal && (
          <div className="vcs-modal-backdrop" onClick={() => setRegisterRepoModal(false)}>
            <form className="vcs-modal" onSubmit={handleRegisterRepo} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">Open Local Git Repository</h3>
                <button type="button" className="vcs-btn-sm" onClick={() => setRegisterRepoModal(false)}>
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                <div className="vcs-form-group">
                  <label className="vcs-label">Repository Name</label>
                  <input
                    className="vcs-input"
                    placeholder="e.g. ProjectSync-Local"
                    value={regRepoData.name}
                    onChange={(e) => setRegRepoData({ ...regRepoData, name: e.target.value })}
                    required
                  />
                </div>
                <div className="vcs-form-group">
                  <label className="vcs-label">Local Filesystem Path</label>
                  <input
                    className="vcs-input"
                    placeholder="e.g. C:\Users\DELL\Desktop\MyGitProject"
                    value={regRepoData.path}
                    onChange={(e) => setRegRepoData({ ...regRepoData, path: e.target.value })}
                    required
                  />
                  <span className="vcs-help-text">Must be a directory containing a valid .git folder.</span>
                </div>
                <div className="vcs-form-group">
                  <label className="vcs-label">Description (optional)</label>
                  <input
                    className="vcs-input"
                    placeholder="Brief description"
                    value={regRepoData.description}
                    onChange={(e) => setRegRepoData({ ...regRepoData, description: e.target.value })}
                  />
                </div>
              </div>
              <div className="vcs-modal-footer">
                <button type="button" className="vcs-btn-sm" onClick={() => setRegisterRepoModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="vcs-btn-sm vcs-btn-primary-sm">
                  Register Repository
                </button>
              </div>
            </form>
          </div>
        )}

        {/* MODAL: CLONE REPOSITORY */}
        {cloneRepoModal && (
          <div
            className="vcs-modal-backdrop"
            onClick={() => {
              if (!cloneLoading) setCloneRepoModal(false)
            }}
          >
            <div className="vcs-modal" style={{ maxWidth: '650px' }} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Download size={18} color="var(--primary)" />
                  <span>Clone Remote Git Repository</span>
                </h3>
                <button
                  type="button"
                  className="vcs-btn-sm"
                  disabled={cloneLoading}
                  onClick={() => setCloneRepoModal(false)}
                >
                  <X size={16} />
                </button>
              </div>

              {/* SUCCESS VIEW */}
              {cloneResult ? (
                <div className="vcs-modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div className="vcs-alert vcs-alert-success">
                    <CheckCircle2 size={20} style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Repository cloned successfully!</strong>
                      <div style={{ fontSize: '0.82rem', marginTop: '0.2rem' }}>
                        The remote repository was cloned and registered in local version control.
                      </div>
                    </div>
                  </div>

                  <div className="vcs-clone-detail-card">
                    <div className="vcs-clone-detail-item">
                      <span className="vcs-clone-detail-label">Repository Name</span>
                      <span className="vcs-clone-detail-val">{cloneResult.repository.name}</span>
                    </div>
                    <div className="vcs-clone-detail-item">
                      <span className="vcs-clone-detail-label">Branch</span>
                      <span className="vcs-clone-detail-val">{cloneResult.repository.defaultBranch}</span>
                    </div>
                    <div className="vcs-clone-detail-item">
                      <span className="vcs-clone-detail-label">Remote URL</span>
                      <span className="vcs-clone-detail-val" style={{ fontSize: '0.8rem', wordBreak: 'break-all' }}>
                        {cloneResult.repository.remoteUrl}
                      </span>
                    </div>
                    <div className="vcs-clone-detail-item">
                      <span className="vcs-clone-detail-label">Disk Path</span>
                      <span className="vcs-clone-detail-val" style={{ fontSize: '0.78rem', wordBreak: 'break-all' }}>
                        {cloneResult.repository.diskPath}
                      </span>
                    </div>
                  </div>

                  <div>
                    <label className="vcs-label" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.4rem' }}>
                      <Terminal size={14} />
                      <span>Clone Execution Output</span>
                    </label>
                    <div className="vcs-terminal-box">
                      {cloneResult.cloneOutput || 'Clone executed with status: OK'}
                    </div>
                  </div>

                  <div className="vcs-modal-footer" style={{ marginTop: '0.5rem', padding: 0 }}>
                    <button
                      type="button"
                      className="vcs-btn-sm"
                      onClick={() => setCloneRepoModal(false)}
                    >
                      Close
                    </button>
                    <button
                      type="button"
                      className="vcs-btn-sm vcs-btn-primary-sm"
                      onClick={() => {
                        setCloneRepoModal(false)
                        setActiveRepoId(cloneResult.repository.id)
                        setActiveTab('changes')
                      }}
                    >
                      <FolderGit2 size={14} />
                      <span>Open in Dashboard</span>
                    </button>
                  </div>
                </div>
              ) : (
                /* FORM VIEW */
                <form onSubmit={handleCloneRepo}>
                  <div className="vcs-modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                    <div style={{ padding: '0.65rem 0.85rem', background: '#eff6ff', borderRadius: '8px', border: '1px solid #bfdbfe', fontSize: '0.82rem', color: '#1e40af', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <ShieldCheck size={18} style={{ flexShrink: 0, color: '#2563eb' }} />
                      <span><strong>Self-Hosted Git:</strong> Repositories are hosted and managed directly on your local ProjectSync-AI server. No external GitHub account or connection needed.</span>
                    </div>

                    {/* Source Type Selector */}
                    <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <button
                        type="button"
                        className={`vcs-btn-sm ${cloneSourceType === 'internal' ? 'vcs-btn-primary-sm' : ''}`}
                        onClick={() => {
                          setCloneSourceType('internal')
                          setCloneData({ ...cloneData, remoteUrl: '', name: '' })
                        }}
                      >
                        <Database size={13} />
                        <span>Clone Internal Repo</span>
                      </button>

                      <button
                        type="button"
                        className={`vcs-btn-sm ${cloneSourceType === 'custom' ? 'vcs-btn-primary-sm' : ''}`}
                        onClick={() => {
                          setCloneSourceType('custom')
                          setCloneData({ ...cloneData, remoteUrl: '', name: '' })
                        }}
                      >
                        <FolderGit2 size={13} />
                        <span>Local Path / URL</span>
                      </button>
                    </div>

                    {cloneSourceType === 'internal' ? (
                      <div className="vcs-form-group">
                        <label className="vcs-label">Select Internal Repository to Clone *</label>
                        <select
                          className="vcs-input"
                          value={cloneData.remoteUrl}
                          onChange={(e) => {
                            const val = e.target.value
                            setCloneData({
                              ...cloneData,
                              remoteUrl: val,
                              name: val ? `${val}-clone` : '',
                              isNameAutoDerived: false,
                            })
                          }}
                          disabled={cloneLoading}
                          required
                        >
                          <option value="">-- Choose an internal team repository --</option>
                          {repositories.map((r) => (
                            <option key={r.id} value={r.name}>
                              {r.name} (branch: {r.defaultBranch}, owner: {r.ownerUsername || 'team'})
                            </option>
                          ))}
                        </select>
                        <span className="vcs-help-text">
                          Clones directly from the ProjectSync-AI local repository store.
                        </span>
                      </div>
                    ) : (
                      <div className="vcs-form-group">
                        <label className="vcs-label">Local Filesystem Path or Internal Git URL *</label>
                        <input
                          className="vcs-input"
                          placeholder="e.g. C:\Users\DELL\.projectsync\repositories\team-repo or internal-repo-name"
                          value={cloneData.remoteUrl}
                          onChange={(e) => handleCloneUrlChange(e.target.value)}
                          disabled={cloneLoading}
                          required
                        />
                        <span className="vcs-help-text">
                          Specify a local folder containing a Git repo or an internal server clone URL.
                        </span>
                      </div>
                    )}

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                      <div className="vcs-form-group">
                        <label className="vcs-label">Local Destination Name</label>
                        <input
                          className="vcs-input"
                          placeholder="Auto-derived if empty"
                          value={cloneData.name}
                          onChange={(e) => setCloneData({ ...cloneData, name: e.target.value, isNameAutoDerived: false })}
                          disabled={cloneLoading}
                        />
                      </div>

                      <div className="vcs-form-group">
                        <label className="vcs-label">Branch (optional)</label>
                        <input
                          className="vcs-input"
                          placeholder="e.g. main (default HEAD)"
                          value={cloneData.branch}
                          onChange={(e) => setCloneData({ ...cloneData, branch: e.target.value })}
                          disabled={cloneLoading}
                        />
                      </div>
                    </div>

                    <div className="vcs-form-group">
                      <label className="vcs-label">Description (optional)</label>
                      <input
                        className="vcs-input"
                        placeholder="Brief notes about this cloned repository"
                        value={cloneData.description}
                        onChange={(e) => setCloneData({ ...cloneData, description: e.target.value })}
                        disabled={cloneLoading}
                      />
                    </div>

                    {/* Collapsible Auth Section */}
                    {cloneSourceType === 'custom' && (
                      <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '0.65rem 0.85rem' }}>
                        <button
                          type="button"
                          onClick={() => setShowCloneAuth(!showCloneAuth)}
                          style={{
                            background: 'none',
                            border: 'none',
                            padding: 0,
                            cursor: 'pointer',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            width: '100%',
                            fontSize: '0.86rem',
                            fontWeight: 600,
                            color: 'var(--text-primary)',
                          }}
                        >
                          <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            <ShieldCheck size={16} color="var(--primary)" />
                            <span>Optional Credentials (Internal Network Shares)</span>
                          </span>
                          {showCloneAuth ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                        </button>

                        {showCloneAuth && (
                          <div style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
                            <div className="vcs-form-group">
                              <label className="vcs-label">Username</label>
                              <input
                                className="vcs-input"
                                placeholder="Username"
                                value={cloneData.username}
                                onChange={(e) => setCloneData({ ...cloneData, username: e.target.value })}
                                disabled={cloneLoading}
                              />
                            </div>
                            <div className="vcs-form-group">
                              <label className="vcs-label">Password / Token</label>
                              <input
                                type="password"
                                className="vcs-input"
                                placeholder="Password or access token"
                                value={cloneData.personalAccessToken}
                                onChange={(e) => setCloneData({ ...cloneData, personalAccessToken: e.target.value })}
                                disabled={cloneLoading}
                              />
                            </div>
                          </div>
                        )}
                      </div>
                    )}

                    {/* ERROR ALERT */}
                    {cloneError && (
                      <div className="vcs-alert vcs-alert-error" style={{ whiteSpace: 'pre-wrap' }}>
                        <AlertCircle size={18} style={{ flexShrink: 0 }} />
                        <span>{cloneError}</span>
                      </div>
                    )}
                  </div>

                  <div className="vcs-modal-footer">
                    <button
                      type="button"
                      className="vcs-btn-sm"
                      onClick={() => setCloneRepoModal(false)}
                      disabled={cloneLoading}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="vcs-btn-sm vcs-btn-primary-sm"
                      disabled={cloneLoading}
                    >
                      {cloneLoading ? (
                        <>
                          <RefreshCw size={14} className="vcs-spin" />
                          <span>Cloning Repository...</span>
                        </>
                      ) : (
                        <>
                          <Download size={14} />
                          <span>Clone Repository</span>
                        </>
                      )}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        )}

        {/* MODAL: CONFIGURE REMOTE */}
        {remoteModal && (
          <div className="vcs-modal-backdrop" onClick={() => setRemoteModal(false)}>
            <form className="vcs-modal" onSubmit={handleSaveRemote} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">Configure Remote Repository</h3>
                <button type="button" className="vcs-btn-sm" onClick={() => setRemoteModal(false)}>
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                <div className="vcs-form-group">
                  <label className="vcs-label">Remote URL (origin)</label>
                  <input
                    className="vcs-input"
                    placeholder="https://github.com/your-username/your-repo.git"
                    value={remoteUrl}
                    onChange={(e) => setRemoteUrl(e.target.value)}
                    required
                  />
                  <span className="vcs-help-text">HTTPS remote URL from your personal GitHub account.</span>
                </div>
              </div>
              <div className="vcs-modal-footer">
                <button type="button" className="vcs-btn-sm" onClick={() => setRemoteModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="vcs-btn-sm vcs-btn-primary-sm">
                  Save Remote
                </button>
              </div>
            </form>
          </div>
        )}

        {/* MODAL: GITHUB SYNC (Fetch/Pull/Push) */}
        {syncModal.open && (
          <div className="vcs-modal-backdrop" onClick={() => setSyncModal({ open: false, op: 'PULL' })}>
            <form className="vcs-modal" onSubmit={handleExecuteSync} onClick={(e) => e.stopPropagation()}>
              <div className="vcs-modal-header">
                <h3 className="vcs-modal-title">GitHub {syncModal.op}</h3>
                <button
                  type="button"
                  className="vcs-btn-sm"
                  onClick={() => setSyncModal({ open: false, op: 'PULL' })}
                >
                  <X size={16} />
                </button>
              </div>
              <div className="vcs-modal-body">
                <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--text-secondary)' }}>
                  Target remote: <strong>origin</strong> (
                  {activeRepo?.remoteUrl || 'No remote URL set'})
                </p>

                <div className="vcs-form-group">
                  <label className="vcs-label">GitHub Username</label>
                  <input
                    className="vcs-input"
                    placeholder="Your GitHub account username"
                    value={githubAuth.username}
                    onChange={(e) => setGithubAuth({ ...githubAuth, username: e.target.value })}
                  />
                </div>

                <div className="vcs-form-group">
                  <label className="vcs-label">GitHub Personal Access Token (PAT)</label>
                  <input
                    type="password"
                    className="vcs-input"
                    placeholder="ghp_xxxxxxxxxxxxxxxxxxxx"
                    value={githubAuth.token}
                    onChange={(e) => setGithubAuth({ ...githubAuth, token: e.target.value })}
                  />
                  <span className="vcs-help-text">
                    Stored in-memory for this session only; never written to disk or logs.
                  </span>
                </div>

                {syncResult && (
                  <div
                    className={`vcs-alert vcs-alert-${syncResult.success ? 'success' : 'error'}`}
                    style={{ whiteSpace: 'pre-wrap' }}
                  >
                    <strong>{syncResult.message}</strong>
                    {syncResult.details && <div style={{ fontSize: '0.78rem', marginTop: '0.25rem' }}>{syncResult.details}</div>}
                  </div>
                )}
              </div>
              <div className="vcs-modal-footer">
                <button
                  type="button"
                  className="vcs-btn-sm"
                  onClick={() => setSyncModal({ open: false, op: 'PULL' })}
                >
                  Close
                </button>
                <button type="submit" className="vcs-btn-sm vcs-btn-primary-sm" disabled={loading}>
                  {loading ? 'Processing...' : `Execute ${syncModal.op}`}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
