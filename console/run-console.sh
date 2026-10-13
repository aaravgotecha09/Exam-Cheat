#!/bin/sh
# Compiles and runs the console version (needs only a JDK 17+; no Maven, no Spring).
S=../backend/src/main/java/com/examcheat
mkdir -p out
javac -d out $S/model/*.java $S/event/*.java $S/exception/InvalidChoiceException.java $S/analytics/*.java $S/console/*.java && java -cp out com.examcheat.console.ConsoleApp
