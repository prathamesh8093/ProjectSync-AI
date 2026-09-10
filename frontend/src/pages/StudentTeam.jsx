import { useCallback, useEffect, useState } from 'react'
import { LayoutDashboard, UserRound, Users, FileText, ListChecks } from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import {
  createTeam,
  getMyTeam,
  addTeamMember,
  getTeamMembers,
} from '../services/teamService.js'
import Input from '../components/ui/Input.jsx'
import Button from '../components/ui/Button.jsx'
import Badge from '../components/ui/Badge.jsx'
import '../styles/team.css'

const studentNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/student/dashboard' },
  { label: 'My Profile', icon: UserRound, path: '/student/profile' },
  { label: 'My Team', icon: Users, path: '/student/team' },
  { label: 'Project Proposal', icon: FileText, path: '/student/proposal' },
  { label: 'Tasks / Projects', icon: ListChecks, path: null },
]

function StudentTeam() {
  const { user } = useAuth()

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [team, setTeam] = useState(null)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [teamNameInput, setTeamNameInput] = useState('')
  const [createError, setCreateError] = useState('')
  const [creating, setCreating] = useState(false)
  const [createSuccess, setCreateSuccess] = useState(false)

  const [members, setMembers] = useState([])
  const [membersLoading, setMembersLoading] = useState(false)
  const [membersError, setMembersError] = useState('')

  const [addMemberId, setAddMemberId] = useState('')
  const [addMemberError, setAddMemberError] = useState('')
  const [addingMember, setAddingMember] = useState(false)
  const [addMemberSuccess, setAddMemberSuccess] = useState(false)

  const loadMembers = useCallback(async (teamId) => {
    setMembersLoading(true)
    setMembersError('')
    try {
      const response = await getTeamMembers(teamId)
      setMembers(response.data)
    } catch {
      setMembersError('Unable to load team members right now.')
    } finally {
      setMembersLoading(false)
    }
  }, [])

  useEffect(() => {
    let isMounted = true

    const loadTeam = async () => {
      try {
        const response = await getMyTeam()
        if (!isMounted) return
        setTeam(response.data)
        loadMembers(response.data.id)
      } catch (err) {
        if (!isMounted) return
        if (err.response && err.response.status === 404) {
          setTeam(null)
        } else {
          setLoadError('Unable to load your team right now. Please try again later.')
        }
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadTeam()
    return () => {
      isMounted = false
    }
  }, [loadMembers])

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    setCreateError('')
    setCreateSuccess(false)

    const trimmedName = teamNameInput.trim()
    if (!trimmedName) {
      setCreateError('Team name is required.')
      return
    }
    if (trimmedName.length > 100) {
      setCreateError('Team name must be 100 characters or fewer.')
      return
    }

    setCreating(true)
    try {
      const response = await createTeam(trimmedName)
      setTeam(response.data)
      setShowCreateForm(false)
      setCreateSuccess(true)
      loadMembers(response.data.id)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setCreateError('You already belong to a team.')
      } else if (err.response && err.response.status === 400) {
        setCreateError('Please check the team name and try again.')
      } else {
        setCreateError('Something went wrong while creating the team. Please try again.')
      }
    } finally {
      setCreating(false)
    }
  }

  const handleAddMemberSubmit = async (e) => {
    e.preventDefault()
    setAddMemberError('')
    setAddMemberSuccess(false)

    const trimmedId = addMemberId.trim()
    if (!trimmedId) {
      setAddMemberError('Student User ID is required.')
      return
    }

    setAddingMember(true)
    try {
      await addTeamMember(team.id, trimmedId)
      setAddMemberId('')
      setAddMemberSuccess(true)
      loadMembers(team.id)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setAddMemberError('This student already belongs to a team.')
      } else if (err.response && err.response.status === 404) {
        setAddMemberError('No student was found with that ID.')
      } else if (err.response && err.response.status === 403) {
        setAddMemberError('Only the team leader can add members.')
      } else if (err.response && err.response.status === 400) {
        setAddMemberError('That user cannot be added to the team.')
      } else {
        setAddMemberError('Something went wrong while adding the member. Please try again.')
      }
    } finally {
      setAddingMember(false)
    }
  }

  const currentMember = members.find((m) => m.email === user.email)
  const isLeader = currentMember?.teamRole === 'LEADER'

  const formatDate = (value) => {
    if (!value) return ''
    return new Date(value).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  return (
    <DashboardLayout navItems={studentNavItems}>
      <h1 className="content-title">Team</h1>
      <p className="content-subtitle">Build your project team.</p>

      {loading ? (
        <div className="team-card">
          <p className="team-loading">Loading your team...</p>
        </div>
      ) : loadError ? (
        <div className="team-card">
          <p className="team-load-error">{loadError}</p>
        </div>
      ) : !team ? (
        <div className="team-empty-card">
          <h2>You haven't joined a team yet.</h2>
          <p>Create a team to start collaborating with your classmates.</p>

          {!showCreateForm ? (
            <Button onClick={() => setShowCreateForm(true)}>Create Team</Button>
          ) : (
            <form className="team-create-form" onSubmit={handleCreateSubmit} noValidate>
              <Input
                id="teamName"
                name="teamName"
                label="Team Name"
                placeholder="e.g. ProjectSync Team"
                value={teamNameInput}
                onChange={(e) => setTeamNameInput(e.target.value)}
                error={createError}
              />
              <div className="team-create-actions">
                <Button type="submit" loading={creating}>
                  {creating ? 'Creating...' : 'Create Team'}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setShowCreateForm(false)
                    setCreateError('')
                  }}
                >
                  Cancel
                </Button>
              </div>
            </form>
          )}
        </div>
      ) : (
        <>
          {createSuccess && (
            <div className="team-save-success">Team created successfully.</div>
          )}

          <div className="team-header-card">
            <h2>{team.teamName}</h2>
            <div className="team-info-grid">
              <div className="team-info-item">
                <span className="team-info-label">Created</span>
                <span className="team-info-value">{formatDate(team.createdAt)}</span>
              </div>
              <div className="team-info-item">
                <span className="team-info-label">Members</span>
                <span className="team-info-value">{members.length}</span>
              </div>
              <div className="team-info-item">
                <span className="team-info-label">Your role</span>
                <Badge tone={isLeader ? 'primary' : 'neutral'}>
                  {currentMember?.teamRole || '—'}
                </Badge>
              </div>
            </div>
          </div>

          <div className="team-members-card">
            <h3>Members</h3>

            {membersLoading ? (
              <p className="team-loading">Loading members...</p>
            ) : membersError ? (
              <p className="team-load-error">{membersError}</p>
            ) : (
              <div className="team-members-list">
                {members.map((member) => {
                  const memberInitials = member.fullName
                    .split(' ')
                    .map((part) => part[0])
                    .slice(0, 2)
                    .join('')
                    .toUpperCase()

                  return (
                    <div key={member.id} className="team-member-row">
                      <div className="team-member-avatar">{memberInitials}</div>
                      <div className="team-member-info">
                        <span className="team-member-name">{member.fullName}</span>
                        <span className="team-member-email">{member.email}</span>
                      </div>
                      <div className="team-member-meta">
                        <Badge tone={member.teamRole === 'LEADER' ? 'primary' : 'neutral'}>
                          {member.teamRole}
                        </Badge>
                        <span className="team-member-joined">
                          Joined {formatDate(member.joinedAt)}
                        </span>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>

          <div className="team-add-member-card">
            <h3>Add Member</h3>

            {isLeader ? (
              <form className="team-add-member-form" onSubmit={handleAddMemberSubmit} noValidate>
                <Input
                  id="studentUserId"
                  name="studentUserId"
                  label="Student User ID"
                  placeholder="Paste the student's user ID"
                  value={addMemberId}
                  onChange={(e) => setAddMemberId(e.target.value)}
                  error={addMemberError}
                />
                <Button type="submit" loading={addingMember}>
                  {addingMember ? 'Adding...' : 'Add Member'}
                </Button>
                {addMemberSuccess && (
                  <div className="team-save-success">Member added successfully.</div>
                )}
              </form>
            ) : (
              <p className="team-leader-note">Only the team leader can add members.</p>
            )}
          </div>
        </>
      )}
    </DashboardLayout>
  )
}

export default StudentTeam