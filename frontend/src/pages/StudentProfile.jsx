import { useEffect, useState } from 'react'
import { LayoutDashboard, UserRound, Users, FileText, ListChecks, Sparkles, CheckCircle2, AlertCircle, ArrowRight, ArrowLeft, RefreshCw, Award } from 'lucide-react'
import DashboardLayout from '../layouts/DashboardLayout.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { getStudentProfile, updateStudentProfile } from '../services/profileService.js'
import { getMyAssessment, getAssessmentQuiz, submitAssessmentQuiz } from '../services/assessmentService.js'
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
  { label: 'Tasks / Projects', icon: ListChecks, path: null },
]

const emptyForm = { studentId: '', department: '', year: '', bio: '' }

const skillFields = [
  { name: 'frontendScore', label: 'Frontend' },
  { name: 'backendScore', label: 'Backend' },
  { name: 'databaseScore', label: 'Database' },
  { name: 'devopsScore', label: 'DevOps' },
  { name: 'mlAiScore', label: 'ML / AI' },
]

const interestOptions = ['Fullstack', 'Frontend', 'Backend', 'AI/ML', 'DevOps', 'Data', 'QA']

function getProficiency(score) {
  if (score >= 9) return { label: 'Expert', color: '#10b981' }
  if (score >= 7) return { label: 'Advanced', color: '#6366f1' }
  if (score >= 4) return { label: 'Intermediate', color: '#f59e0b' }
  return { label: 'Foundational', color: '#64748b' }
}

