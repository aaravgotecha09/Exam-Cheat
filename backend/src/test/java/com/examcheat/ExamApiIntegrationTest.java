package com.examcheat;

import com.examcheat.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full-stack tests: HTTP -> controller -> service -> JPA -> H2 (in memory). */
@SpringBootTest
@AutoConfigureMockMvc
class ExamApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SuspiciousEventRepository eventRepo;
    @Autowired AnswerRepository answerRepo;
    @Autowired ExamSessionRepository sessionRepo;
    @Autowired StudentRepository studentRepo;
    final ObjectMapper om = new ObjectMapper();
    int seq = 0;

    @BeforeEach
    void clean() {
        eventRepo.deleteAll();
        answerRepo.deleteAll();
        sessionRepo.deleteAll();
        studentRepo.deleteAll();
    }

    // ---------- helpers ----------
    JsonNode send(MockHttpServletRequestBuilder b, String body, String token, int expected) throws Exception {
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) b.header("X-Session-Token", token);
        MvcResult r = mvc.perform(b).andExpect(status().is(expected)).andReturn();
        String text = r.getResponse().getContentAsString();
        return text.isEmpty() ? null : om.readTree(text);
    }

    JsonNode register(int id, String name) throws Exception {
        return send(post("/api/students/register"), "{\"studentId\":" + id + ",\"name\":\"" + name + "\"}", null, 200);
    }

    JsonNode start(int id, int paper, String client) throws Exception {
        return send(post("/api/exams/start"), "{\"studentId\":" + id + ",\"paperId\":" + paper + ",\"clientId\":\"" + client + "\"}", null, 200);
    }

    JsonNode event(String sid, String token, String eventId, String type, String source, int expected) throws Exception {
        String body = "{\"eventId\":\"" + eventId + "\",\"type\":\"" + type + "\",\"timestamp\":\"" + Instant.now() + "\",\"source\":\"" + source + "\"}";
        return send(post("/api/exams/" + sid + "/events"), body, token, expected);
    }

    String newEventId() { return "evt-test-" + (++seq) + "-abcdef"; }

    // ---------- tests ----------
    @Test
    void registrationValidatesInput() throws Exception {
        JsonNode ok = register(1013, "Shreya Bhuia");
        assertEquals("Shreya Bhuia", ok.get("name").asText());
        send(post("/api/students/register"), "{\"studentId\":\"abc\",\"name\":\"x\"}", null, 400);
        send(post("/api/students/register"), "{\"studentId\":5,\"name\":\"   \"}", null, 400);
        send(post("/api/students/register"), "{\"studentId\":-4,\"name\":\"x\"}", null, 400);
    }

    @Test
    void papersAreReturnedWithoutTheAnswerKey() throws Exception {
        JsonNode list = send(get("/api/papers"), null, null, 200);
        assertEquals(3, list.size());
        assertEquals("DBMS", list.get(2).get("name").asText());
        for (int p = 1; p <= 3; p++) {
            JsonNode paper = send(get("/api/papers/" + p), null, null, 200);
            assertEquals(5, paper.get("questions").size());
            assertEquals(10, paper.get("totalMarks").asInt());
            String json = paper.toString().toLowerCase();
            assertFalse(json.contains("correct"), "answer key must not be exposed");
        }
        send(get("/api/papers/9"), null, null, 400);
    }

    @Test
    void startRequiresRegisteredStudentAndValidPaper() throws Exception {
        send(post("/api/exams/start"), "{\"studentId\":999,\"paperId\":1,\"clientId\":\"tab-a\"}", null, 404);
        register(1, "A");
        send(post("/api/exams/start"), "{\"studentId\":1,\"paperId\":7,\"clientId\":\"tab-a\"}", null, 400);
    }

    @Test
    void eachEventScoresCorrectlyAndRiskIsClassified() throws Exception {
        register(10, "Rahul");
        JsonNode s = start(10, 1, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        int[] points = {2, 3, 3};
        String[] types = {"TAB_SWITCH", "COPY_PASTE", "WINDOW_EXIT"};
        int total = 0;
        for (int i = 0; i < 3; i++) {
            JsonNode r = event(sid, tok, newEventId(), types[i], "BROWSER_EXTENSION", 200);
            assertEquals(points[i], r.get("event").get("points").asInt());
            total += points[i];
            assertEquals(total, r.get("cheatScore").asInt());
        }
        assertEquals("SUSPICIOUS", send(get("/api/exams/" + sid), null, tok, 200).get("risk").asText());
        JsonNode last = event(sid, tok, newEventId(), "MULTIPLE_PERSON", "SIMULATED_DEMO", 200);
        assertEquals(13, last.get("cheatScore").asInt());
        assertEquals("HIGH RISK", last.get("risk").asText());
        assertEquals(4, last.get("eventCount").asInt());
    }

    @Test
    void eventsArePersistedInTheDatabase() throws Exception {
        register(11, "P");
        JsonNode s = start(11, 2, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        event(sid, tok, newEventId(), "TAB_SWITCH", "BROWSER_EXTENSION", 200);
        assertEquals(1, eventRepo.findBySessionIdOrderByOccurredAtAscIdAsc(sid).size());
        assertEquals(2, eventRepo.findBySessionIdOrderByOccurredAtAscIdAsc(sid).get(0).getPoints());
    }

    @Test
    void duplicateEventIdIsNotScoredTwice() throws Exception {
        register(12, "P");
        JsonNode s = start(12, 1, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        String id = newEventId();
        JsonNode first = event(sid, tok, id, "COPY_PASTE", "BROWSER_EXTENSION", 200);
        JsonNode retry = event(sid, tok, id, "COPY_PASTE", "BROWSER_EXTENSION", 200);
        assertFalse(first.get("duplicate").asBoolean());
        assertTrue(retry.get("duplicate").asBoolean());
        assertEquals(3, retry.get("cheatScore").asInt());
        assertEquals(1, retry.get("eventCount").asInt());
    }

    @Test
    void invalidEventsAreRejected() throws Exception {
        register(13, "P");
        JsonNode s = start(13, 1, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        event(sid, tok, newEventId(), "NOT_A_TYPE", "BROWSER_EXTENSION", 400);
        event(sid, tok, newEventId(), "TAB_SWITCH", "SERVER_RULE", 400);
        event(sid, tok, "short", "TAB_SWITCH", "BROWSER_EXTENSION", 400);
        send(post("/api/exams/" + sid + "/events"),
                "{\"eventId\":\"" + newEventId() + "\",\"type\":\"TAB_SWITCH\",\"timestamp\":\"yesterday\"}", tok, 400);
        send(post("/api/exams/" + sid + "/events"),
                "{\"eventId\":\"" + newEventId() + "\",\"type\":\"TAB_SWITCH\",\"timestamp\":\"" + Instant.now().plusSeconds(3600) + "\"}", tok, 400);
    }

    @Test
    void unknownSessionAndMissingTokenAreRejected() throws Exception {
        send(get("/api/exams/does-not-exist"), null, "x", 404);
        register(14, "P");
        JsonNode s = start(14, 1, "tab-1");
        send(get("/api/exams/" + s.get("sessionId").asText()), null, null, 401);
    }

    @Test
    void sessionsAreIsolatedFromEachOther() throws Exception {
        register(20, "Alice");
        register(21, "Bob");
        JsonNode a = start(20, 1, "tab-a");
        JsonNode b = start(21, 1, "tab-b");
        String aSid = a.get("sessionId").asText(), aTok = a.get("sessionToken").asText();
        String bSid = b.get("sessionId").asText(), bTok = b.get("sessionToken").asText();
        // Alice's token cannot read, score or answer in Bob's session
        send(get("/api/exams/" + bSid), null, aTok, 403);
        event(bSid, aTok, newEventId(), "TAB_SWITCH", "BROWSER_EXTENSION", 403);
        send(post("/api/exams/" + bSid + "/answers"), "{\"questionNumber\":1,\"selectedOption\":\"A\"}", aTok, 403);
        event(aSid, aTok, newEventId(), "TAB_SWITCH", "BROWSER_EXTENSION", 200);
        assertEquals(0, send(get("/api/exams/" + bSid), null, bTok, 200).get("cheatScore").asInt());
        assertEquals(2, send(get("/api/exams/" + aSid), null, aTok, 200).get("cheatScore").asInt());
    }

    @Test
    void answersAreMarkedOnTheServerAndDuplicatesRejected() throws Exception {
        register(30, "P");
        JsonNode s = start(30, 1, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        String[] chosen = {"C", "A", "A", "D", "D"};            // Q3 wrong
        for (int i = 0; i < 5; i++) {
            JsonNode r = send(post("/api/exams/" + sid + "/answers"),
                    "{\"questionNumber\":" + (i + 1) + ",\"selectedOption\":\"" + chosen[i] + "\"}", tok, 200);
            assertEquals(i + 1, r.get("attempted").asInt());
            assertFalse(r.toString().contains("correct"), "no correctness leaked during the exam");
        }
        send(post("/api/exams/" + sid + "/answers"), "{\"questionNumber\":1,\"selectedOption\":\"B\"}", tok, 409);
        send(post("/api/exams/" + sid + "/answers"), "{\"questionNumber\":2,\"selectedOption\":\"Z\"}", tok, 400);
        send(post("/api/exams/" + sid + "/answers"), "{\"questionNumber\":9,\"selectedOption\":\"A\"}", tok, 400);
        JsonNode state = send(get("/api/exams/" + sid), null, tok, 200);
        assertNull(state.get("marksObtained"), "marks hidden until finished");

        JsonNode fin = send(post("/api/exams/" + sid + "/finish"), null, tok, 200);
        assertEquals(8, fin.get("marksObtained").asInt());
        assertEquals(10, fin.get("totalMarks").asInt());
        assertFalse(fin.get("review").get(2).get("correct").asBoolean());
        assertEquals("B", fin.get("review").get(2).get("correctOption").asText());
        send(post("/api/exams/" + sid + "/answers"), "{\"questionNumber\":1,\"selectedOption\":\"A\"}", tok, 409);
    }

    @Test
    void questionEndpointReturnsOneQuestionWithoutAnswerKey() throws Exception {
        register(31, "P");
        JsonNode s = start(31, 3, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        JsonNode q = send(get("/api/exams/" + sid + "/questions/1"), null, tok, 200);
        assertEquals("Which SQL command is used to retrieve data from a table?", q.get("text").asText());
        assertEquals(4, q.get("options").size());
        assertFalse(q.toString().toLowerCase().contains("correct"));
        send(get("/api/exams/" + sid + "/questions/6"), null, tok, 400);
    }

    @Test
    void demoScenarioScoreFiveIsSuspiciousAndReportIsWritten() throws Exception {
        register(1013, "Shreya Bhuia");
        JsonNode s = start(1013, 1, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        String[] ans = {"C", "A", "B", "D", "D"};
        for (int i = 0; i < 5; i++) {
            send(post("/api/exams/" + sid + "/answers"), "{\"questionNumber\":" + (i + 1) + ",\"selectedOption\":\"" + ans[i] + "\"}", tok, 200);
        }
        event(sid, tok, newEventId(), "TAB_SWITCH", "BROWSER_EXTENSION", 200);
        JsonNode r = event(sid, tok, newEventId(), "COPY_PASTE", "BROWSER_EXTENSION", 200);
        assertEquals(5, r.get("cheatScore").asInt());
        assertEquals("SUSPICIOUS", r.get("risk").asText());

        send(get("/api/exams/" + sid + "/report"), null, tok, 409);       // not finished yet
        JsonNode fin = send(post("/api/exams/" + sid + "/finish"), null, tok, 200);
        assertEquals(10, fin.get("marksObtained").asInt());

        JsonNode rep = send(get("/api/exams/" + sid + "/report"), null, tok, 200);
        String text = rep.get("report").asText();
        assertTrue(text.contains("Marks Obtained      : 10 / 10"));
        assertTrue(text.contains("Cheat Score         : 5"));
        assertTrue(text.contains("Risk Level          : SUSPICIOUS"));
        assertTrue(text.indexOf("Tab switch at") < text.indexOf("Copy/Paste attempt at"));

        Path file = Paths.get("target/test-reports").toAbsolutePath().resolve(rep.get("fileName").asText());
        assertTrue(Files.exists(file), "report file must exist: " + file);
        assertTrue(Files.readString(file).contains("Student ID          : 1013"));

        MvcResult dl = mvc.perform(get("/api/exams/" + sid + "/report/download").header("X-Session-Token", tok))
                .andExpect(status().isOk()).andReturn();
        assertTrue(dl.getResponse().getHeader("Content-Disposition").contains("attachment"));
        assertEquals(text, dl.getResponse().getContentAsString().trim());
    }

    @Test
    void multipleLoginRuleAndRefreshResume() throws Exception {
        register(40, "P");
        JsonNode first = start(40, 1, "tab-A");
        String sid = first.get("sessionId").asText();
        // refresh of the SAME tab -> same session, no penalty
        JsonNode again = start(40, 1, "tab-A");
        assertTrue(again.get("resumed").asBoolean());
        assertEquals(sid, again.get("sessionId").asText());
        assertEquals(0, again.get("state").get("cheatScore").asInt());
        // a SECOND tab while the first is active -> MULTIPLE_LOGIN on both sessions (+4 each)
        JsonNode second = start(40, 1, "tab-B");
        assertTrue(second.get("multipleLoginRecorded").asBoolean());
        assertEquals(4, second.get("state").get("cheatScore").asInt());
        assertEquals(4, send(get("/api/exams/" + sid), null, again.get("sessionToken").asText(), 200).get("cheatScore").asInt());
        // repeating the same second-tab start does not score again
        start(40, 1, "tab-B");
        assertEquals(4, send(get("/api/exams/" + sid), null, again.get("sessionToken").asText(), 200).get("cheatScore").asInt());
    }

    @Test
    void eventRateLimitIsEnforced() throws Exception {
        register(50, "P");
        JsonNode s = start(50, 1, "tab-1");
        String sid = s.get("sessionId").asText(), tok = s.get("sessionToken").asText();
        for (int i = 0; i < 5; i++) event(sid, tok, newEventId(), "TAB_SWITCH", "BROWSER_EXTENSION", 200);   // limit is 5/min in test config
        event(sid, tok, newEventId(), "TAB_SWITCH", "BROWSER_EXTENSION", 429);
    }

    @Test
    void errorsUseOneConsistentJsonShape() throws Exception {
        JsonNode err = send(get("/api/exams/nope"), null, "tok", 404);
        assertEquals(404, err.get("status").asInt());
        assertNotNull(err.get("message"));
        assertNotNull(err.get("timestamp"));
    }
}
