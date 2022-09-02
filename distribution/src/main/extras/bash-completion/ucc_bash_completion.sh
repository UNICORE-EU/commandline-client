
_ucc()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="admin-info admin-runcommand batch cat connect copy-file-status cp create-storage create-tss exec get-output job-abort job-restart job-status list-jobs list-sites list-storages list-transfers list-workflows ls metadata mkdir rename resolve rest rm run run-groovy share shell stat system-info umask workflow-control workflow-submit"
  global_opts="--configuration --help --output --registry --verbose --with-timing --authenticationMethod --acceptAllIssuers --preference"


  # parsing for ucc command word (2nd word in commandline.
  # ucc <command> [OPTIONS] <args>)
  if [ $COMP_CWORD -eq 1 ]; then
    COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    return 0
  fi

  # looking for arguments matching to command
  case "${COMP_WORDS[1]}" in
    admin-info)
    opts="$global_opts --long --tags --all --filter --fields"
    ;;
    admin-runcommand)
    opts="$global_opts --url --sitename"
    ;;
    batch)
    opts="$global_opts --threads --submitOnly --input --siteWeights --sitename --keep --maxNewJobs --f --max --update --noResourceCheck --noFetchOutcome"
    ;;
    cat)
    opts="$global_opts --protocol --bytes"
    ;;
    connect)
    opts="$global_opts --lifetime"
    ;;
    copy-file-status)
    opts="$global_opts "
    ;;
    cp)
    opts="$global_opts --protocol --resume --schedule --asynchronous --bytes --recursive"
    ;;
    create-storage)
    opts="$global_opts --lifetime --sitename --factoryURL --info --type"
    ;;
    create-tss)
    opts="$global_opts --factoryURL --lifetime --sitename"
    ;;
    exec)
    opts="$global_opts --keep --dryRun --broker --sitename"
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
    opts="$global_opts --long --tags --sitename --all --filter --fields"
    ;;
    list-sites)
    opts="$global_opts --long --tags --sitename --all --filter --fields"
    ;;
    list-storages)
    opts="$global_opts --long --tags --all --filter --fields"
    ;;
    list-transfers)
    opts="$global_opts --long --tags --all --filter --fields"
    ;;
    list-workflows)
    opts="$global_opts --nojobs --long --tags --all --nofiles --filter --fields"
    ;;
    ls)
    opts="$global_opts --human --recursive --show-metadata --long"
    ;;
    metadata)
    opts="$global_opts --file --wait --advanced-query --command --storage --query"
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
    opts="$global_opts --schedule --asynchronous --tags --sitename --brief --example --broker --allocation --quiet"
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
    opts="$global_opts --wait --tags --sitename --factoryURL --uccInput --storageURL --dryRun"
    ;;

    rest)
    #looking for 'rest' command
    if [ $COMP_CWORD -eq 2 ]; then
      opts="get put post delete"
    else
      opts="$global_opts "
    fi
    ;;
  esac
  
  COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
  
  _filedir

}

complete -o filenames -F _ucc ucc
