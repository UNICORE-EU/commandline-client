@echo off

rem
rem Windows startscript for the UCC
rem

rem Figure out where UCC is installed
set UCC_HOME=%~d0%~p0..

rem Creating user UCC configuration directory
set USER_UCC_HOME=%HOMEPATH%\.ucc
if not exist %USER_UCC_HOME% (
   echo Creating user's preferences directory at %USER_UCC_HOME%
   mkdir "%USER_UCC_HOME%"
)
if not exist %USER_UCC_HOME%\trusted-certs (
   echo Creating trusted certs directory at %USER_UCC_HOME%\trusted-certs
   mkdir "%USER_UCC_HOME%"\trusted-certs
)

rem Copy preferences file if not exists
if not exist %USER_UCC_HOME%\preferences (
   echo Copying preferences file to %USER_UCC_HOME%
   @copy %UCC_HOME%\conf\preferences.windows %USER_UCC_HOME%\preferences
)

rem Build the Java classpath
set CLASSPATH=.
for %%i in ("%UCC_HOME%\lib\*.jar") do ( call :cpappend %%i )

set VM_ARGS1="-Ducc.preferences=%USER_UCC_HOME%\preferences"

set VM_ARGS2="-Dlog4j.configurationFile=file:///%UCC_HOME%\conf\logging.properties"

set CMD_LINE_ARGS=%*

rem
rem Go
rem
java %VM_ARGS1% %VM_ARGS2% eu.unicore.ucc.UCC %CMD_LINE_ARGS%
goto :eof


rem
rem Helper to append stuff to the classpath
rem
:cpappend
if ""%1"" == """" goto done
set CLASSPATH=%CLASSPATH%;%*
shift
goto :cpappend
:done
goto :eof
