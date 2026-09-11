complete -c palomar -f
complete -c palomar -n '__fish_use_subcommand' -a 'install start stop restart status logs pair web claude-status update uninstall'
complete -c palomar -n '__fish_seen_subcommand_from update' -l check -d 'Check for an update'
complete -c palomar -n '__fish_seen_subcommand_from update' -l status -d 'Show update status'
complete -c palomar -n '__fish_seen_subcommand_from update' -l recover -d 'Retry recovery'
complete -c palomar -n '__fish_seen_subcommand_from update' -l yes -d 'Skip confirmation'
complete -c palomar -n '__fish_seen_subcommand_from uninstall' -l purge -d 'Remove local Palomar data'
complete -c palomar -n '__fish_seen_subcommand_from uninstall' -l yes -d 'Skip confirmation'