function SkillAssessmentCard() {
  const [assessment, setAssessment] = useState(null)
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [mode, setMode] = useState('view') // 'view' | 'prompt' | 'quiz' | 'result'

  // Quiz state
  const [quizData, setQuizData] = useState(null)
  const [loadingQuiz, setLoadingQuiz] = useState(false)
  const [currentQIndex, setCurrentQIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [primaryInterest, setPrimaryInterest] = useState('Fullstack')
  const [pastProjects, setPastProjects] = useState('')
  const [submittingQuiz, setSubmittingQuiz] = useState(false)
  const [quizResult, setQuizResult] = useState(null)
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    loadAssessment()
  }, [])

  const loadAssessment = async () => {
    setLoadingInitial(true)
    try {
      const res = await getMyAssessment()
      setAssessment(res.data)
      setPrimaryInterest(res.data.primaryInterest || 'Fullstack')
      setPastProjects(res.data.pastProjects || '')
      setMode('view')
    } catch {
      setAssessment(null)
      setMode('prompt')
    } finally {
      setLoadingInitial(false)
    }
  }

  const startQuiz = async () => {
    setLoadingQuiz(true)
    setErrorMsg('')
    try {
      const res = await getAssessmentQuiz()
      setQuizData(res.data)
      setCurrentQIndex(0)
      setAnswers({})
      setMode('quiz')
    } catch {
      setErrorMsg('Could not load quiz questions. Please ensure the backend is running.')
    } finally {
      setLoadingQuiz(false)
    }
  }

  const handleSelectOption = (qId, optionIndex) => {
    setAnswers((prev) => ({ ...prev, [qId]: optionIndex }))
  }

  const handleSubmitQuiz = async () => {
    setSubmittingQuiz(true)
    setErrorMsg('')
    try {
      const payload = {
        quizId: quizData.quizId,
        answers,
        primaryInterest,
        pastProjects,
      }
      const res = await submitAssessmentQuiz(payload)
      setQuizResult(res.data)
      setAssessment(res.data.assessment)
      setMode('result')
    } catch {
      setErrorMsg('Failed to submit assessment. Please try again.')
    } finally {
      setSubmittingQuiz(false)
    }
  }

  if (loadingInitial) {
    return (
      <div className="profile-form-card">
        <p className="profile-loading">Loading skill assessment...</p>
      </div>
    )
  }

  // 1. PROMPT VIEW: When student hasn't taken the assessment yet
  if (mode === 'prompt') {
    return (
      <div className="profile-form-card">
        <div className="profile-section">
          <div className="ai-badge">
            <Sparkles size={14} /> AI Real-Time Assessment
          </div>
          <h3>Technical Skill Assessment</h3>
          <p className="profile-section-hint">
            Instead of self-ratings, take a quick 5-question AI technical challenge covering Frontend, Backend, Database, DevOps, and ML/AI. Your real-time performance directly determines your team role suitability.
          </p>
        </div>
        {errorMsg && <div className="profile-save-error">{errorMsg}</div>}
        <div className="profile-form-actions">
          <Button type="button" onClick={startQuiz} loading={loadingQuiz}>
            <Sparkles size={16} style={{ marginRight: '6px' }} />
            Start AI Skill MCQ Quiz
          </Button>
        </div>
      </div>
    )
  }

  // 2. QUIZ VIEW: Live MCQ Test
  if (mode === 'quiz' && quizData) {
    const questions = quizData.questions || []
    const isSurveyStep = currentQIndex >= questions.length
    const currentQ = !isSurveyStep ? questions[currentQIndex] : null

    return (
      <div className="ai-quiz-card">
        <div className="ai-quiz-top">
          <div className="ai-badge">
            <Sparkles size={14} /> AI Technical Challenge
          </div>
          <div className="ai-quiz-progress-text">
            {isSurveyStep ? 'Final Step: Interests' : `Question ${currentQIndex + 1} of ${questions.length}`}
          </div>
        </div>

        {errorMsg && <div className="profile-save-error">{errorMsg}</div>}

        {!isSurveyStep && currentQ && (
          <>
            <div>
              <span className="ai-quiz-category-tag">{currentQ.category}</span>
            </div>
            <h4 className="ai-quiz-question-text">{currentQ.question}</h4>

            <div className="ai-quiz-options">
              {currentQ.options.map((opt, optIdx) => {
                const isSelected = answers[currentQ.id] === optIdx
                return (
                  <button
                    key={optIdx}
                    type="button"
                    className={`ai-quiz-option-btn ${isSelected ? 'selected' : ''}`}
                    onClick={() => handleSelectOption(currentQ.id, optIdx)}
                  >
                    <span className="ai-option-marker">
                      {String.fromCharCode(65 + optIdx)}
                    </span>
                    <span>{opt}</span>
                  </button>
                )
              })}
            </div>

            <div className="ai-quiz-nav">
              <Button
                type="button"
                variant="secondary"
                disabled={currentQIndex === 0}
                onClick={() => setCurrentQIndex((i) => i - 1)}
              >
                <ArrowLeft size={16} style={{ marginRight: '4px' }} /> Previous
              </Button>

              <Button
                type="button"
                disabled={answers[currentQ.id] === undefined}
                onClick={() => setCurrentQIndex((i) => i + 1)}
              >
                Next <ArrowRight size={16} style={{ marginLeft: '4px' }} />
              </Button>
            </div>
          </>
        )}

        {isSurveyStep && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h4 className="ai-quiz-question-text">Almost done! Set your preferences:</h4>
            <div className="ui-field">
              <label htmlFor="quizInterest" className="ui-label">
                Primary Area of Interest
              </label>
              <select
                id="quizInterest"
                className="ui-input"
                value={primaryInterest}
                onChange={(e) => setPrimaryInterest(e.target.value)}
              >
                {interestOptions.map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
              </select>
            </div>

            <Textarea
              id="quizPastProjects"
              label="Past Projects & Experience (Optional)"
              placeholder="Describe projects you've built, languages, or tools you've used..."
              value={pastProjects}
              onChange={(e) => setPastProjects(e.target.value)}
            />

            <div className="ai-quiz-nav">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setCurrentQIndex(questions.length - 1)}
              >
                <ArrowLeft size={16} style={{ marginRight: '4px' }} /> Back to Questions
              </Button>

              <Button
                type="button"
                onClick={handleSubmitQuiz}
                loading={submittingQuiz}
              >
                <Award size={16} style={{ marginRight: '6px' }} />
                Submit & Calculate Skill Ratings
              </Button>
            </div>
          </div>
        )}
      </div>
    )
  }

  // 3. RESULT VIEW: Shows performance breakdown
  if (mode === 'result' && quizResult) {
    return (
      <div className="profile-form-card">
        <div className="ai-result-banner">
          <div className="ai-badge">
            <CheckCircle2 size={14} /> Evaluation Complete
          </div>
          <div className="ai-result-score">
            {quizResult.correctCount} / {quizResult.totalQuestions} Correct
          </div>
          <p style={{ margin: 0, color: 'var(--text-secondary)', fontSize: '13.5px' }}>
            Your real-time technical MCQ answers were evaluated. Objective skill ratings (1-10) have been assigned and saved.
          </p>
        </div>

        <div className="profile-section">
          <h3>Question Review & Explanations</h3>
          {quizResult.review &&
            quizResult.review.map((item, idx) => (
              <div
                key={idx}
                className={`ai-review-item ${item.isCorrect ? 'correct' : 'incorrect'}`}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <span className="ai-quiz-category-tag">{item.category}</span>
                  <span style={{ fontSize: '12px', fontWeight: 600, color: item.isCorrect ? 'var(--success)' : 'var(--danger)' }}>
                    {item.isCorrect ? 'Correct (+Score)' : 'Incorrect'}
                  </span>
                </div>
                <div style={{ fontWeight: 600, fontSize: '13.5px', marginBottom: '6px' }}>
                  {item.question}
                </div>
                <div style={{ fontSize: '12.5px', color: 'var(--text-secondary)' }}>
                  Your answer: <strong>{item.selectedIndex >= 0 ? item.options[item.selectedIndex] : 'None'}</strong>
                </div>
                {!item.isCorrect && (
                  <div style={{ fontSize: '12.5px', color: 'var(--success)', marginTop: '2px' }}>
                    Correct answer: <strong>{item.options[item.correctIndex]}</strong>
                  </div>
                )}
                {item.explanation && (
                  <div className="ai-review-explanation">
                    <strong>Explanation:</strong> {item.explanation}
                  </div>
                )}
              </div>
            ))}
        </div>

        <div className="profile-form-actions">
          <Button type="button" onClick={() => setMode('view')}>
            View Verified Skill Profile
          </Button>
        </div>
      </div>
    )
  }

  // 4. VERIFIED VIEW: Shows current skill profile
  return (
    <div className="profile-form-card">
      <div className="profile-section">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
          <div>
            <div className="ai-badge">
              <Award size={14} /> AI-Verified Skill Ratings
            </div>
            <h3>Technical Skill Profile</h3>
            <p className="profile-section-hint">
              Objective skills calibrated through real-time technical MCQ challenges.
            </p>
          </div>
          <Button type="button" variant="secondary" onClick={startQuiz} loading={loadingQuiz}>
            <RefreshCw size={14} style={{ marginRight: '6px' }} /> Retake Assessment
          </Button>
        </div>

        {errorMsg && <div className="profile-save-error">{errorMsg}</div>}

        <div className="ai-verified-grid">
          {skillFields.map((f) => {
            const score = assessment ? assessment[f.name] || 0 : 0
            const prof = getProficiency(score)
            return (
              <div key={f.name} className="ai-skill-card">
                <div className="ai-skill-header">
                  <span>{f.label}</span>
                  <span className="ai-skill-rating">{score} / 10</span>
                </div>
                <div className="ai-skill-bar-bg">
                  <div
                    className="ai-skill-bar-fill"
                    style={{ width: `${Math.min(score * 10, 100)}%` }}
                  />
                </div>
                <span className="ai-skill-level" style={{ color: prof.color, fontWeight: 700 }}>
                  {prof.label}
                </span>
              </div>
            )
          })}
        </div>

        {assessment && (
          <div style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '13px' }}>
            <div>
              <span style={{ color: 'var(--text-muted)' }}>Primary Focus: </span>
              <strong style={{ color: 'var(--text-primary)' }}>{assessment.primaryInterest || 'Not specified'}</strong>
            </div>
            {assessment.pastProjects && (
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Past Experience: </span>
                <span style={{ color: 'var(--text-secondary)' }}>{assessment.pastProjects}</span>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

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
        setErrors({ studentId: 'This Student ID is already in use.' })
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
    <DashboardLayout navItems={studentNavItems}>
      <h1 className="content-title">My Profile</h1>
      <p className="content-subtitle">Manage your academic profile information.</p>

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
                {formData.year !== '' && <span>Year {formData.year}</span>}
              </div>
            </div>
          </div>

          {!profileExists && (
            <div className="profile-empty-note">
              You haven't set up your academic profile yet. Fill in the details below to get started.
            </div>
          )}

          <form className="profile-form-card" onSubmit={handleSubmit} noValidate>
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

          <SkillAssessmentCard />
        </>
      )}
    </DashboardLayout>
  )
}

export default StudentProfile