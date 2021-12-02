
_ucc()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="admin-info admin-runcommand batch cat connect copy-file-status cp create-storage create-tss exec get-output job-abort job-restart job-status list-jobs list-sites list-storages list-transfers list-workflows ls metadata mkdir rename resolve rest rm run run-groovy share shell stat system-info umask workflow-control workflow-submit"
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
    opts="$global_opts --fields --all --filter --tags"
    ;;
    admin-runcommand)
    opts="$global_opts --sitename --url"
    ;;
    batch)
    opts="$global_opts --noResourceCheck --threads --keep --siteWeights --sitename --noFetchOutcome --follow --max --submitOnly --update --input --maxNewJobs"
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
    opts="$global_opts --bytes --recursive --resume --asynchronous --schedule --protocol"
    ;;
    create-storage)
    opts="$global_opts --sitename --type --info --factoryURL --lifetime"
    ;;
    create-tss)
    opts="$global_opts --sitename --factoryURL --lifetime"
    ;;
    exec)
    opts="$global_opts --dryRun --broker --sitename --keep"
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
    opts="$global_opts --all"
    ;;
    list-jobs)
    opts="$global_opts --fields --tags --filter --all --sitename"
    ;;
    list-sites)
    opts="$global_opts --fields --tags --filter --all --sitename"
    ;;
    list-storages)
    opts="$global_opts --fields --all --filter --tags"
    ;;
    list-transfers)
    opts="$global_opts --fields --all --filter --tags"
    ;;
    list-workflows)
    opts="$global_opts --fields --tags --filter --nofiles --all --nojobs"
    ;;
    ls)
    opts="$global_opts --recursive --show-metadata --human"
    ;;
    metadata)
    opts="$global_opts --wait --advanced-query --query --file --command --storage"
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
    opts="$global_opts --tags --quiet --broker --brief --sitename --asynchronous --schedule --example"
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
    opts="$global_opts --show-metadata --human"
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
    opts="$global_opts --wait --tags --dryRun --sitename --storageURL --uccInput --factoryURL"
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
