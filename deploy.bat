@echo off 
call mvn clean deploy -Dmaven.test.skip=true -P sonatype-oss-release
pause