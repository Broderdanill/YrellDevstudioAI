package se.yrell.ai;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import se.yrell.ai.core.AISettings;
import se.yrell.ai.core.ExportSettings;

public final class AIPlugin extends AbstractUIPlugin {
    public static final String ID = "se.yrell.aiassistant";
    private static AIPlugin instance;
    public AIPlugin() { instance = this; }
    public static AIPlugin get() { return instance; }
    @Override public void start(BundleContext context) throws Exception { super.start(context); }
    @Override public void stop(BundleContext context) throws Exception { instance = null; super.stop(context); }
    @Override protected void initializeDefaultPreferences(IPreferenceStore prefs) { AISettings.defaults(prefs); ExportSettings.defaults(prefs); }
    public static void error(String label, Throwable cause) {
        if (instance != null) instance.getLog().log(new Status(IStatus.ERROR,ID,label,cause));
    }
}
