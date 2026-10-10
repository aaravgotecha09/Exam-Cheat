# Exam Cheat Detection System - Web version

Back end: Java (OOP) using only the JDK. No Maven, no libraries.
Front end: one HTML page (web/index.html) served by the Java server.

## Run
    javac Model.java ExamServer.java
    java ExamServer 8080
Then open http://localhost:8080  (or run run.sh / run.bat). Needs JDK 11+.

## Files
- Model.java      Your original OOP classes (Student, Scorable, CheatEvent + 5 events, Question,
                  TestPaper, PaperBank, ExamMonitor, Report, InvalidChoiceException).
- ExamServer.java REST API + static file server. Uses the model classes directly.
- web/index.html  Registration, exam, live proctor panel, result, admin dashboard.

## API
GET  /api/papers
POST /api/start   {studentId,name,paper}      -> session + questions (answers never sent)
POST /api/answer  {sessionId,index,answer}
POST /api/event   {sessionId,type}            type: tab | copy | login | exit | person
GET  /api/status?sessionId=
POST /api/submit  {sessionId}                 -> report, appended to ExamReport.txt
GET  /api/report?sessionId=   GET /api/admin

## Detection
- Tab switch      browser visibilitychange (automatic)
- Window exit     browser blur (automatic)
- Copy/paste      copy/cut/paste events (automatic, also blocked)
- Multiple login  server: same Student ID starts a second exam while one is active (automatic)
- Multiple person simulated button (as in the project brief)
Scores 2/3/4/3/5; NORMAL 0-4, SUSPICIOUS 5-9, HIGH RISK 10+.
