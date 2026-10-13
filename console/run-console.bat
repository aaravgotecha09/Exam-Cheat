@echo off
REM Compiles and runs the console version (needs only a JDK 17+; no Maven, no Spring).
set SRC=..\backend\src\main\java\com\examcheat
if not exist out mkdir out
javac -d out %SRC%\model\*.java %SRC%\event\*.java %SRC%\exception\InvalidChoiceException.java %SRC%\analytics\*.java %SRC%\console\*.java
java -cp out com.examcheat.console.ConsoleApp
pause
