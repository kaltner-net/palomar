_palomar_complete() {
  local current previous
  COMPREPLY=()
  current="${COMP_WORDS[COMP_CWORD]}"
  previous="${COMP_WORDS[COMP_CWORD-1]:-}"
  if [[ "$previous" == "update" ]]; then
    COMPREPLY=( $(compgen -W "--check --status --recover --yes" -- "$current") )
  elif [[ "$previous" == "uninstall" ]]; then
    COMPREPLY=( $(compgen -W "--purge --yes --help" -- "$current") )
  else
    COMPREPLY=( $(compgen -W "install start stop restart status logs pair web claude-status update uninstall" -- "$current") )
  fi
}
complete -F _palomar_complete palomar
