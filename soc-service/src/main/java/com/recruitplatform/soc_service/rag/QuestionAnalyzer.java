package com.recruitplatform.soc_service.rag;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuestionAnalyzer {

    private static final List<String> KNOWN_SERVICES = List.of(
            "api-gateway", "auth-service", "job-service", "application-service", "soc-service");

    private static final Pattern ALERT_ID_PATTERN = Pattern.compile("alt-[a-f0-9-]{10,}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

    public RagIntent analyse(String question) {
        String q = question.toLowerCase();

        boolean mentionsAlert = q.contains("alert");
        boolean mentionsWhy = q.contains("why") || q.contains("created") || q.contains("explain");
        boolean mentionsControl = q.contains("control") || q.contains("mitigat")
                || q.contains("respond") || q.contains("response") || q.contains("should be done")
                || q.contains("what should");
        boolean mentionsThreat = q.contains("threat");
        boolean mentionsDependency = q.contains("depend") || q.contains("impact")
                || (q.contains("fail") && q.contains("affect"));
        boolean mentionsHighSeverityUsers = q.contains("user")
                && (q.contains("high") || q.contains("severe") || q.contains("critical"));

        QuestionIntent intent;
        if (mentionsWhy && mentionsAlert) {
            intent = QuestionIntent.ALERT_EXPLANATION;
        } else if (mentionsControl) {
            intent = QuestionIntent.CONTROLS_FOR_ALERT;
        } else if (mentionsDependency) {
            intent = QuestionIntent.DEPENDENCY_IMPACT;
        } else if (mentionsThreat) {
            intent = QuestionIntent.THREATS_FOR_SERVICE;
        } else if (mentionsHighSeverityUsers) {
            intent = QuestionIntent.HIGH_SEVERITY_USERS;
        } else if (mentionsAlert) {
            intent = QuestionIntent.ALERTS_BY_SERVICE;
        } else {
            intent = QuestionIntent.UNKNOWN;
        }

        return new RagIntent(intent, extractService(q), extractAlertId(question), extractEmail(question));
    }

    private Optional<String> extractService(String q) {
        return KNOWN_SERVICES.stream().filter(q::contains).findFirst();
    }

    private Optional<String> extractAlertId(String question) {
        Matcher m = ALERT_ID_PATTERN.matcher(question);
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }

    private Optional<String> extractEmail(String question) {
        Matcher m = EMAIL_PATTERN.matcher(question);
        return m.find() ? Optional.of(m.group()) : Optional.empty();
    }
}