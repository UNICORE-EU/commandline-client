
_ucc()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="admin-info admin-runcommand batch cat connect copy-file-status cp create-storage create-tss exec get-output job-abort job-restart job-status list-attributes list-jobs list-sites list-storages list-transfers list-workflows ls metadata mkdir rename resolve rest rm run run-groovy share shell stat system-info umask workflow-control workflow-submit"
  global_opts="--long --url-pattern --raw --configuration --help --output --registry --verbose --with-timing --authenticationMethod --acceptAllIssuers --preference"


  # parsing for ucc command word (2nd word in commandline.
  # ucc <command> [OPTIONS] <args>)
  if [ $COMP_CWORD -eq 1 ]; then
    COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    return 0
  fi

  # looking for arguments matching to command
  case "${COMP_WORDS[1]}" in
    admin-info)
    opts="$global_opts --filter --fields --all --tags"
    ;;
    admin-runcommand)
    opts="$global_opts --sitename --url"
    ;;
    batch)
    opts="$global_opts --sitename --input --noFetchOutcome --noResourceCheck --update --maxNewJobs --siteWeights --follow --threads --max --keep --submitOnly"
    ;;
    cat)
    opts="$global_opts --bytes --protocols"
    ;;
    connect)
    opts="$global_opts --lifetime"
    ;;
    copy-file-status)
    opts="$global_opts "
    ;;
    cp)
    opts="$global_opts --bytes --resume --schedule --asynchronous --recursive --protocols"
    ;;
    create-storage)
    opts="$global_opts --sitename --info --type --lifetime --factoryURL"
    ;;
    create-tss)
    opts="$global_opts --sitename --lifetime --factoryURL"
    ;;
    exec)
    opts="$global_opts --sitename --broker --dryRun --keep"
    ;;
    get-output)
    opts="$global_opts --brief"
    ;;
    job-abort)
    opts="$global_opts "
    ;;
    job-restart)
    opts="$global_opts "
    ;;
    job-status)
    opts="$global_opts --all"
    ;;
    list-attributes)
    opts="$global_opts "
    ;;
    list-jobs)
    opts="$global_opts --sitename --fields --all --tags --filter"
    ;;
    list-sites)
    opts="$global_opts --sitename --fields --all --tags --filter"
    ;;
    list-storages)
    opts="$global_opts --filter --fields --all --tags"
    ;;
    list-transfers)
    opts="$global_opts --filter --fields --all --tags"
    ;;
    list-workflows)
    opts="$global_opts --fields --nofiles --nojobs --filter --all --tags"
    ;;
    ls)
    opts="$global_opts --human --show-metadata --recursive"
    ;;
    metadata)
    opts="$global_opts --query --wait --file --storage --advanced-query --command"
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
    opts="$global_opts --sitename --broker --stdout --schedule --asynchronous --stderr --dryRun --example --brief"
    ;;
    run-groovy)
    opts="$global_opts --expression --file"
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
    opts="$global_opts "
    ;;
    umask)
    opts="$global_opts --set"
    ;;
    workflow-control)
    opts="$global_opts "
    ;;
    workflow-submit)
    opts="$global_opts --sitename --wait --dryRun --uccInput --factoryURL --storageURL --name"
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
