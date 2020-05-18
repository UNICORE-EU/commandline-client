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

rem Copying configuration files if not exists
set USER_UCC_FILES=preferences extensions user-keystore.jks 
for %%i in (%USER_UCC_FILES%) do (
	if not exist %USER_UCC_HOME%\%%i (
		echo Copying file %%i to %USER_UCC_HOME%
		@copy %UCC_HOME%\conf\%%i %USER_UCC_HOME%
	)
)

rem Build the Java classpath
set CLASSPATH=.
for %%i in ("%UCC_HOME%\lib\*.jar") do ( call :cpappend %%i )

set VM_ARGS1="-Ducc.extensions=%UCC_HOME%\conf\extensions"
set VM_ARGS2="-Dlog4j.configuration=file:///%UCC_HOME%\conf\logging.properties"
set VM_ARGS3="-Ducc.preferences=%USER_UCC_HOME%\preferences"

set CMD_LINE_ARGS=%*

rem
rem Go
rem
java %VM_ARGS1% %VM_ARGS2% %VM_ARGS3% de.fzj.unicore.ucc.UCC %CMD_LINE_ARGS%
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
