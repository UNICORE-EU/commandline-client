
_ucc()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="admin-info admin-runcommand batch cat connect copy-file-status cp create-storage create-tss exec get-output job-abort job-restart job-status list-jobs list-sites list-storages list-transfers list-workflows ls metadata mkdir rename resolve rest rm run run-groovy share shell stat system-info umask workflow-control workflow-submit"
  global_opts="--accept-all-issuers --authentication-method --configuration --help --output --preference --registry --verbose --with-timing"


  # parsing for ucc command word (2nd word in commandline.
  # ucc <command> [OPTIONS] <args>)
  if [ $COMP_CWORD -eq 1 ]; then
    COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    return 0
  fi

  # looking for arguments matching to command
  case "${COMP_WORDS[1]}" in
    admin-info)
    opts="$global_opts --all --fields --filter --long --tags"
    ;;
    admin-runcommand)
    opts="$global_opts --sitename --url"
    ;;
    batch)
    opts="$global_opts --f --input --keep --max --max-new-jobs --no-fetch-outcome --no-resource-check --site-weights --sitename --submit-only --threads --update"
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
    opts="$global_opts --asynchronous --bytes --protocol --recursive --resume --schedule"
    ;;
    create-storage)
    opts="$global_opts --factory-url --info --lifetime --sitename --type"
    ;;
    create-tss)
    opts="$global_opts --factory-url --lifetime --sitename"
    ;;
    exec)
    opts="$global_opts --broker --dry-run --keep --sitename"
    ;;
    get-output)
    opts="$global_opts --brief --quiet"
    ;;
    job-abort)
    opts="$global_opts "
    ;;
    job-restart)
    opts="$global_opts "
    ;;
    job-status)
    opts="$global_opts --all --long"
    ;;
    list-jobs)
    opts="$global_opts --all --fields --filter --long --sitename --tags"
    ;;
    list-sites)
    opts="$global_opts --all --fields --filter --long --sitename --tags"
    ;;
    list-storages)
    opts="$global_opts --all --fields --filter --long --tags"
    ;;
    list-transfers)
    opts="$global_opts --all --fields --filter --long --tags"
    ;;
    list-workflows)
    opts="$global_opts --all --fields --filter --long --no-files --no-internal --no-jobs --tags"
    ;;
    ls)
    opts="$global_opts --human --long --recursive --show-metadata"
    ;;
    metadata)
    opts="$global_opts --advanced-query --command --file --query --storage --wait"
    ;;
    mkdir)
    opts="$global_opts "
    ;;
    rename)
    opts="$global_opts "
    ;;
    resolve)
    opts="$global_opts --full --list"
    ;;
    rm)
    opts="$global_opts --quiet"
    ;;
    run)
    opts="$global_opts --allocation --asynchronous --brief --broker --example --quiet --schedule --sitename --tags"
    ;;
    run-groovy)
    opts="$global_opts --expression --file"
    ;;
    share)
    opts="$global_opts --clean --delete"
    ;;
    shell)
    opts="$global_opts --file"
    ;;
    stat)
    opts="$global_opts --human --show-metadata"
    ;;
    system-info)
    opts="$global_opts --long --raw --url-pattern"
    ;;
    umask)
    opts="$global_opts --set"
    ;;
    workflow-control)
    opts="$global_opts "
    ;;
    workflow-submit)
    opts="$global_opts --dry-run --factory-url --sitename --storage-url --tags --ucc-input --wait"
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
