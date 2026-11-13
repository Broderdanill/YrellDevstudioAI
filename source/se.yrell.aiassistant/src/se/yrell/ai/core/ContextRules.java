package se.yrell.ai.core;

import java.util.regex.Pattern;
/** Guardrails applied to the exact context previewed and sent. */
public final class ContextRules {
    private ContextRules() {}
    private static final Pattern SECRET=Pattern.compile("(?i)\\b(password|passwd|authorization|api[_ -]?key|access[_ -]?token|client[_ -]?secret|bearer)\\b\\s*[:=]\\s*(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s;,]+)");
    private static final Pattern URL_CREDENTIALS=Pattern.compile("(?i)(https?://)[^\\s/@:]+:[^\\s/@]+@");
    public static String sanitize(String value,boolean redact) {
        if(value==null)return "";
        if(!redact)return value;
        String output=URL_CREDENTIALS.matcher(value).replaceAll("$1[REDACTED]@");
        return SECRET.matcher(output).replaceAll("$1=[REDACTED]");
    }
    public static String limit(String s,int max) {
        if(s==null)return "";
        if(s.length()<=max)return s;
        String marker="\n[TRUNCATED: context size limit reached]\n";
        return s.substring(0,Math.max(0,max-marker.length()))+marker;
    }
}
