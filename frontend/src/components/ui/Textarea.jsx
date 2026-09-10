function Textarea({ label, error, hint, id, ...props }) {
  return (
    <div className="ui-field">
      {label && (
        <label htmlFor={id} className="ui-label">
          {label}
        </label>
      )}
      <textarea id={id} className={`ui-textarea ${error ? 'has-error' : ''}`} {...props} />
      {error && <span className="ui-field-error">{error}</span>}
      {!error && hint && <span className="ui-field-hint">{hint}</span>}
    </div>
  )
}

export default Textarea