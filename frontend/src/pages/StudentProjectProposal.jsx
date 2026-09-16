import { useEffect, useState } from 'react'
import { LayoutDashboard, UserRound, Users, FileText, ListChecks } from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import {
  createProposal,
  getMyProposal,
  updateProposal,
  submitProposal,
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
  { label: 'Tasks / Projects', icon: ListChecks, path: '/student/projects' },
]

const emptyForm = {
  title: '',
  description: '',
  objectives: '',
  technologyStack: '',
}

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

  useEffect(() => {
    let isMounted = true

    const loadLeaderStatus = async () => {
      try {
        const teamResponse = await getMyTeam()
        const membersResponse = await getTeamMembers(
          teamResponse.data.id
        )

        if (!isMounted) return

        const currentMember = membersResponse.data.find(
          (m) => m.email === user.email
        )

        setIsLeader(
          currentMember?.teamRole === 'LEADER'
        )
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

        if (
          err.response &&
          err.response.status === 404
        ) {
          setProposal(null)
        } else {
          setLoadError(
            'Unable to load your proposal right now. Please try again later.'
          )
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
      newErrors.title =
        'Title must be 200 characters or fewer.'
    }

    if (!data.description.trim()) {
      newErrors.description =
        'Project description is required.'
    }

    return newErrors
  }

  const buildPayload = (data) => ({
    title: data.title.trim(),
    description: data.description.trim(),
    objectives: data.objectives.trim() || null,
    technologyStack:
      data.technologyStack.trim() || null,
  })

  const handleCreateChange = (e) => {
    setCreateFormData({
      ...createFormData,
      [e.target.name]: e.target.value,
    })
  }

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    setCreateSuccess(false)

    const newErrors = validate(createFormData)
    setCreateErrors(newErrors)

    if (Object.keys(newErrors).length > 0) return

    setCreating(true)

    try {
      const response = await createProposal(
        buildPayload(createFormData)
      )

      setProposal(response.data)
      setShowCreateForm(false)
      setCreateFormData(emptyForm)
      setCreateSuccess(true)
    } catch (err) {
      if (
        err.response &&
        err.response.status === 409
      ) {
        setCreateErrors({
          title:
            'Your team already has a proposal.',
        })
      } else if (
        err.response &&
        err.response.status === 403
      ) {
        setCreateErrors({
          title:
            'Only the team leader can create a proposal.',
        })
      } else if (
        err.response &&
        err.response.status === 404
      ) {
        setCreateErrors({
          title:
            'You need to be part of a team before creating a proposal.',
        })
      } else if (
        err.response &&
        err.response.status === 400
      ) {
        setCreateErrors({
          title:
            'Please check the form and try again.',
        })
      } else {
        setCreateErrors({
          title:
            'Something went wrong while creating the proposal. Please try again.',
        })
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
    setEditFormData({
      ...editFormData,
      [e.target.name]: e.target.value,
    })
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
      const response = await updateProposal(
        proposal.id,
        buildPayload(editFormData)
      )

      setProposal(response.data)
      setEditing(false)
      setSaveSuccess(true)
    } catch (err) {
      if (
        err.response &&
        err.response.status === 409
      ) {
        setSaveError(
          'This proposal can no longer be edited.'
        )
      } else if (
        err.response &&
        err.response.status === 403
      ) {
        setSaveError(
          'Only the team leader can edit this proposal.'
        )
      } else if (
        err.response &&
        err.response.status === 400
      ) {
        setSaveError(
          'Please check the highlighted fields and try again.'
        )
      } else {
        setSaveError(
          'Something went wrong while saving. Please try again.'
        )
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
      const response = await submitProposal(
        proposal.id
      )

      setProposal(response.data)
      setSubmitSuccess(true)
    } catch (err) {
      if (
        err.response &&
        err.response.status === 409
      ) {
        setSubmitError(
          'This proposal has already been submitted or reviewed.'
        )
      } else if (
        err.response &&
        err.response.status === 403
      ) {
        setSubmitError(
          'Only the team leader can submit this proposal.'
        )
      } else {
        setSubmitError(
          'Something went wrong while submitting. Please try again.'
        )
      }
    } finally {
      setSubmitting(false)
    }
  }

  const isDraft = proposal?.status === 'DRAFT'
  const canManage = isDraft && isLeader
  const hasReview = Boolean(
    proposal?.reviewDecision
  )
  const isAwaitingReview =
    proposal?.status === 'SUBMITTED' &&
    !hasReview

  return (
    <DashboardLayout navItems={studentNavItems}>
      <h1 className="content-title">
        Project Proposal
      </h1>

      <p className="content-subtitle">
        Manage your team's project proposal.
      </p>

      {loading ? (
        <div className="proposal-card">
          <p className="proposal-loading">
            Loading your proposal...
          </p>
        </div>
      ) : loadError ? (
        <div className="proposal-card">
          <p className="proposal-load-error">
            {loadError}
          </p>
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
            <div className="proposal-save-success">
              Proposal created successfully.
            </div>
          )}

          {isLeader &&
            (!showCreateForm ? (
              <Button
                onClick={() =>
                  setShowCreateForm(true)
                }
              >
                Create Proposal
              </Button>
            ) : (
              <form
                className="proposal-form"
                onSubmit={handleCreateSubmit}
                noValidate
              >
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
                  value={
                    createFormData.description
                  }
                  onChange={handleCreateChange}
                  error={
                    createErrors.description
                  }
                />

                <Textarea
                  id="objectives"
                  name="objectives"
                  label="Objectives"
                  placeholder="What are the goals of this project?"
                  value={
                    createFormData.objectives
                  }
                  onChange={handleCreateChange}
                />

                <Input
                  id="technologyStack"
                  name="technologyStack"
                  label="Technology Stack"
                  placeholder="e.g. React, Spring Boot, PostgreSQL"
                  value={
                    createFormData.technologyStack
                  }
                  onChange={handleCreateChange}
                />

                <div className="proposal-form-actions">
                  <Button
                    type="submit"
                    loading={creating}
                  >
                    {creating
                      ? 'Creating...'
                      : 'Create Proposal'}
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
            ))}
        </div>
      ) : (
        <>
          {saveSuccess && (
            <div className="proposal-save-success">
              Proposal updated successfully.
            </div>
          )}

          {submitSuccess && (
            <div className="proposal-save-success">
              Proposal submitted successfully.
            </div>
          )}

          <div className="proposal-header-card">
            <div className="proposal-header-top">
              <h2>{proposal.title}</h2>

              <Badge
                tone={
                  statusTone[proposal.status] ||
                  'neutral'
                }
              >
                {proposal.status}
              </Badge>
            </div>

            {proposal.submittedAt && (
              <p className="proposal-meta">
                Submitted{' '}
                {new Date(
                  proposal.submittedAt
                ).toLocaleDateString(undefined, {
                  year: 'numeric',
                  month: 'short',
                  day: 'numeric',
                })}
              </p>
            )}
          </div>

          {!editing ? (
            <>
              <div className="proposal-form-card">
                <div className="proposal-field">
                  <span className="proposal-field-label">
                    Project Description
                  </span>

                  <p className="proposal-field-value">
                    {proposal.description}
                  </p>
                </div>

                <div className="proposal-field">
                  <span className="proposal-field-label">
                    Objectives
                  </span>

                  <p className="proposal-field-value">
                    {proposal.objectives ||
                      'Not provided.'}
                  </p>
                </div>

                <div className="proposal-field">
                  <span className="proposal-field-label">
                    Technology Stack
                  </span>

                  <p className="proposal-field-value">
                    {proposal.technologyStack ||
                      'Not provided.'}
                  </p>
                </div>

                {submitError && (
                  <div className="proposal-save-error">
                    {submitError}
                  </div>
                )}

                {canManage && (
                  <div className="proposal-form-actions">
                    <Button
                      onClick={startEditing}
                    >
                      Edit
                    </Button>

                    <Button
                      variant="secondary"
                      onClick={
                        handleSubmitProposal
                      }
                      loading={submitting}
                    >
                      {submitting
                        ? 'Submitting...'
                        : 'Submit Proposal'}
                    </Button>
                  </div>
                )}

                {!isDraft && !hasReview && (
                  <p className="proposal-readonly-note">
                    This proposal is{' '}
                    {proposal.status.toLowerCase()}{' '}
                    and can no longer be edited.
                  </p>
                )}

                {isDraft && !isLeader && (
                  <p className="proposal-readonly-note">
                    Only the team leader can edit or
                    submit this proposal.
                  </p>
                )}
              </div>

              {isAwaitingReview && (
                <div className="proposal-review-card">
                  <h3>Faculty Review</h3>

                  <p className="proposal-review-pending">
                    Awaiting Faculty Review
                  </p>
                </div>
              )}

              {hasReview && (
                <div className="proposal-review-card">
                  <h3>Faculty Review</h3>

                  <div className="proposal-field">
                    <span className="proposal-field-label">
                      Decision
                    </span>

                    <p
                      className={`proposal-review-decision proposal-review-decision-${proposal.reviewDecision.toLowerCase()}`}
                    >
                      {proposal.reviewDecision}
                    </p>
                  </div>

                  <div className="proposal-field">
                    <span className="proposal-field-label">
                      Comments
                    </span>

                    <p className="proposal-field-value">
                      {proposal.reviewComments ||
                        'No comments provided.'}
                    </p>
                  </div>

                  <div className="proposal-field">
                    <span className="proposal-field-label">
                      Reviewed By
                    </span>

                    <p className="proposal-field-value">
                      {proposal.reviewedByName ||
                        'Unknown'}
                    </p>
                  </div>

                  <div className="proposal-field">
                    <span className="proposal-field-label">
                      Reviewed At
                    </span>

                    <p className="proposal-field-value">
                      {formatDateTime(
                        proposal.reviewedAt
                      )}
                    </p>
                  </div>
                </div>
              )}
            </>
          ) : (
            <form
              className="proposal-form-card"
              onSubmit={handleEditSubmit}
              noValidate
            >
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
                value={
                  editFormData.description
                }
                onChange={handleEditChange}
                error={
                  editErrors.description
                }
              />

              <Textarea
                id="editObjectives"
                name="objectives"
                label="Objectives"
                value={
                  editFormData.objectives
                }
                onChange={handleEditChange}
              />

              <Input
                id="editTechnologyStack"
                name="technologyStack"
                label="Technology Stack"
                value={
                  editFormData.technologyStack
                }
                onChange={handleEditChange}
              />

              {saveError && (
                <div className="proposal-save-error">
                  {saveError}
                </div>
              )}

              <div className="proposal-form-actions">
                <Button
                  type="submit"
                  loading={saving}
                >
                  {saving
                    ? 'Saving...'
                    : 'Save Changes'}
                </Button>

                <Button
                  type="button"
                  variant="secondary"
                  onClick={cancelEditing}
                >
                  Cancel
                </Button>
              </div>
            </form>
          )}
        </>
      )}
    </DashboardLayout>
  )
}

export default StudentProjectProposal