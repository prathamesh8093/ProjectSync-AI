import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  LayoutDashboard,
  UserRound,
  Users,
  FileText,
  ClipboardCheck,
} from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import {
  getFacultyProposals,
  getFacultyProposalById,
  reviewFacultyProposal,
} from '../services/facultyProposalService.js'
import Button from '../components/ui/Button.jsx'
import Textarea from '../components/ui/Textarea.jsx'
import Badge from '../components/ui/Badge.jsx'
import '../styles/facultyProposal.css'

const facultyNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/faculty/dashboard' },
  { label: 'Faculty Profile', icon: UserRound, path: '/faculty/profile' },
  { label: 'Student Teams', icon: Users, path: null },
  { label: 'Project Proposals', icon: FileText, path: '/faculty/proposals' },
  {
    label: 'Project Monitoring',
    icon: ClipboardCheck,
    path: '/faculty/projects',
  },
]

const statusTone = {
  DRAFT: 'neutral',
  SUBMITTED: 'primary',
  APPROVED: 'success',
  REJECTED: 'neutral',
}

function formatDate(value) {
  if (!value) return '—'

  return new Date(value).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function messageForError(err) {
  if (!err.response) {
    return 'Something went wrong. Please try again.'
  }

  switch (err.response.status) {
    case 401:
      return 'Your session has expired. Please log in again.'
    case 403:
      return 'You do not have permission to access faculty proposals.'
    case 404:
      return 'Proposal not found.'
    case 409:
      return 'Only submitted proposals can be reviewed.'
    case 400:
      return 'Please check the review details and try again.'
    default:
      return 'Something went wrong. Please try again.'
  }
}

function FacultyProjectProposals() {
  const [proposals, setProposals] = useState([])
  const [listLoading, setListLoading] = useState(true)
  const [listError, setListError] = useState('')

  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')

  const [comments, setComments] = useState('')
  const [reviewing, setReviewing] = useState(false)
  const [reviewError, setReviewError] = useState('')
  const [reviewSuccess, setReviewSuccess] = useState('')

  useEffect(() => {
    let isMounted = true

    const loadProposals = async () => {
      try {
        const response = await getFacultyProposals()

        if (!isMounted) return

        setProposals(response.data)
      } catch (err) {
        if (!isMounted) return

        setListError(messageForError(err))
      } finally {
        if (isMounted) {
          setListLoading(false)
        }
      }
    }

    loadProposals()

    return () => {
      isMounted = false
    }
  }, [])

  const viewDetails = async (proposalId) => {
    setSelectedId(proposalId)
    setDetail(null)
    setDetailError('')
    setReviewError('')
    setReviewSuccess('')
    setComments('')
    setDetailLoading(true)

    try {
      const response = await getFacultyProposalById(proposalId)
      setDetail(response.data)
    } catch (err) {
      setDetailError(messageForError(err))
    } finally {
      setDetailLoading(false)
    }
  }

  const closeDetails = () => {
    setSelectedId(null)
    setDetail(null)
    setDetailError('')
    setReviewError('')
    setReviewSuccess('')
    setComments('')
  }

  const handleReview = async (decision) => {
    setReviewError('')
    setReviewSuccess('')
    setReviewing(true)

    try {
      const response = await reviewFacultyProposal(detail.id, {
        decision,
        comments: comments.trim() || null,
      })

      setDetail(response.data)

      setReviewSuccess(
        `Proposal ${
          decision === 'APPROVED' ? 'approved' : 'rejected'
        } successfully.`
      )

      setProposals((prev) =>
        prev.filter((proposal) => proposal.id !== response.data.id)
      )
    } catch (err) {
      setReviewError(messageForError(err))
    } finally {
      setReviewing(false)
    }
  }

  return (
    <DashboardLayout navItems={facultyNavItems}>
      <h1 className="content-title">Faculty Proposal Review</h1>

      <p className="content-subtitle">
        Review project proposals submitted by student teams.
      </p>

      {listLoading ? (
        <div className="faculty-proposal-card">
          <p className="faculty-proposal-loading">
            Loading submitted proposals...
          </p>
        </div>
      ) : listError ? (
        <div className="faculty-proposal-card">
          <p className="faculty-proposal-error">{listError}</p>
        </div>
      ) : proposals.length === 0 ? (
        <div className="faculty-proposal-empty-card">
          <h2>No submitted proposals</h2>
          <p>
            Submitted project proposals will appear here for faculty review.
          </p>
        </div>
      ) : (
        <div className="faculty-proposal-list">
          {proposals.map((proposal) => (
            <div key={proposal.id} className="faculty-proposal-row">
              <div className="faculty-proposal-row-main">
                <div className="faculty-proposal-row-top">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
                    <span style={{
                      fontFamily: 'monospace',
                      fontWeight: 700,
                      fontSize: '0.82rem',
                      background: '#f1f5f9',
                      color: '#334155',
                      padding: '0.15rem 0.5rem',
                      borderRadius: '4px',
                      border: '1px solid #cbd5e1',
                    }}>
                      {proposal.projectCode || proposal.id}
                    </span>
                    <h3>{proposal.title}</h3>
                  </div>

                  <Badge tone={statusTone[proposal.status] || 'neutral'}>
                    {proposal.status}
                  </Badge>
                </div>

                <div className="faculty-proposal-row-meta">
                  <span>{proposal.teamName}</span>

                  <span>
                    Submitted by {proposal.submittedByName || 'Unknown'}
                  </span>

                  <span>{formatDate(proposal.submittedAt)}</span>

                  {proposal.technologyStack && (
                    <span>{proposal.technologyStack}</span>
                  )}
                </div>
              </div>

              <Button
                variant="secondary"
                onClick={() => viewDetails(proposal.id)}
              >
                View Details
              </Button>
            </div>
          ))}
        </div>
      )}

      {selectedId && (
        <div className="faculty-proposal-details-card">
          {detailLoading ? (
            <p className="faculty-proposal-loading">
              Loading proposal details...
            </p>
          ) : detailError ? (
            <p className="faculty-proposal-error">{detailError}</p>
          ) : detail ? (
            <>
              <div className="faculty-proposal-details-top">
                <div>
                  <span style={{
                    display: 'inline-block',
                    fontFamily: 'monospace',
                    fontWeight: 700,
                    fontSize: '0.85rem',
                    background: '#f1f5f9',
                    color: '#334155',
                    padding: '0.2rem 0.6rem',
                    borderRadius: '4px',
                    border: '1px solid #cbd5e1',
                    marginBottom: '0.4rem',
                  }}>
                    ID: {detail.projectCode || detail.id}
                  </span>
                  <h2>{detail.title}</h2>
                </div>

                <Badge tone={statusTone[detail.status] || 'neutral'}>
                  {detail.status}
                </Badge>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Project Code / ID
                </span>

                <p className="faculty-proposal-field-value" style={{ fontFamily: 'monospace', fontWeight: 700, color: '#0369a1' }}>
                  {detail.projectCode || detail.id}
                </p>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Team
                </span>

                <p className="faculty-proposal-field-value">
                  {detail.teamName}
                </p>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Description
                </span>

                <p className="faculty-proposal-field-value">
                  {detail.description}
                </p>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Objectives
                </span>

                <p className="faculty-proposal-field-value">
                  {detail.objectives || 'Not provided.'}
                </p>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Technology Stack
                </span>

                <p className="faculty-proposal-field-value">
                  {detail.technologyStack || 'Not provided.'}
                </p>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Submitted By
                </span>

                <p className="faculty-proposal-field-value">
                  {detail.submittedByName || 'Unknown'}
                </p>
              </div>

              <div className="faculty-proposal-field">
                <span className="faculty-proposal-field-label">
                  Submitted At
                </span>

                <p className="faculty-proposal-field-value">
                  {formatDate(detail.submittedAt)}
                </p>
              </div>

              {detail.reviewDecision && (
                <div className="faculty-proposal-review-summary">
                  <h3>Review</h3>

                  <div className="faculty-proposal-field">
                    <span className="faculty-proposal-field-label">
                      Decision
                    </span>

                    <p className="faculty-proposal-field-value">
                      {detail.reviewDecision}
                    </p>
                  </div>

                  <div className="faculty-proposal-field">
                    <span className="faculty-proposal-field-label">
                      Comments
                    </span>

                    <p className="faculty-proposal-field-value">
                      {detail.reviewComments || 'No comments.'}
                    </p>
                  </div>

                  <div className="faculty-proposal-field">
                    <span className="faculty-proposal-field-label">
                      Reviewed By
                    </span>

                    <p className="faculty-proposal-field-value">
                      {detail.reviewedByName}
                    </p>
                  </div>

                  <div className="faculty-proposal-field">
                    <span className="faculty-proposal-field-label">
                      Reviewed At
                    </span>

                    <p className="faculty-proposal-field-value">
                      {formatDate(detail.reviewedAt)}
                    </p>
                  </div>
                </div>
              )}

              {reviewSuccess && (
                <div className="faculty-proposal-success">
                  {reviewSuccess}
                </div>
              )}

              {reviewError && (
                <div className="faculty-proposal-error-banner">
                  {reviewError}
                </div>
              )}

              {detail.status === 'SUBMITTED' && (
                <div className="faculty-proposal-review-form">
                  <h3>Review</h3>

                  <Textarea
                    id="reviewComments"
                    name="reviewComments"
                    label="Review Comments"
                    placeholder="Optional comments for the team..."
                    value={comments}
                    onChange={(e) => setComments(e.target.value)}
                  />

                  <div className="faculty-proposal-review-actions">
                    <Button
                      onClick={() => handleReview('APPROVED')}
                      loading={reviewing}
                    >
                      {reviewing ? 'Submitting...' : 'Approve'}
                    </Button>

                    <Button
                      variant="secondary"
                      onClick={() => handleReview('REJECTED')}
                      loading={reviewing}
                    >
                      {reviewing ? 'Submitting...' : 'Reject'}
                    </Button>
                  </div>
                </div>
              )}

              {detail.status === 'APPROVED' && (
                <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                    Proposal is approved. Track its progress in the monitoring dashboard.
                  </span>
                  <Link
                    to={`/faculty/projects?projectId=${detail.projectCode || detail.id}`}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.45rem',
                      background: '#2563eb',
                      color: '#ffffff',
                      padding: '0.45rem 0.9rem',
                      borderRadius: '6px',
                      textDecoration: 'none',
                      fontWeight: 500,
                      fontSize: '0.85rem',
                    }}
                  >
                    Monitor Project Progress →
                  </Link>
                </div>
              )}

              <div className="faculty-proposal-details-actions">
                <Button variant="secondary" onClick={closeDetails}>
                  Close
                </Button>
              </div>
            </>
          ) : null}
        </div>
      )}
    </DashboardLayout>
  )
}

export default FacultyProjectProposals