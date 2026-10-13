# Viva Questions and Answers (Exam Cheat Detection System)

**Team:** Shreya Bhuia (I013), Aarav Gotecha (I019) · NMIMS MPSTME · Java OOP mini project

### 1. What does the project do?
It watches an online MCQ exam for suspicious behaviour (tab switch, copy/paste, window exit, multiple login, simulated multiple person), gives each event points, adds them into a cheat score and classifies the student as NORMAL, SUSPICIOUS or HIGH RISK. It then writes a report file.

### 2. Where is OOP used? (point to the UML diagram `docs/uml-class-diagram.png`)
| Concept | Where |
|---|---|
| Encapsulation | `Student`, `Question`, `TestPaper`: private final fields + getters |
| Abstraction / abstract class | `CheatEvent` (abstract `calculateScore()`, `getEventName()`) |
| Interface | `Scorable`, implemented by `CheatEvent` and `ExamMonitor` |
| Inheritance | 5 event classes extend `CheatEvent` |
| Runtime polymorphism | `ExamMonitor.calculateScore()` loops over `List<CheatEvent>` and calls each subclass's own score |
| Method / constructor overloading | `countEvents(String)` / `countEvents(EventType)`; `Student(id,name)` / `Student(id,name,exam)` |
| Association | `ExamMonitor` – `Student`, `ExamMonitor` – `TestPaper` |
| Aggregation | `ExamMonitor` holds many `CheatEvent`s |
| Composition | `TestPaper` is made of 5 `Question`s |
| Enum with behaviour | `RiskLevel.fromScore()` |
| Generics / collections | `List<CheatEvent>`, `Map<Integer,Character>`, `EnumMap` in `ExamAnalytics` |
| Exception handling | user-defined `InvalidChoiceException`; try/catch in console |
| File handling | `Report.saveToFile`, `CsvExporter.save` |
| Factory pattern | `CheatEventFactory.create(...)` |

### 3. What is the difference between an abstract class and an interface here?
`CheatEvent` is an abstract class because the events share state (time, source, id) and some behaviour. `Scorable` is an interface because "can produce a score" is a capability that unrelated classes (`CheatEvent`, `ExamMonitor`) can both have.

### 4. Explain runtime polymorphism with your code.
`ExamMonitor` stores `CheatEvent` references. `e.calculateScore()` runs the version in the real object (2 for `TabSwitchEvent`, 5 for `PersonDetectionEvent`), decided at runtime.

### 5. Scoring and risk rules?
Tab switch 2, copy/paste 3, multiple login 4, window exit 3, multiple person 5. Total 0–4 NORMAL, 5–9 SUSPICIOUS, 10+ HIGH RISK.

### 6. Why is the Spring Boot backend the source of truth?
A browser can be tampered with. The extension only sends the *type* of event; the server picks the points, validates time and rate, and removes duplicates.

### 7. Why a Chrome extension?
A normal web page cannot know if the student leaves it. The extension hosts the exam page and uses `visibilitychange`, `blur` and clipboard events.

### 8. Does it prove cheating?
No. Browser signals are indicators for a human examiner. Multiple-person detection is only **simulated** because real detection needs camera + ML and consent.

### 9. How is a real tab switch different from a window exit?
Tab switch = page becomes hidden (`document.hidden`). Window exit = window loses focus while still visible. The mapper avoids double-counting one action.

### 10. How are multiple logins detected?
Each browser tab has a client id. A second client id using the same Student ID within 120 s of the first adds 4 points to the sessions.

### 11. Where is data stored?
H2 database (JPA entities `StudentEntity`, `ExamSessionEntity`, `AnswerEntity`, `SuspiciousEventEntity`) plus one report text file per session. On Render's free plan the disk is temporary; MongoDB Atlas or PostgreSQL would be used for permanent storage.

### 12. Why also a console version?
It runs the same domain classes with only `javac`, showing the OOP design is independent of Spring and the browser. Run `console/run-console.bat`.

### 13. What would you improve?
Real face/person detection with consent, teacher login with roles, permanent database, a proper admin dashboard, and extension store publishing.

### 14. What are the privacy considerations?
The student ticks a consent box before the exam. Only event types and timestamps are stored, not screen contents or clipboard text.
