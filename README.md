# Exam Cheat Detection System: Backend (Render) + Chrome Extension

```
ecd/
  backend/     Spring Boot (Java 21) REST API + H2 database + Dockerfile   -> deployed on Render
  extension/   Chrome extension: the exam page, popup and settings          -> loaded in Chrome
  docs/        sample-ExamReport.txt
  render.yaml  optional Render blueprint
```
There is **no separate frontend**: the exam screen is `extension/exam.html`, so students need the extension.
Scores, risk, marks and reports are always calculated by the backend.

---------------------------------------------------------------------------------------------------
## PART A. Put the backend on Render (baby steps)

Render builds Java apps from a **Docker** file (included: `backend/Dockerfile`) and pulls your code from **GitHub**.

### A1. Put the code on GitHub
1. Make a free account at github.com and sign in.
2. Click **+ (top right) > New repository**. Name: `exam-cheat-detection`. Keep it Public or Private. Click **Create repository**.
3. Unzip this project. On the new repo page click **uploading an existing file**.
4. Drag in **the contents of the unzipped folder** (the `backend` folder, `extension` folder, `docs`, `render.yaml`, `README.md`).
   The `backend` folder must be at the top level of the repo, with `Dockerfile` and `pom.xml` directly inside it.
5. Click **Commit changes**.

(Or with Git installed: `git init`, `git add .`, `git commit -m "first"`, `git branch -M main`,
`git remote add origin https://github.com/YOURNAME/exam-cheat-detection.git`, `git push -u origin main`.)

### A2. Create the Render service
1. Go to render.com, sign up (you can use your GitHub login) and open the **Dashboard**.
2. Click **New +** then **Web Service**.
3. Choose **Build and deploy from a Git repository**, connect GitHub, select `exam-cheat-detection`.
4. Fill in the form:
   | Field | Value |
   |---|---|
   | Name | `exam-cheat-backend` (this becomes your URL) |
   | Region | nearest to you |
   | Branch | `main` |
   | **Root Directory** | `backend` |
   | **Runtime / Language** | **Docker** |
   | Instance type | **Free** |
5. Open **Advanced** and set **Health Check Path** to `/api/health`.
6. (Optional now, recommended later) **Environment Variables**: nothing is required. Later you can add
   `CORS_ORIGINS` = `chrome-extension://YOUR_EXTENSION_ID` to accept only your extension (see Part B step 7).
7. Click **Create Web Service**. Watch the **Logs**. The first build takes several minutes (it downloads Maven dependencies).
   Wait for `Started ExamCheatApplication` and the status **Live**.

### A3. Test it
1. At the top of the service page copy the URL, like `https://exam-cheat-backend.onrender.com`.
2. Open `https://YOUR-URL/api/health` in a browser. You should see `{"status":"UP"}`.
3. Open `https://YOUR-URL/api/papers` to see the three papers.

### A4. Things to know about Render FREE
* The service **sleeps after ~15 minutes without traffic**. The first request afterwards can take about a minute. The extension shows a "waking up" message. Open the health URL before a demo.
* The disk is **temporary**: the H2 database and report files are wiped on every restart/redeploy. Students can still **Download report** from the result page, and that file is what you keep. For permanent storage you need a paid disk or an external database (future work).
* Free services have limited monthly hours. If the build fails because of memory, read the log and tell me the error.

### A5. Updating the backend later
1. Change the file on GitHub (open file, pencil icon, edit, **Commit changes**) or push from your computer.
2. Render redeploys automatically. Or click **Manual Deploy > Deploy latest commit** on the service page.
3. Watch **Logs** until **Live**, then retest `/api/health`.

---------------------------------------------------------------------------------------------------
## PART B. Load the extension in Chrome (baby steps)

1. Open the `extension` folder. Open **`config.js`** in Notepad. Change the one line to your Render URL:
   `var ECD_DEFAULT_API = 'https://exam-cheat-backend.onrender.com';` (no slash at the end). Save.
   (For a backend on your own PC use `http://localhost:8080`.)
2. In Chrome type `chrome://extensions` in the address bar, press Enter.
3. Turn **Developer mode** ON (top right).
4. Click **Load unpacked**, select the **`extension`** folder (the one containing `manifest.json`), click **Select Folder**.
5. A card "Exam Cheat Detection Monitor" appears. Click the puzzle icon in the toolbar and **pin** it.
6. The **Settings** page opens by itself. Check the URL, click **Test connection**. Wait up to a minute if Render was asleep. It must say "Backend reachable".
7. (Optional lock-down) On the extension card copy the **ID** (32 letters). In Render > Environment add
   `CORS_ORIGINS` = `chrome-extension://THAT_ID` and save (the service redeploys). The id stays the same while you load the same folder from the same place.
8. Click the toolbar icon > **Open exam page**. Register (ID + name), choose a paper, start.
   "Monitoring active" must be green.

### Test each event
| Event | Points | How |
|---|---|---|
| Tab switch | 2 | Go to another Chrome tab and come back |
| Copy/paste | 3 | Ctrl+C or Ctrl+V on the exam page (blocked but recorded) |
| Window exit | 3 | Click another application so Chrome loses focus, then return |
| Multiple login | 4 | Open the exam page in a **second tab**, register the same ID and start. Refreshing does not count |
| Multiple person | 5 | **Simulated** button on the exam page or in the popup |

Demo: Java paper, answers C A B D D, one tab switch (+2), one paste (+3) = score 5 = SUSPICIOUS, submit, Download report (compare with `docs/sample-ExamReport.txt`).

