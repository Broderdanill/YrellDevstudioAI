package se.yrell.ai.core;
import org.eclipse.jface.preference.IPreferenceStore;
/** Context limits. No endpoint, model or credential is used or needed. */
public record AISettings(boolean includeRelated, boolean includeFormFields,
        int maxObjects, int maxRelated, int maxFields, int maxActions, int maxChars,
        boolean redact, boolean reviewBeforeExport) {
    public static final String RELATED="includeRelated",FIELDS="includeFields",MAX_OBJECTS="maxObjects",
        MAX_RELATED="maxRelated",MAX_FIELDS="maxFields",MAX_ACTIONS="maxActions",MAX_CHARS="maxChars",
        REDACT="redact",REVIEW="reviewBeforeExport";
    public static void defaults(IPreferenceStore p) {
        p.setDefault(RELATED,false);p.setDefault(FIELDS,true);
        p.setDefault(MAX_OBJECTS,20);p.setDefault(MAX_RELATED,12);p.setDefault(MAX_FIELDS,300);
        p.setDefault(MAX_ACTIONS,100);p.setDefault(MAX_CHARS,200000);
        p.setDefault(REDACT,true);p.setDefault(REVIEW,true);
    }
    public static AISettings from(IPreferenceStore p) {
        return new AISettings(p.getBoolean(RELATED),p.getBoolean(FIELDS),
            clamp(p.getInt(MAX_OBJECTS),1,100),clamp(p.getInt(MAX_RELATED),0,100),
            clamp(p.getInt(MAX_FIELDS),0,5000),clamp(p.getInt(MAX_ACTIONS),1,1000),
            clamp(p.getInt(MAX_CHARS),1000,1000000),p.getBoolean(REDACT),p.getBoolean(REVIEW));
    }
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
}
