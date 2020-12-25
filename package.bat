@echo off 
call mvn clean package -Dmaven.test.skip=true
pause