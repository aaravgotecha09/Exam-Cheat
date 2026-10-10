import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// One exam attempt = one ExamMonitor + login state
class ExamSession {
    final String id = UUID.randomUUID().toString().substring(0, 8);
    final ExamMonitor monitor;
    boolean submitted = false;
    final String startedAt = ExamServer.now();

    ExamSession(ExamMonitor monitor) {
        this.monitor = monitor;
    }
}

// BACK END: REST API + static file server, built on the OOP model in Model.java
public class ExamServer {
    static final Map<String, ExamSession> sessions = new LinkedHashMap<>();
    static final Path WEB = Paths.get("web");

    static String now() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // ---------- tiny JSON helpers (no external libraries) ----------
    static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': break;
                case '\t': b.append("\\t"); break;
                default: b.append(c);
            }
        }
        return b.toString();
    }

    static String q(String s) {
        return "\"" + esc(s) + "\"";
    }

    // reads a value from a flat JSON object: {"a":"x","b":5}
    static String field(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|[-\\w.]+)").matcher(json);
        if (!m.find()) return "";
        return m.group(2) != null ? m.group(2).replace("\\\"", "\"").replace("\\\\", "\\") : m.group(1);
    }

    static String body(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    static void json(HttpExchange ex, int code, String j) throws IOException {
        send(ex, code, "application/json; charset=utf-8", j.getBytes(StandardCharsets.UTF_8));
    }

    static void error(HttpExchange ex, int code, String msg) throws IOException {
        json(ex, code, "{\"error\":" + q(msg) + "}");
    }

    // ---------- JSON builders for model objects ----------
    static String statusJson(ExamSession s) {
        ExamMonitor m = s.monitor;
        StringBuilder ev = new StringBuilder("[");
        List<CheatEvent> list = m.getEvents();
        for (int i = 0; i < list.size(); i++) {
            CheatEvent e = list.get(i);
            if (i > 0) ev.append(",");
            ev.append("{\"name\":").append(q(e.getEventName()))
              .append(",\"time\":").append(q(e.getTimestamp()))
              .append(",\"points\":").append(e.calculateScore()).append("}");
        }
        ev.append("]");
        return "{\"sessionId\":" + q(s.id)
                + ",\"score\":" + m.calculateScore()
                + ",\"risk\":" + q(m.classifyRisk())
                + ",\"attempted\":" + m.getQuestionsAttempted()
                + ",\"totalQuestions\":" + m.getPaper().getQuestionCount()
                + ",\"submitted\":" + s.submitted
                + ",\"events\":" + ev + "}";
    }

    static String summaryJson(ExamSession s) {
        ExamMonitor m = s.monitor;
        Student st = m.getStudent();
        return "{\"sessionId\":" + q(s.id)
                + ",\"studentId\":" + st.getStudentId()
                + ",\"name\":" + q(st.getName())
                + ",\"exam\":" + q(st.getExamName())
                + ",\"startedAt\":" + q(s.startedAt)
                + ",\"attempted\":" + m.getQuestionsAttempted()
                + ",\"marks\":" + m.getMarksObtained()
                + ",\"totalMarks\":" + m.getPaper().getTotalMarks()
                + ",\"score\":" + m.calculateScore()
                + ",\"risk\":" + q(m.classifyRisk())
                + ",\"submitted\":" + s.submitted + "}";
    }

    static String questionsJson(TestPaper p) {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < p.getQuestionCount(); i++) {
            Question qu = p.getQuestion(i);
            if (i > 0) b.append(",");
            b.append("{\"text\":").append(q(qu.getText())).append(",\"marks\":").append(qu.getMarks()).append(",\"options\":[");
            String[] o = qu.getOptions();
            for (int j = 0; j < o.length; j++) {
                if (j > 0) b.append(",");
                b.append(q(o[j]));
            }
            b.append("]}");
        }
        return b.append("]").toString();
    }

    static CheatEvent makeEvent(String type) {
        String t = now();
        switch (type) {
            case "tab": return new TabSwitchEvent(t);
            case "copy": return new CopyPasteEvent(t);
            case "login": return new MultipleLoginEvent(t);
            case "exit": return new WindowExitEvent(t);
            case "person": return new PersonDetectionEvent(t);
            default: return null;
        }
    }

    // ---------- API handler ----------
    static void api(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();
        String method = ex.getRequestMethod();
        try {
            synchronized (sessions) {
                if (path.equals("/api/papers") && method.equals("GET")) {
                    String[] n = PaperBank.paperNames();
                    StringBuilder b = new StringBuilder("[");
                    for (int i = 0; i < n.length; i++) {
                        TestPaper p = PaperBank.getPaper(i + 1);
                        if (i > 0) b.append(",");
                        b.append("{\"id\":").append(i + 1).append(",\"name\":").append(q(n[i]))
                         .append(",\"questions\":").append(p.getQuestionCount())
                         .append(",\"marks\":").append(p.getTotalMarks()).append("}");
                    }
                    json(ex, 200, b.append("]").toString());
                    return;
                }

                if (path.equals("/api/start") && method.equals("POST")) {
                    String b = body(ex);
                    int id, paperChoice;
                    try {
                        id = Integer.parseInt(field(b, "studentId").trim());
                        paperChoice = Integer.parseInt(field(b, "paper").trim());
                    } catch (NumberFormatException e) {
                        error(ex, 400, "Student ID and paper must be numbers.");
                        return;
                    }
                    String name = field(b, "name").trim();
                    if (name.isEmpty()) { error(ex, 400, "Name is required."); return; }
                    if (paperChoice < 1 || paperChoice > 3) {
                        error(ex, 400, new InvalidChoiceException("Paper choice must be between 1 and 3.").getMessage());
                        return;
                    }
                    // REAL multiple-login detection: same student ID already has an active exam
                    boolean dup = false;
                    for (ExamSession old : sessions.values()) {
                        if (old.monitor.getStudent().getStudentId() == id && !old.submitted) {
                            old.monitor.recordEvent(new MultipleLoginEvent(now()));
                            dup = true;
                        }
                    }
                    TestPaper paper = PaperBank.getPaper(paperChoice);
                    ExamSession s = new ExamSession(new ExamMonitor(new Student(id, name, paper.getPaperName()), paper));
                    if (dup) s.monitor.recordEvent(new MultipleLoginEvent(now()));
                    sessions.put(s.id, s);
                    json(ex, 200, "{\"sessionId\":" + q(s.id) + ",\"paperName\":" + q(paper.getPaperName())
                            + ",\"totalMarks\":" + paper.getTotalMarks()
                            + ",\"duplicateLogin\":" + dup
                            + ",\"questions\":" + questionsJson(paper) + "}");
                    return;
                }

                if (path.equals("/api/admin") && method.equals("GET")) {
                    StringBuilder b = new StringBuilder("[");
                    boolean first = true;
                    for (ExamSession s : sessions.values()) {
                        if (!first) b.append(",");
                        first = false;
                        b.append(summaryJson(s));
                    }
                    json(ex, 200, b.append("]").toString());
                    return;
                }

                // everything below needs a session
                String sid = "";
                if (query != null) {
                    for (String kv : query.split("&")) {
                        if (kv.startsWith("sessionId=")) sid = URLDecoder.decode(kv.substring(10), "UTF-8");
                    }
                }
                String b = method.equals("POST") ? body(ex) : "";
                if (sid.isEmpty()) sid = field(b, "sessionId");
                ExamSession s = sessions.get(sid);
                if (s == null) { error(ex, 404, "Unknown session."); return; }

                if (path.equals("/api/status")) {
                    json(ex, 200, statusJson(s));
                } else if (path.equals("/api/event") && method.equals("POST")) {
                    if (s.submitted) { error(ex, 409, "Exam already submitted."); return; }
                    CheatEvent e = makeEvent(field(b, "type"));
                    if (e == null) { error(ex, 400, "Unknown event type."); return; }
                    s.monitor.recordEvent(e);
                    json(ex, 200, statusJson(s));
                } else if (path.equals("/api/answer") && method.equals("POST")) {
                    if (s.submitted) { error(ex, 409, "Exam already submitted."); return; }
                    String a = field(b, "answer").trim().toUpperCase();
                    int index = Integer.parseInt(field(b, "index").trim());
                    if (a.length() != 1 || a.charAt(0) < 'A' || a.charAt(0) > 'D') {
                        error(ex, 400, new InvalidChoiceException("Answer must be A, B, C or D.").getMessage());
                        return;
                    }
                    if (index != s.monitor.getCurrentNumber() - 1 || !s.monitor.hasMoreQuestions()) {
                        error(ex, 409, "That question was already answered.");
                        return;
                    }
                    s.monitor.submitAnswer(a.charAt(0));
                    json(ex, 200, statusJson(s));
                } else if (path.equals("/api/submit") && method.equals("POST")) {
                    if (!s.submitted) {
                        s.submitted = true;
                        new Report(s.monitor).saveToFile("ExamReport.txt");   // file handling
                    }
                    json(ex, 200, "{\"summary\":" + summaryJson(s) + ",\"status\":" + statusJson(s)
                            + ",\"report\":" + q(new Report(s.monitor).generateReport()) + "}");
                } else if (path.equals("/api/report")) {
                    json(ex, 200, "{\"report\":" + q(new Report(s.monitor).generateReport()) + "}");
                } else {
                    error(ex, 404, "Not found.");
                }
            }
        } catch (NumberFormatException e) {
            error(ex, 400, "Please enter a valid number.");
        } catch (Exception e) {
            error(ex, 500, "Server error: " + e);
        }
    }

    // ---------- static files (front end) ----------
    static void statics(HttpExchange ex) throws IOException {
        String p = ex.getRequestURI().getPath();
        if (p.equals("/")) p = "/index.html";
        Path f = WEB.resolve(p.substring(1)).normalize();
        if (!f.startsWith(WEB) || !Files.isRegularFile(f)) {
            send(ex, 404, "text/plain", "Not found".getBytes());
            return;
        }
        String type = p.endsWith(".html") ? "text/html; charset=utf-8"
                : p.endsWith(".css") ? "text/css" : p.endsWith(".js") ? "application/javascript" : "application/octet-stream";
        send(ex, 200, type, Files.readAllBytes(f));
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", ExamServer::api);
        server.createContext("/", ExamServer::statics);
        server.start();
        System.out.println("SHREYA BHUIA 70412500089 MBA TECH IT I013");
        System.out.println("AARAV GOTECHA 70412500088 MBA TECH IT I019");
        System.out.println("\n***** EXAM CHEAT DETECTION SYSTEM (web) *****");
        System.out.println("Open http://localhost:" + port + " in your browser. Press Ctrl+C to stop.");
    }
}
