# Test Cases

Automated tests are in `backend/src/test/java` (JUnit 5) and `extension/test` (node:test).

| # | Test | Input | Expected | Automated | Last run here |
|---|---|---|---|---|---|
| 1 | Event scores | each of 5 events | 2,3,4,3,5 | DomainModelTest | PASS |
| 2 | Risk boundaries | scores 0,4,5,9,10 | NORMAL, NORMAL, SUSPICIOUS, SUSPICIOUS, HIGH RISK | DomainModelTest, AnalyticsTest | PASS |
| 3 | Polymorphic total | tab switch + paste + person | 10 | DomainModelTest | PASS |
| 4 | Marking | answers CABDD on paper 1 | 10 / 10 | DomainModelTest | PASS |
| 5 | Duplicate answer | answer Q1 twice | exception, first answer kept | DomainModelTest | PASS |
| 6 | Bad input | option 'E', question 9, type "FOO" | InvalidChoiceException | DomainModelTest | PASS |
| 7 | Report fields | finished exam | contains all required fields | DomainModelTest | PASS |
| 8 | Count by type | 2 tab switches | `countEvents(TAB_SWITCH)==2` | AnalyticsTest | PASS |
| 9 | Ranking and averages | 2 students | highest first, avg 8.0 | AnalyticsTest | PASS |
| 10 | CSV injection | name `=cmd...` | prefixed with `'` | AnalyticsTest | PASS |
| 11 | Extension dedupe | tab switch then blur | 1 TAB_SWITCH, no WINDOW_EXIT | eventMapper.test.js | PASS |
| 12 | Consent gate | open exam page | Start disabled until box ticked | Playwright (manual script) | PASS |
| 13 | Paste blocked | paste in exam page | event +3, paste blocked | Playwright | PASS |
| 14 | Simulate person | click demo button | +5, SUSPICIOUS | Playwright | PASS |
| 15 | Full exam | answer 5 questions, submit | marks and report shown | Playwright (against a contract mock backend) | PASS |
| 16 | REST API | register, start, events, answers, finish, report | 2xx, 4xx on errors | ExamApiIntegrationTest | **Not run here** (Maven unavailable): run `mvn test` |
| 17 | Console app | scripted menu session | report, summary, CSV | manual, see `docs/console-sample-output.txt` | PASS |
| 18 | Timer | wait for countdown to reach 0 | exam auto-submitted | manual | **Not run** (set `ECD_EXAM_MINUTES=1` to try) |

Manual test on your own PC: switch to another tab during the exam → score +2 and event log entry; click on another window → +3.
