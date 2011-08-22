rem  Location of Eclipse SDK with RCP Delta pack
set TYCHO_TEST_TARGET_PLATFORM=c:\eclipse\3.6.1\eclipse

rem Location of maven used to build tycho 
set TYCHO_M2_HOME=c:\tools\apache-maven-3.0

set MAVEN_OPTS=-Xmx512m -XX:MaxPermSize=128m

call %TYCHO_M2_HOME%\bin\mvn clean install -U -e -V || exit /b

call %TYCHO_M2_HOME%\bin\mvn -f tycho-its\pom.xml clean test -U -e -V -Dtycho.testTargetPlatform=%TYCHO_TEST_TARGET_PLATFORM% || exit /b

rem call %TYCHO_M2_HOME%\bin\mvn -Dsite.generation site:stage || exit /b
