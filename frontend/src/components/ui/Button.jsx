function Button({ children, variant = 'primary', loading = false, disabled, ...props }) {
  return (
    <button
      className={`ui-button ui-button-${variant}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? <span className="ui-button-spinner" aria-hidden="true" /> : null}
      <span>{children}</span>
    </button>
  )
}

export default Button