import { useEffect, useState } from 'react'
import { LayoutDashboard, UserRound, Users, FileText, ClipboardCheck } from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { getFacultyProfile, updateFacultyProfile } from '../services/profileService.js'
import Input from '../components/ui/Input.jsx'
import Button from '../components/ui/Button.jsx'
import Badge from '../components/ui/Badge.jsx'
import '../styles/profile.css'

const facultyNavItems = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/faculty/dashboard' },
  { label: 'Faculty Profile', icon: UserRound, path: '/faculty/profile' },
  { label: 'Student Teams', icon: Users, path: null },
  { label: 'Project Proposals', icon: FileText, path: '/faculty/proposals' },
  { label: 'Project Monitoring', icon: ClipboardCheck, path: '/faculty/projects' },
]

const emptyForm = { facultyId: '', department: '', designation: '' }

function FacultyProfile() {
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
        const response = await getFacultyProfile()
        if (!isMounted) return
        setProfileExists(true)
        setFormData({
          facultyId: response.data.facultyId || '',
          department: response.data.department || '',
          designation: response.data.designation || '',
        })
      } catch (err) {
        if (!isMounted) return
        if (err.response && err.response.status === 404) {
          setProfileExists(false)
        } else {
          setLoadError('Unable to load your profile right now. Please try again later.')
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
    setFormData({ ...formData, [e.target.name]: e.target.value })
  }

  const validate = () => {
    const newErrors = {}

    if (!formData.facultyId.trim()) {
      newErrors.facultyId = 'Faculty ID is required.'
    } else if (formData.facultyId.trim().length > 50) {
      newErrors.facultyId = 'Faculty ID must be 50 characters or fewer.'
    }

    if (formData.department.trim().length > 100) {
      newErrors.department = 'Department must be 100 characters or fewer.'
    }

    if (formData.designation.trim().length > 100) {
      newErrors.designation = 'Designation must be 100 characters or fewer.'
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
        facultyId: formData.facultyId.trim(),
        department: formData.department.trim() || null,
        designation: formData.designation.trim() || null,
      }
      const response = await updateFacultyProfile(payload)
      setFormData({
        facultyId: response.data.facultyId || '',
        department: response.data.department || '',
        designation: response.data.designation || '',
      })
      setProfileExists(true)
      setSaveSuccess(true)
    } catch (err) {
      if (err.response && err.response.status === 409) {
        setErrors({ facultyId: 'This Faculty ID is already in use.' })
      } else if (err.response && err.response.status === 400) {
        setSaveError('Please check the highlighted fields and try again.')
      } else {
        setSaveError('Something went wrong while saving. Please try again.')
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
    <DashboardLayout navItems={facultyNavItems}>
      <h1 className="content-title">My Profile</h1>
      <p className="content-subtitle">Manage your faculty profile information.</p>

      {loading ? (
        <div className="profile-card">
          <p className="profile-loading">Loading your profile...</p>
        </div>
      ) : loadError ? (
        <div className="profile-card">
          <p className="profile-load-error">{loadError}</p>
        </div>
      ) : (
        <>
          <div className="profile-header-card">
            <div className="profile-avatar">{initials}</div>
            <div className="profile-header-info">
              <h2>{user.fullName}</h2>
              <div className="profile-header-meta">
                <Badge tone="primary">{user.role}</Badge>
                {formData.department && <span>{formData.department}</span>}
                {formData.designation && <span>{formData.designation}</span>}
              </div>
            </div>
          </div>

          {!profileExists && (
            <div className="profile-empty-note">
              You haven't set up your faculty profile yet. Fill in the details below to get started.
            </div>
          )}

          <form className="profile-form-card" onSubmit={handleSubmit} noValidate>
            <div className="profile-section">
              <h3>Faculty information</h3>
              <div className="profile-form-grid">
                <Input
                  id="facultyId"
                  name="facultyId"
                  label="Faculty ID"
                  placeholder="e.g. FAC2026001"
                  value={formData.facultyId}
                  onChange={handleChange}
                  error={errors.facultyId}
                />
                <Input
                  id="department"
                  name="department"
                  label="Department"
                  placeholder="e.g. Computer Science"
                  value={formData.department}
                  onChange={handleChange}
                  error={errors.department}
                />
                <Input
                  id="designation"
                  name="designation"
                  label="Designation"
                  placeholder="e.g. Assistant Professor"
                  value={formData.designation}
                  onChange={handleChange}
                  error={errors.designation}
                />
              </div>
            </div>

            {saveError && <div className="profile-save-error">{saveError}</div>}
            {saveSuccess && (
              <div className="profile-save-success">Profile saved successfully.</div>
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

export default FacultyProfile