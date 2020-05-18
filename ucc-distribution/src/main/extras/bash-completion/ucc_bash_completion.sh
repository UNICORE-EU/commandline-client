
_ucc()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="admin-info admin-runcommand batch bes-get-output bes-job-status bes-list-att bes-list-jobs bes-submit-job bes-terminate-job broker-run cat chgrp chmod connect connect-to-testgrid copy-file copy-file-status cp create-storage create-tss download-config exec find get-file get-output issue-delegation job-abort job-restart job-status list-applications list-attributes list-jobs list-sites list-storages list-transfers list-workflows ls metadata mkdir put-file rename reservation resolve rm run run-groovy run-test save-attributes setacl share shell stat system-info umask workflow-control workflow-submit wsrf"
  global_opts="--long --raw --configuration --help --output --registry --user --verbose --with-timing --delegationAssertion --authenticationMethod --preference --attributeAssertion --voGroup --includeAttributes --VO --excludeAttributes"


  # parsing for ucc command word (2nd word in commandline.
  # ucc <command> [OPTIONS] <args>)
  if [ $COMP_CWORD -eq 1 ]; then
    COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    return 0
  fi

  # looking for arguments matching to command
  case "${COMP_WORDS[1]}" in
    admin-info)
    opts="$global_opts --filter --all"
    ;;
    admin-runcommand)
    opts="$global_opts --sitename --url"
    ;;
    batch)
    opts="$global_opts --sitename --input --noFetchOutcome --noResourceCheck --update --jsdl --maxNewJobs --siteWeights --follow --threads --max --keep --submitOnly"
    ;;
    bes-get-output)
    opts="$global_opts "
    ;;
    bes-job-status)
    opts="$global_opts "
    ;;
    bes-list-att)
    opts="$global_opts --sitename"
    ;;
    bes-list-jobs)
    opts="$global_opts --sitename"
    ;;
    bes-submit-job)
    opts="$global_opts --sitename --jsdl --stdout --brief --stderr"
    ;;
    bes-terminate-job)
    opts="$global_opts "
    ;;
    broker-run)
    opts="$global_opts --sitename --noFilenameFix --storageURL --asynchronous --lifetime --factoryURL --dryRun"
    ;;
    cat)
    opts="$global_opts --bytes --protocols"
    ;;
    chgrp)
    opts="$global_opts --recursive"
    ;;
    chmod)
    opts="$global_opts --recursive"
    ;;
    connect)
    opts="$global_opts --lifetime"
    ;;
    connect-to-testgrid)
    opts="$global_opts "
    ;;
    copy-file)
    opts="$global_opts --bytes --target --schedule --force-remote --source --asynchronous --protocols"
    ;;
    copy-file-status)
    opts="$global_opts "
    ;;
    cp)
    opts="$global_opts --schedule --resume --asynchronous --protocols"
    ;;
    create-storage)
    opts="$global_opts --sitename --info --type --lifetime --factoryURL"
    ;;
    create-tss)
    opts="$global_opts --sitename --lifetime --factoryURL"
    ;;
    download-config)
    opts="$global_opts --assumeyes --newtruststore --updateexisting"
    ;;
    exec)
    opts="$global_opts --sitename --broker --dryRun --keep"
    ;;
    find)
    opts="$global_opts --name --recursive"
    ;;
    get-file)
    opts="$global_opts --bytes --append --target --recursive --source --protocols"
    ;;
    get-output)
    opts="$global_opts --brief"
    ;;
    issue-delegation)
    opts="$global_opts --sitename --file --target --validity --subject"
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
    list-applications)
    opts="$global_opts --sitename --filter --all"
    ;;
    list-attributes)
    opts="$global_opts "
    ;;
    list-jobs)
    opts="$global_opts --sitename --filter --all"
    ;;
    list-sites)
    opts="$global_opts --sitename --filter --all"
    ;;
    list-storages)
    opts="$global_opts --filter --all"
    ;;
    list-transfers)
    opts="$global_opts --filter --all"
    ;;
    list-workflows)
    opts="$global_opts --filter --nofiles --all --nojobs"
    ;;
    ls)
    opts="$global_opts --human --show-metadata --recursive"
    ;;
    metadata)
    opts="$global_opts --query --wait --file --storage --metadata-service --advanced-query --command"
    ;;
    mkdir)
    opts="$global_opts "
    ;;
    put-file)
    opts="$global_opts --bytes --append --target --recursive --source --protocols"
    ;;
    rename)
    opts="$global_opts "
    ;;
    reservation)
    opts="$global_opts --sitename --jsdl --delete --start --list"
    ;;
    resolve)
    opts="$global_opts --full"
    ;;
    rm)
    opts="$global_opts --quiet"
    ;;
    run)
    opts="$global_opts --sitename --broker --stdout --jsdl --schedule --asynchronous --stderr --dryRun --example --brief"
    ;;
    run-groovy)
    opts="$global_opts --expression --file"
    ;;
    run-test)
    opts="$global_opts --expression --file"
    ;;
    save-attributes)
    opts="$global_opts --prettifyAssertion --attributeListOutFile"
    ;;
    setacl)
    opts="$global_opts --delete --recursive --clean"
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
    opts="$global_opts --sitename --noFilenameFix --wait --workflowName --storageURL --lifetime --factoryURL --dryRun --uccInput"
    ;;

    wsrf)
    #looking for wsrf command
    if [ $COMP_CWORD -eq 2 ]; then
      opts="getproperties destroy extend"
    else
      opts="$global_opts "
    fi
    ;;
  esac
  
  COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
  
  _filedir

}

complete -o filenames -F _ucc ucc
