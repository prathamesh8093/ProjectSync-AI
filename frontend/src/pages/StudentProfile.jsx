import { useEffect, useState } from 'react'
import { LayoutDashboard, UserRound, Users, FileText, ListChecks } from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { getStudentProfile, updateStudentProfile } from '../services/profileService.js'
import Input from '../components/ui/Input.jsx'
import Textarea from '../components/ui/Textarea.jsx'
import Button from '../components/ui/Button.jsx'
import Badge from '../components/ui/Badge.jsx'
import '../styles/profile.css'

const studentNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/student/dashboard' },
  { label: 'My Profile', icon: UserRound, path: '/student/profile' },
  { label: 'My Team', icon: Users, path: '/student/team' },
  { label: 'Project Proposal', icon: FileText, path: '/student/proposal' },
  { label: 'Tasks / Projects', icon: ListChecks, path: '/student/projects' },
]

const emptyForm = { studentId: '', department: '', year: '', bio: '' }

function StudentProfile() {
  const { user } = useAuth()
  const [loading, setLoading] = useState(true)
  const [profileExists, setProfileExists] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [formData, setFormData] = useState(emptyForm)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [saveSuccess, setSaveSuccess] = useState(false)

  useEffect(() => {
    let isMounted = true

    const loadProfile = async () => {
      try {
        const response = await getStudentProfile()
        if (!isMounted) return

        setProfileExists(true)
        setFormData({
          studentId: response.data.studentId || '',
          department: response.data.department || '',
          year: response.data.year ?? '',
          bio: response.data.bio || '',
        })
      } catch (err) {
        if (!isMounted) return

        if (err.response && err.response.status === 404) {
          setProfileExists(false)
        } else {
          setLoadError(
            'Unable to load your profile right now. Please try again later.'
          )
        }
      } finally {
        if (isMounted) setLoading(false)
      }
    }

    loadProfile()

    return () => {
      isMounted = false
    }
  }, [])

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  const validate = () => {
    const newErrors = {}

    if (!formData.studentId.trim()) {
      newErrors.studentId = 'Student ID is required.'
    }

    if (formData.year !== '') {
      const yearNum = Number(formData.year)

      if (!Number.isInteger(yearNum) || yearNum < 1 || yearNum > 6) {
        newErrors.year = 'Year must be a whole number between 1 and 6.'
      }
    }

    return newErrors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaveError('')
    setSaveSuccess(false)

    const newErrors = validate()
    setErrors(newErrors)

    if (Object.keys(newErrors).length > 0) return

    setSaving(true)

    try {
      const payload = {
        studentId: formData.studentId.trim(),
        department: formData.department.trim() || null,
        year: formData.year === '' ? null : Number(formData.year),
        bio: formData.bio.trim() || null,
      }

      const response = await updateStudentProfile(payload)

      setFormData({
        studentId: response.data.studentId || '',
        department: response.data.department || '',
        year: response.data.year ?? '',
        bio: response.data.bio || '',
      })

      setProfileExists(true)
      setSaveSuccess(true)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setErrors({
          studentId: 'This Student ID is already in use.',
        })
      } else if (err.response && err.response.status === 400) {
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

  const initials = user.fullName
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

  return (
    <DashboardLayout navItems={studentNavItems}>
      <h1 className="content-title">My Profile</h1>
      <p className="content-subtitle">
        Manage your academic profile information.
      </p>

      {loading ? (
        <div className="profile-card">
          <p className="profile-loading">
            Loading your profile...
          </p>
        </div>
      ) : loadError ? (
        <div className="profile-card">
          <p className="profile-load-error">
            {loadError}
          </p>
        </div>
      ) : (
        <>
          <div className="profile-header-card">
            <div className="profile-avatar">{initials}</div>

            <div className="profile-header-info">
              <h2>{user.fullName}</h2>

              <div className="profile-header-meta">
                <Badge tone="primary">{user.role}</Badge>

                {formData.department && (
                  <span>{formData.department}</span>
                )}

                {formData.year !== '' && (
                  <span>Year {formData.year}</span>
                )}
              </div>
            </div>
          </div>

          {!profileExists && (
            <div className="profile-empty-note">
              You haven't set up your academic profile yet.
              Fill in the details below to get started.
            </div>
          )}

          <form
            className="profile-form-card"
            onSubmit={handleSubmit}
            noValidate
          >
            <div className="profile-section">
              <h3>Academic information</h3>

              <div className="profile-form-grid">
                <Input
                  id="studentId"
                  name="studentId"
                  label="Student ID"
                  placeholder="e.g. STU2026001"
                  value={formData.studentId}
                  onChange={handleChange}
                  error={errors.studentId}
                />

                <Input
                  id="department"
                  name="department"
                  label="Department"
                  placeholder="e.g. Computer Science"
                  value={formData.department}
                  onChange={handleChange}
                />

                <Input
                  id="year"
                  name="year"
                  label="Year"
                  type="number"
                  min="1"
                  max="6"
                  placeholder="1-6"
                  value={formData.year}
                  onChange={handleChange}
                  error={errors.year}
                />
              </div>
            </div>

            <div className="profile-section">
              <h3>About</h3>

              <Textarea
                id="bio"
                name="bio"
                label="Bio"
                placeholder="Tell your team and faculty a bit about yourself..."
                value={formData.bio}
                onChange={handleChange}
              />
            </div>

            {saveError && (
              <div className="profile-save-error">
                {saveError}
              </div>
            )}

            {saveSuccess && (
              <div className="profile-save-success">
                Profile saved successfully.
              </div>
            )}

            <div className="profile-form-actions">
              <Button type="submit" loading={saving}>
                {saving ? 'Saving...' : 'Save changes'}
              </Button>
            </div>
          </form>
        </>
      )}
    </DashboardLayout>
  )
}

export default StudentProfile