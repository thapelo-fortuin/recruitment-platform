package com.recruitplatform.soc_service.rag;

import java.util.Collection;
import java.util.Map;

public class AnswerTemplates {

    public static String alertsByService(String service, Collection<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No alerts have been raised for " + service + " recently.";
        }
        return rows.size() + " alert(s) have been raised for " + service
                + ". The most recent is \"" + rows.iterator().next().get("ruleName")
                + "\" at " + rows.iterator().next().get("severity") + " severity.";
    }

    public static String highSeverityUsers(Collection<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No users are currently linked to HIGH or CRITICAL alerts.";
        }
        StringBuilder sb = new StringBuilder(rows.size() + " user(s) are linked to high-severity alerts: ");
        rows.forEach(r -> sb.append(r.get("userId")).append(" (").append(r.get("alertCount")).append("), "));
        return sb.substring(0, sb.length() - 2) + ".";
    }

    public static String controlsForAlert(Collection<Map<String, Object>> rows) {
        if (rows.isEmpty() || rows.iterator().next().get("threat") == null) {
            return "No threat or control information is linked to this alert.";
        }
        Map<String, Object> row = rows.iterator().next();
        return "This alert indicates \"" + row.get("threat") + "\" (MITRE " + row.get("mitreId")
                + "). Recommended controls: " + row.get("controls")
                + ". Documented response procedure(s): " + row.get("procedures") + ".";
    }

    public static String dependencyImpact(String service, Collection<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No other service currently depends on " + service + ".";
        }
        StringBuilder sb = new StringBuilder("If " + service + " fails, the following would be affected: ");
        rows.forEach(r -> sb.append(r.get("dependentService")).append(", "));
        return sb.substring(0, sb.length() - 2) + ".";
    }

    public static String threatsForService(String service, Collection<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No threats have been linked to alerts on " + service + " yet.";
        }
        StringBuilder sb = new StringBuilder("Threats observed against " + service + ": ");
        rows.forEach(r -> sb.append(r.get("threat")).append(" (MITRE ").append(r.get("mitreId")).append("), "));
        return sb.substring(0, sb.length() - 2) + ".";
    }

    public static String alertExplanation(Collection<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return "No matching alert was found.";
        }
        Map<String, Object> row = rows.iterator().next();
        return "Alert " + row.get("alertId") + " (" + row.get("ruleName") + ", " + row.get("severity")
                + ") was created because: " + row.get("description")
                + ". It indicates the threat(s) " + row.get("threats")
                + ", mitigated by " + row.get("controls")
                + ". Recommended action: " + row.get("recommendedAction") + ".";
    }
}