---------------------------------------------------------------------------------------------------
## PART C. Changing things and updating (baby steps)

### Change the extension
1. Edit a file in the `extension` folder (Notepad or VS Code), save.
2. Go to `chrome://extensions` and click the circular **reload** arrow on the extension card.
3. Close the old exam tab and open **Open exam page** again (old tabs keep the old code).
4. Something wrong? On the card click **service worker** or **Errors**; on the exam page press F12 > Console.
5. New backend URL only: use the extension **Settings** page (no code change, Chrome will ask permission once).
6. Give it to a friend: send them the `extension` folder (zip), they repeat Part B steps 2-5.

### Change the backend
Follow A5. Most useful settings (`backend/src/main/resources/application.properties`):
* `exam.policy.block-clipboard` true/false (stop copy/paste or only record it)
* `app.max-events-per-minute`, `app.session-active-seconds` (the multiple-login window, default 120 s)
You can also override them as **Environment Variables** in Render (e.g. `EXAM_POLICY_BLOCK_CLIPBOARD=false`).

### Run the backend on your own PC instead (optional)
Install JDK 21 and Maven, then in PowerShell: `cd backend` and `mvn spring-boot:run`. Check http://localhost:8080/api/health.
Run tests with `mvn test`.

---------------------------------------------------------------------------------------------------
## API reference (summary)
Errors: `{"status":404,"error":"Not Found","message":"...","timestamp":"..."}`. Everything under `/api/exams/{id}` needs header `X-Session-Token` from `/start`.

| Method & path | Body | Notes |
|---|---|---|
| GET `/api/health`, GET `/` | | status |
| POST `/api/students/register` | `{studentId, name}` | 400 on bad input |
| GET `/api/papers`, `/api/papers/{id}` | | no answer key |
| POST `/api/exams/start` | `{studentId, paperId, clientId}` | resume, or multiple-login rule |
| GET `/api/exams/{id}` | | state, score, risk, events |
| GET `/api/exams/{id}/questions/{n}` | | one question |
| POST `/api/exams/{id}/events` | `{eventId, type, timestamp, source?, details?}` | idempotent by eventId |
| POST `/api/exams/{id}/answers` | `{questionNumber, selectedOption}` | 409 duplicate |
| POST `/api/exams/{id}/finish` | | marks, review, report file |
| GET `/api/exams/{id}/report`, `/report/download` | | 409 until finished |

Event types: TAB_SWITCH 2, COPY_PASTE 3, MULTIPLE_LOGIN 4, WINDOW_EXIT 3, MULTIPLE_PERSON 5 (simulated). Risk: 0-4 NORMAL, 5-9 SUSPICIOUS, 10+ HIGH RISK.
Multiple-login rule: same tab refresh = resume (no penalty); a different tab for the same student while another session was used in the last 120 s = +4 on both sessions.
Security: demo-level only (random session token, no passwords). Anyone can register any Student ID. Not production-secure.

## Where the OOP concepts are (backend/src/main/java/com/examcheat)
| Concept | Where |
|---|---|
| Encapsulation, constructors | `model/Student`, `Question`, `TestPaper` |
| Inheritance + abstract class | `event/CheatEvent` and the five event classes |
| Interface | `model/Scorable` |
| Runtime polymorphism | `ExamMonitor.calculateScore()`, `event/CheatEventFactory` |
| Association / aggregation / composition | `ExamMonitor` (Student, TestPaper, `List<CheatEvent>`), `TestPaper` -> `Question[]` |
| Generics / collections | `List<CheatEvent>`, `Map<Integer,Character>` |
| User-defined exception | `exception/InvalidChoiceException` -> HTTP 400 |
| File handling | `model/Report.saveToFile`, `service/ReportService` |

## Honest verification status
| Item | Status |
|---|---|
| Domain classes, scoring, risk boundaries, papers, marking, report text (`DomainModelTest`, 8 tests) | **Run, passed** |
| Service rules: idempotent events, validation, session isolation, duplicate answers, multiple-login, throttling, report file (52 checks, in-memory repositories) | **Run, passed** |
| Extension event mapping (`node --test extension/test/eventMapper.test.js`, 5 tests) | **Run, passed** |
| Exam page + popup in real Chromium with the extension loaded (against a mock of this API) | **Run, passed**: monitoring active, paste blocked and scored, simulate button, popup values, submit |
| Spring Boot app itself (JPA/H2 mapping, web wiring, `ExamApiIntegrationTest`), the Docker build and the real Render deploy | **NOT run** (my environment could not download Maven dependencies). Run `mvn test` locally if you can; otherwise watch the Render build log and send me any error |
| Real tab-switch / window-blur detection in a normal Chrome window | **Not verified** (cannot be reproduced headless): test it by hand |

## Troubleshooting
| Problem | Fix |
|---|---|
| Render build fails | Open Logs, copy the red error to me. Check Root Directory is `backend` and Runtime is Docker |
| `Root directory does not exist` | The `backend` folder is not at the top level of the repo |
| Extension says Backend offline / cannot reach | Wrong URL in Settings, or Render is asleep: open `/api/health` and wait a minute |
| CORS error in the console | Remove `CORS_ORIGINS` in Render or set it to `chrome-extension://YOUR_ID` exactly |
| "Permission was not granted" in Settings | Click Save again and press Allow |
| Old behaviour after editing code | Reload the extension on `chrome://extensions`, then open a **new** exam tab |
| All data disappeared | Free Render restarts wipe the disk. Download reports right after submitting |
| Window exit not counted | Chrome only reports focus loss of its own window |
