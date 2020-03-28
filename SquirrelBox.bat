@ECHO OFF
java -jar "SquirrelBox 1.6.jar"
IF NOT ERRORLEVEL 1 GOTO END
ECHO Maybe you need the current Java Runtime Environment? (http://java.sun.com)
PAUSE
:END