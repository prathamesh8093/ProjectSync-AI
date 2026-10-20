import { useEffect, useState } from 'react'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ListChecks,
  Sparkles,
  X,
  Copy,
  Check,
  Calendar,
  Layers,
} from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import {
  createProposal,
  getMyProposal,
  updateProposal,
  submitProposal,
  generateAiProposalDraft,
  getSprintBreakdown,
} from '../services/projectProposalService.js'
import { getMyTeam, getTeamMembers } from '../services/teamService.js'
import Input from '../components/ui/Input.jsx'
import Textarea from '../components/ui/Textarea.jsx'
import Button from '../components/ui/Button.jsx'
import Badge from '../components/ui/Badge.jsx'
import '../styles/projectProposal.css'

const studentNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/student/dashboard' },
  { label: 'My Profile', icon: UserRound, path: '/student/profile' },
  { label: 'My Team', icon: Users, path: '/student/team' },
  { label: 'Project Proposal', icon: FileText, path: '/student/proposal' },
  { label: 'Tasks / Projects', icon: ListChecks, path: null },
]

const emptyForm = { title: '', description: '', objectives: '', technologyStack: '' }

const statusTone = {
  DRAFT: 'neutral',
  SUBMITTED: 'primary',
  APPROVED: 'success',
  REJECTED: 'neutral',
}

function toFormData(proposal) {
  return {
    title: proposal.title || '',
    description: proposal.description || '',
    objectives: proposal.objectives || '',
    technologyStack: proposal.technologyStack || '',
  }
}

function formatDateTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function StudentProjectProposal() {
  const { user } = useAuth()

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [proposal, setProposal] = useState(null)
  const [isLeader, setIsLeader] = useState(false)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createFormData, setCreateFormData] = useState(emptyForm)
  const [createErrors, setCreateErrors] = useState({})
  const [creating, setCreating] = useState(false)
  const [createSuccess, setCreateSuccess] = useState(false)

  const [editing, setEditing] = useState(false)
  const [editFormData, setEditFormData] = useState(emptyForm)
  const [editErrors, setEditErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [saveSuccess, setSaveSuccess] = useState(false)

  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [submitSuccess, setSubmitSuccess] = useState(false)

  // AI Proposal Drafter State
  const [aiModalOpen, setAiModalOpen] = useState(false)
  const [aiIdea, setAiIdea] = useState('')
  const [aiTechStack, setAiTechStack] = useState('')
  const [aiWeeks, setAiWeeks] = useState(12)
  const [aiGenerating, setAiGenerating] = useState(false)
  const [aiError, setAiError] = useState('')
  const [aiDraft, setAiDraft] = useState(null)
  const [copied, setCopied] = useState(false)

  const openAiModal = () => {
    setAiError('')
    setCopied(false)
    if (editing && editFormData.title) {
      if (!aiIdea) setAiIdea(editFormData.title)
      if (!aiTechStack) setAiTechStack(editFormData.technologyStack)
    } else if (showCreateForm && createFormData.title) {
      if (!aiIdea) setAiIdea(createFormData.title)
      if (!aiTechStack) setAiTechStack(createFormData.technologyStack)
    }
    setAiModalOpen(true)
  }

  const closeAiModal = () => {
    setAiModalOpen(false)
    setAiError('')
  }

  const handleGenerateDraft = async (e) => {
    if (e) e.preventDefault()
    if (!aiIdea.trim()) {
      setAiError('Please enter a project idea or problem statement.')
      return
    }
    setAiError('')
    setAiGenerating(true)
    try {
      const res = await generateAiProposalDraft({
        idea: aiIdea.trim(),
        technologyStack: aiTechStack.trim() || undefined,
        durationWeeks: Number(aiWeeks) || 12,
      })
      setAiDraft(res.data)
    } catch (err) {
      setAiError(
        err.response?.data?.message ||
        'Failed to generate proposal draft. Please try again.'
      )
    } finally {
      setAiGenerating(false)
    }
  }

  const handleApplyDraft = () => {
    if (!aiDraft) return

    const appliedData = {
      title: aiDraft.title || '',
      description: aiDraft.formattedDescription || aiDraft.abstractText || '',
      objectives: aiDraft.formattedObjectives || aiDraft.objectives || '',
      technologyStack: aiDraft.technologyStack || '',
    }

    if (editing) {
      setEditFormData(appliedData)
      setEditErrors({})
    } else {
      setCreateFormData(appliedData)
      setShowCreateForm(true)
      setCreateErrors({})
    }
    setAiModalOpen(false)
  }

  const handleCopyMarkdown = () => {
    if (!aiDraft) return
    const text = `# ${aiDraft.title}\n\n` +
      `## Technology Stack\n${aiDraft.technologyStack}\n\n` +
      `## Objectives\n${aiDraft.formattedObjectives || aiDraft.objectives}\n\n` +
      `${aiDraft.formattedDescription}`

    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2500)
  }

  // AI Sprint & Story Breakdown State
  const [sprintBreakdownOpen, setSprintBreakdownOpen] = useState(false)
  const [sprintBreakdownData, setSprintBreakdownData] = useState(null)
  const [loadingSprintBreakdown, setLoadingSprintBreakdown] = useState(false)
  const [sprintBreakdownError, setSprintBreakdownError] = useState('')
  const [copiedStories, setCopiedStories] = useState(false)

  const openSprintBreakdown = async () => {
    if (!proposal?.id) return
    setSprintBreakdownOpen(true)
    setSprintBreakdownError('')
    if (sprintBreakdownData) return
    setLoadingSprintBreakdown(true)
    try {
      const res = await getSprintBreakdown(proposal.id)
      setSprintBreakdownData(res.data)
    } catch {
      setSprintBreakdownError('Failed to generate sprint breakdown. Please try again.')
    } finally {
      setLoadingSprintBreakdown(false)
    }
  }

  const handleCopyStories = () => {
    if (!sprintBreakdownData) return
    let text = `# ${sprintBreakdownData.projectTitle} - Sprint Backlog (${sprintBreakdownData.totalStoryPoints} Story Points)\n\n`
    sprintBreakdownData.sprints?.forEach((s) => {
      text += `## ${s.sprintName} (${s.plannedPoints} pts)\nGoal: ${s.sprintGoal}\n\n`
      s.userStories?.forEach((u) => {
        text += `### [${u.storyId}] ${u.title} (${u.storyPoints} pts - ${u.recommendedRole})\n`
        text += `${u.userStoryText}\n\nAcceptance Criteria:\n`
        u.acceptanceCriteria?.forEach((ac) => {
          text += `- ${ac}\n`
        })
        text += '\n'
      })
    })
    navigator.clipboard.writeText(text)
    setCopiedStories(true)
    setTimeout(() => setCopiedStories(false), 2500)
  }

  useEffect(() => {
    let isMounted = true

    const loadLeaderStatus = async () => {
      try {
        const teamResponse = await getMyTeam()
        const membersResponse = await getTeamMembers(teamResponse.data.id)
        if (!isMounted) return
        const currentMember = membersResponse.data.find((m) => m.email === user.email)
        setIsLeader(currentMember?.teamRole === 'LEADER')
      } catch {
        if (!isMounted) return
        setIsLeader(false)
      }
    }

    const loadProposal = async () => {
      try {
        const response = await getMyProposal()
        if (!isMounted) return
        setProposal(response.data)
      } catch (err) {
        if (!isMounted) return
        if (err.response && err.response.status === 404) {
          setProposal(null)
        } else {
          setLoadError('Unable to load your proposal right now. Please try again later.')
        }
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadLeaderStatus()
    loadProposal()
    return () => {
      isMounted = false
    }
  }, [user.email])

  const validate = (data) => {
    const newErrors = {}

    if (!data.title.trim()) {
      newErrors.title = 'Project title is required.'
    } else if (data.title.trim().length > 200) {
      newErrors.title = 'Title must be 200 characters or fewer.'
    }

    if (!data.description.trim()) {
      newErrors.description = 'Project description is required.'
    }

    return newErrors
  }

  const buildPayload = (data) => ({
    title: data.title.trim(),
    description: data.description.trim(),
    objectives: data.objectives.trim() || null,
    technologyStack: data.technologyStack.trim() || null,
  })

  const handleCreateChange = (e) => {
    setCreateFormData({ ...createFormData, [e.target.name]: e.target.value })
  }

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    setCreateSuccess(false)

    const newErrors = validate(createFormData)
    setCreateErrors(newErrors)
    if (Object.keys(newErrors).length > 0) return

    setCreating(true)
    try {
      const response = await createProposal(buildPayload(createFormData))
      setProposal(response.data)
      setShowCreateForm(false)
      setCreateFormData(emptyForm)
      setCreateSuccess(true)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setCreateErrors({ title: 'Your team already has a proposal.' })
      } else if (err.response && err.response.status === 403) {
        setCreateErrors({ title: 'Only the team leader can create a proposal.' })
      } else if (err.response && err.response.status === 404) {
        setCreateErrors({ title: 'You need to be part of a team before creating a proposal.' })
      } else if (err.response && err.response.status === 400) {
        setCreateErrors({ title: 'Please check the form and try again.' })
      } else {
        setCreateErrors({ title: 'Something went wrong while creating the proposal. Please try again.' })
      }
    } finally {
      setCreating(false)
    }
  }

  const startEditing = () => {
    setEditFormData(toFormData(proposal))
    setEditErrors({})
    setSaveError('')
    setSaveSuccess(false)
    setEditing(true)
  }

  const cancelEditing = () => {
    setEditing(false)
    setEditErrors({})
    setSaveError('')
  }

  const handleEditChange = (e) => {
    setEditFormData({ ...editFormData, [e.target.name]: e.target.value })
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    setSaveError('')
    setSaveSuccess(false)

    const newErrors = validate(editFormData)
    setEditErrors(newErrors)
    if (Object.keys(newErrors).length > 0) return

    setSaving(true)
    try {
      const response = await updateProposal(proposal.id, buildPayload(editFormData))
      setProposal(response.data)
      setEditing(false)
      setSaveSuccess(true)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setSaveError('This proposal can no longer be edited.')
      } else if (err.response && err.response.status === 403) {
        setSaveError('Only the team leader can edit this proposal.')
      } else if (err.response && err.response.status === 400) {
        setSaveError('Please check the highlighted fields and try again.')
      } else {
        setSaveError('Something went wrong while saving. Please try again.')
      }
    } finally {
      setSaving(false)
    }
  }

  const handleSubmitProposal = async () => {
    setSubmitError('')
    setSubmitSuccess(false)
    setSubmitting(true)
    try {
      const response = await submitProposal(proposal.id)
      setProposal(response.data)
      setSubmitSuccess(true)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setSubmitError('This proposal has already been submitted or reviewed.')
      } else if (err.response && err.response.status === 403) {
        setSubmitError('Only the team leader can submit this proposal.')
      } else {
        setSubmitError('Something went wrong while submitting. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  const isDraft = proposal?.status === 'DRAFT'
  const canManage = isDraft && isLeader
  const hasReview = Boolean(proposal?.reviewDecision)
  const isAwaitingReview = proposal?.status === 'SUBMITTED' && !hasReview

  return (
    <DashboardLayout navItems={studentNavItems}>
      <h1 className="content-title">Project Proposal</h1>
      <p className="content-subtitle">Manage your team's project proposal.</p>

      {loading ? (
        <div className="proposal-card">
          <p className="proposal-loading">Loading your proposal...</p>
        </div>
      ) : loadError ? (
        <div className="proposal-card">
          <p className="proposal-load-error">{loadError}</p>
        </div>
      ) : !proposal ? (
        <div className="proposal-empty-card">
          <h2>No proposal yet</h2>
          <p>
            {isLeader
              ? "Create your team's project proposal to get started."
              : 'Your team leader has not created a project proposal yet.'}
          </p>

          {createSuccess && (
            <div className="proposal-save-success">Proposal created successfully.</div>
          )}

          {isLeader && (
            !showCreateForm ? (
              <div className="proposal-form-actions">
                <Button onClick={() => setShowCreateForm(true)}>Create Proposal</Button>
                <Button variant="secondary" onClick={openAiModal}>
                  <Sparkles size={15} style={{ marginRight: 6 }} />
                  Draft with AI
                </Button>
              </div>
            ) : (
              <form className="proposal-form" onSubmit={handleCreateSubmit} noValidate>
                <div className="ai-proposal-banner">
                  <div className="ai-proposal-banner-info">
                    <Sparkles size={16} className="ai-sparkle-icon" />
                    <div>
                      <strong>Need help drafting your proposal?</strong>
                      <p>Use AI to generate academic abstract, literature review, gaps, and timeline.</p>
                    </div>
                  </div>
                  <Button type="button" variant="secondary" onClick={openAiModal}>
                    ✨ AI Drafter
                  </Button>
                </div>

                <Input
                  id="title"
                  name="title"
                  label="Project Title"
                  placeholder="e.g. ProjectSync AI"
                  value={createFormData.title}
                  onChange={handleCreateChange}
                  error={createErrors.title}
                />
                <Textarea
                  id="description"
                  name="description"
                  label="Project Description"
                  placeholder="Describe what your project does..."
                  value={createFormData.description}
                  onChange={handleCreateChange}
                  error={createErrors.description}
                />
                <Textarea
                  id="objectives"
                  name="objectives"
                  label="Objectives"
                  placeholder="What are the goals of this project?"
                  value={createFormData.objectives}
                  onChange={handleCreateChange}
                />
                <Input
                  id="technologyStack"
                  name="technologyStack"
                  label="Technology Stack"
                  placeholder="e.g. React, Spring Boot, PostgreSQL"
                  value={createFormData.technologyStack}
                  onChange={handleCreateChange}
                />

                <div className="proposal-form-actions">
                  <Button type="submit" loading={creating}>
                    {creating ? 'Creating...' : 'Create Proposal'}
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      setShowCreateForm(false)
                      setCreateErrors({})
                    }}
                  >
                    Cancel
                  </Button>
                </div>
              </form>
            )
          )}
        </div>
      ) : (
        <>
          {saveSuccess && (
            <div className="proposal-save-success">Proposal updated successfully.</div>
          )}
          {submitSuccess && (
            <div className="proposal-save-success">Proposal submitted successfully.</div>
          )}

          <div className="proposal-header-card">
            <div className="proposal-header-top">
              <div>
                <h2>{proposal.title}</h2>
                {proposal.submittedAt && (
                  <p className="proposal-meta">
                    Submitted {new Date(proposal.submittedAt).toLocaleDateString(undefined, {
                      year: 'numeric',
                      month: 'short',
                      day: 'numeric',
                    })}
                  </p>
                )}
              </div>
              <div className="proposal-header-actions">
                <Button variant="secondary" onClick={openAiModal}>
                  <Sparkles size={15} style={{ marginRight: 6 }} />
                  AI Proposal Assistant
                </Button>
                <Button variant="secondary" onClick={openSprintBreakdown} loading={loadingSprintBreakdown}>
                  <Layers size={15} style={{ marginRight: 6 }} />
                  Sprint &amp; Stories AI
                </Button>
                <Badge tone={statusTone[proposal.status] || 'neutral'}>{proposal.status}</Badge>
              </div>
            </div>
          </div>

          {!editing ? (
            <>
              <div className="proposal-form-card">
                <div className="proposal-field">
                  <span className="proposal-field-label">Project Description</span>
                  <p className="proposal-field-value">{proposal.description}</p>
                </div>
                <div className="proposal-field">
                  <span className="proposal-field-label">Objectives</span>
                  <p className="proposal-field-value">
                    {proposal.objectives || 'Not provided.'}
                  </p>
                </div>
                <div className="proposal-field">
                  <span className="proposal-field-label">Technology Stack</span>
                  <p className="proposal-field-value">
                    {proposal.technologyStack || 'Not provided.'}
                  </p>
                </div>

                {submitError && <div className="proposal-save-error">{submitError}</div>}

                {canManage && (
                  <div className="proposal-form-actions">
                    <Button onClick={startEditing}>Edit</Button>
                    <Button variant="secondary" onClick={handleSubmitProposal} loading={submitting}>
                      {submitting ? 'Submitting...' : 'Submit Proposal'}
                    </Button>
                  </div>
                )}

                {!isDraft && !hasReview && (
                  <p className="proposal-readonly-note">
                    This proposal is {proposal.status.toLowerCase()} and can no longer be edited.
                  </p>
                )}

                {isDraft && !isLeader && (
                  <p className="proposal-readonly-note">
                    Only the team leader can edit or submit this proposal.
                  </p>
                )}
              </div>

              {isAwaitingReview && (
                <div className="proposal-review-card">
                  <h3>Faculty Review</h3>
                  <p className="proposal-review-pending">Awaiting Faculty Review</p>
                </div>
              )}

              {hasReview && (
                <div className="proposal-review-card">
                  <h3>Faculty Review</h3>

                  <div className="proposal-field">
                    <span className="proposal-field-label">Decision</span>
                    <p
                      className={`proposal-review-decision proposal-review-decision-${proposal.reviewDecision.toLowerCase()}`}
                    >
                      {proposal.reviewDecision}
                    </p>
                  </div>

                  <div className="proposal-field">
                    <span className="proposal-field-label">Comments</span>
                    <p className="proposal-field-value">
                      {proposal.reviewComments || 'No comments provided.'}
                    </p>
                  </div>

                  <div className="proposal-field">
                    <span className="proposal-field-label">Reviewed By</span>
                    <p className="proposal-field-value">
                      {proposal.reviewedByName || 'Unknown'}
                    </p>
                  </div>

                  <div className="proposal-field">
                    <span className="proposal-field-label">Reviewed At</span>
                    <p className="proposal-field-value">
                      {formatDateTime(proposal.reviewedAt)}
                    </p>
                  </div>
                </div>
              )}
            </>
          ) : (
            <form className="proposal-form-card" onSubmit={handleEditSubmit} noValidate>
              <div className="ai-proposal-banner">
                <div className="ai-proposal-banner-info">
                  <Sparkles size={16} className="ai-sparkle-icon" />
                  <div>
                    <strong>Enhance proposal with AI</strong>
                    <p>Re-generate with complete literature review, gaps, and submission dates.</p>
                  </div>
                </div>
                <Button type="button" variant="secondary" onClick={openAiModal}>
                  ✨ AI Drafter
                </Button>
              </div>

              <Input
                id="editTitle"
                name="title"
                label="Project Title"
                value={editFormData.title}
                onChange={handleEditChange}
                error={editErrors.title}
              />
              <Textarea
                id="editDescription"
                name="description"
                label="Project Description"
                value={editFormData.description}
                onChange={handleEditChange}
                error={editErrors.description}
              />
              <Textarea
                id="editObjectives"
                name="objectives"
                label="Objectives"
                value={editFormData.objectives}
                onChange={handleEditChange}
              />
              <Input
                id="editTechnologyStack"
                name="technologyStack"
                label="Technology Stack"
                value={editFormData.technologyStack}
                onChange={handleEditChange}
              />

              {saveError && <div className="proposal-save-error">{saveError}</div>}

              <div className="proposal-form-actions">
                <Button type="submit" loading={saving}>
                  {saving ? 'Saving...' : 'Save Changes'}
                </Button>
                <Button type="button" variant="secondary" onClick={cancelEditing}>
                  Cancel
                </Button>
              </div>
            </form>
          )}
        </>
      )}

      {aiModalOpen && (
        <div className="ai-modal-overlay" onClick={closeAiModal}>
          <div className="ai-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="ai-modal-header">
              <div className="ai-modal-header-title">
                <Sparkles size={20} className="ai-sparkle-icon" />
                <h3>AI Academic Proposal Drafter</h3>
              </div>
              <button
                type="button"
                className="ai-modal-close-btn"
                onClick={closeAiModal}
                aria-label="Close"
              >
                <X size={18} />
              </button>
            </div>

            <div className="ai-modal-body">
              <div className="ai-modal-inputs">
                <p className="ai-modal-subtitle">
                  Describe your project idea and technologies. AI will generate an academic-standard proposal with Abstract, Measurable Objectives, Literature Review, Gaps, Future Scope, and a timelined module breakdown with start &amp; end dates.
                </p>

                <div className="ai-form-group">
                  <label htmlFor="aiIdea" className="ai-label">
                    Project Idea / Core Problem Statement <span className="ai-req">*</span>
                  </label>
                  <textarea
                    id="aiIdea"
                    className="ai-textarea"
                    rows={3}
                    placeholder="e.g. Precision agriculture IoT system for real-time soil moisture monitoring, crop disease vision diagnostics, and yield prediction..."
                    value={aiIdea}
                    onChange={(e) => setAiIdea(e.target.value)}
                  />
                </div>

                <div className="ai-form-row">
                  <div className="ai-form-group" style={{ flex: 2 }}>
                    <label htmlFor="aiTech" className="ai-label">
                      Technology Stack
                    </label>
                    <input
                      id="aiTech"
                      className="ai-input"
                      placeholder="e.g. React, Spring Boot, PostgreSQL, Python, YOLOv8"
                      value={aiTechStack}
                      onChange={(e) => setAiTechStack(e.target.value)}
                    />
                  </div>
                  <div className="ai-form-group" style={{ flex: 1 }}>
                    <label htmlFor="aiWeeks" className="ai-label">
                      Timeline Duration
                    </label>
                    <select
                      id="aiWeeks"
                      className="ai-select"
                      value={aiWeeks}
                      onChange={(e) => setAiWeeks(Number(e.target.value))}
                    >
                      <option value={8}>8 Weeks</option>
                      <option value={12}>12 Weeks</option>
                      <option value={16}>16 Weeks</option>
                      <option value={20}>20 Weeks</option>
                    </select>
                  </div>
                </div>

                {aiError && <div className="proposal-save-error">{aiError}</div>}

                <div className="ai-generate-actions">
                  <Button onClick={handleGenerateDraft} loading={aiGenerating}>
                    <Sparkles size={15} style={{ marginRight: 6 }} />
                    {aiGenerating ? 'Generating Academic Proposal...' : 'Generate Academic Proposal'}
                  </Button>
                </div>
              </div>

              {aiDraft && (
                <div className="ai-draft-preview-container">
                  <div className="ai-preview-header">
                    <div>
                      <span className="ai-preview-badge">✨ Generated Academic Proposal</span>
                      <h4 className="ai-draft-project-title">{aiDraft.title}</h4>
                    </div>
                    <Button variant="secondary" onClick={handleCopyMarkdown}>
                      {copied ? <Check size={14} /> : <Copy size={14} />}
                      <span style={{ marginLeft: 6 }}>{copied ? 'Copied Markdown!' : 'Copy Markdown'}</span>
                    </Button>
                  </div>

                  <div className="ai-section-box">
                    <h5>1. Abstract</h5>
                    <p>{aiDraft.abstractText}</p>
                  </div>

                  <div className="ai-section-box">
                    <h5>2. Measurable Objectives</h5>
                    <p style={{ whiteSpace: 'pre-wrap' }}>{aiDraft.formattedObjectives || aiDraft.objectives}</p>
                  </div>

                  <div className="ai-section-box">
                    <h5>3. Literature Review</h5>
                    <p>{aiDraft.literatureReview}</p>
                  </div>

                  <div className="ai-section-box">
                    <h5>4. Existing System Gaps &amp; Problem Statement</h5>
                    <p style={{ whiteSpace: 'pre-wrap' }}>{aiDraft.researchGaps}</p>
                  </div>

                  <div className="ai-section-box">
                    <h5>5. Technologies to be Used</h5>
                    <p style={{ whiteSpace: 'pre-wrap' }}>{aiDraft.technologyStack}</p>
                  </div>

                  <div className="ai-section-box">
                    <h5>6. Future Scope &amp; Scalability</h5>
                    <p style={{ whiteSpace: 'pre-wrap' }}>{aiDraft.futureScope}</p>
                  </div>

                  <div className="ai-section-box">
                    <h5>7. Timelined Module Breakdown &amp; Submission Dates</h5>
                    <div className="ai-modules-table-wrapper">
                      <table className="ai-modules-table">
                        <thead>
                          <tr>
                            <th>Module</th>
                            <th>Start Date</th>
                            <th>End Date</th>
                            <th>Key Deliverables</th>
                          </tr>
                        </thead>
                        <tbody>
                          {aiDraft.modules?.map((m, idx) => (
                            <tr key={idx}>
                              <td><strong>{m.moduleNumber}: {m.moduleName}</strong></td>
                              <td>{m.startDate}</td>
                              <td>{m.endDate}</td>
                              <td>{m.deliverables}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="ai-modal-footer">
              <div className="ai-modal-footer-left">
                {aiDraft && (isLeader && (!proposal || isDraft)) && (
                  <Button onClick={handleApplyDraft}>
                    Apply to Proposal Form
                  </Button>
                )}
              </div>
              <Button variant="secondary" onClick={closeAiModal}>
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {sprintBreakdownOpen && (
        <div className="ai-modal-overlay" onClick={() => setSprintBreakdownOpen(false)}>
          <div className="ai-modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 880 }}>
            <div className="ai-modal-header">
              <div className="ai-modal-header-title">
                <Layers size={20} className="ai-sparkle-icon" />
                <h3>AI Sprint &amp; Story Point Breakdown</h3>
              </div>
              <button type="button" className="ai-modal-close-btn" onClick={() => setSprintBreakdownOpen(false)}>
                <X size={18} />
              </button>
            </div>

            <div className="ai-modal-body">
              {loadingSprintBreakdown ? (
                <div style={{ padding: 40, textAlign: 'center' }}>
                  <p className="proposal-loading">Generating Jira-style user stories and Fibonacci story points...</p>
                </div>
              ) : sprintBreakdownError ? (
                <div className="proposal-save-error">{sprintBreakdownError}</div>
              ) : sprintBreakdownData ? (
                <div className="ai-draft-preview-container">
                  <div className="ai-preview-header">
                    <div>
                      <span className="ai-preview-badge">✨ Agile Backlog Generator</span>
                      <h4 className="ai-draft-project-title">{sprintBreakdownData.projectTitle}</h4>
                    </div>
                    <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                      <span className="rubric-awarded-pill">{sprintBreakdownData.totalStoryPoints} Total Story Points</span>
                      <Button variant="secondary" onClick={handleCopyStories}>
                        {copiedStories ? <Check size={14} /> : <Copy size={14} />}
                        <span style={{ marginLeft: 6 }}>{copiedStories ? 'Copied Backlog!' : 'Copy Backlog'}</span>
                      </Button>
                    </div>
                  </div>

                  <div className="sprint-backlog-container">
                    {sprintBreakdownData.sprints?.map((s, idx) => (
                      <div key={idx} className="sprint-column-card">
                        <div className="sprint-column-header">
                          <div>
                            <strong>{s.sprintName}</strong>
                            <p style={{ margin: '2px 0 0 0', fontSize: '12px', color: 'var(--text-secondary)' }}>
                              <em>Goal:</em> {s.sprintGoal}
                            </p>
                          </div>
                          <span className="rubric-awarded-pill">{s.plannedPoints} pts</span>
                        </div>

                        <div className="user-stories-list">
                          {s.userStories?.map((u, sIdx) => (
                            <div key={sIdx} className="user-story-card">
                              <div className="user-story-top">
                                <span className="user-story-id">{u.storyId}</span>
                                <strong>{u.title}</strong>
                                <span className="story-point-badge">{u.storyPoints} pts</span>
                              </div>
                              <p className="user-story-desc">{u.userStoryText}</p>
                              <div className="acceptance-criteria-box">
                                <span className="ac-title">Acceptance Criteria:</span>
                                <ul>
                                  {u.acceptanceCriteria?.map((ac, aIdx) => (
                                    <li key={aIdx}>{ac}</li>
                                  ))}
                                </ul>
                              </div>
                              <div className="story-role-tag">
                                <em>Assignee:</em> <span>{u.recommendedRole}</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>

            <div className="ai-modal-footer">
              <Button variant="secondary" onClick={() => setSprintBreakdownOpen(false)}>Close</Button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}

export default StudentProjectProposal