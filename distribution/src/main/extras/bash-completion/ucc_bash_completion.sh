
_ucc()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="admin-info admin-runcommand batch cat connect copy-file-status cp create-storage create-tss exec get-output job-abort job-restart job-status list-jobs list-sites list-storages list-transfers list-workflows ls metadata mkdir rename resolve rest rm run run-groovy share shell stat system-info umask workflow-control workflow-submit"
  global_opts="--configuration --help --output --registry --verbose --with-timing --authentication-method --accept-all-issuers --preference"


  # parsing for ucc command word (2nd word in commandline.
  # ucc <command> [OPTIONS] <args>)
  if [ $COMP_CWORD -eq 1 ]; then
    COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    return 0
  fi

  # looking for arguments matching to command
  case "${COMP_WORDS[1]}" in
    admin-info)
    opts="$global_opts --fields --long --all --tags --filter"
    ;;
    admin-runcommand)
    opts="$global_opts --sitename --url"
    ;;
    batch)
    opts="$global_opts --input --max --f --site-weights --threads --max-new-jobs --update --submit-only --no-fetch-outcome --keep --sitename --no-resource-check"
    ;;
    cat)
    opts="$global_opts --bytes --protocol"
    ;;
    connect)
    opts="$global_opts --lifetime"
    ;;
    copy-file-status)
    opts="$global_opts "
    ;;
    cp)
    opts="$global_opts --bytes --protocol --recursive --resume --asynchronous --schedule"
    ;;
    create-storage)
    opts="$global_opts --type --info --lifetime --sitename --factory-url"
    ;;
    create-tss)
    opts="$global_opts --sitename --factory-url --lifetime"
    ;;
    exec)
    opts="$global_opts --keep --sitename --dry-run --broker"
    ;;
    get-output)
    opts="$global_opts --quiet --brief"
    ;;
    job-abort)
    opts="$global_opts "
    ;;
    job-restart)
    opts="$global_opts "
    ;;
    job-status)
    opts="$global_opts --long --all"
    ;;
    list-jobs)
    opts="$global_opts --fields --long --all --tags --sitename --filter"
    ;;
    list-sites)
    opts="$global_opts --fields --long --all --tags --sitename --filter"
    ;;
    list-storages)
    opts="$global_opts --fields --long --all --tags --filter"
    ;;
    list-transfers)
    opts="$global_opts --fields --long --all --tags --filter"
    ;;
    list-workflows)
    opts="$global_opts --no-files --fields --no-jobs --long --all --tags --no-internal --filter"
    ;;
    ls)
    opts="$global_opts --human --recursive --long --show-metadata"
    ;;
    metadata)
    opts="$global_opts --command --advanced-query --wait --storage --file --query"
    ;;
    mkdir)
    opts="$global_opts "
    ;;
    rename)
    opts="$global_opts "
    ;;
    resolve)
    opts="$global_opts --list --full"
    ;;
    rm)
    opts="$global_opts --quiet"
    ;;
    run)
    opts="$global_opts --example --broker --quiet --brief --asynchronous --tags --sitename --schedule --allocation"
    ;;
    run-groovy)
    opts="$global_opts --file --expression"
    ;;
    share)
    opts="$global_opts --delete --clean"
    ;;
    shell)
    opts="$global_opts --file"
    ;;
    stat)
    opts="$global_opts --human --show-metadata"
    ;;
    system-info)
    opts="$global_opts --url-pattern --long --raw"
    ;;
    umask)
    opts="$global_opts --set"
    ;;
    workflow-control)
    opts="$global_opts "
    ;;
    workflow-submit)
    opts="$global_opts --ucc-input --storage-url --wait --tags --sitename --factory-url --dry-run"
    ;;

    rest)
    #looking for 'rest' command
    if [ $COMP_CWORD -eq 2 ]; then
      opts="get put post delete $global_opts"
    else
      opts="$global_opts "
    fi
    ;;
  esac
  
  COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
  
  _filedir

}

complete -o filenames -F _ucc ucc
