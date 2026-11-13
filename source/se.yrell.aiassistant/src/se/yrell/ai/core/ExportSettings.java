package se.yrell.ai.core;
import org.eclipse.jface.preference.IPreferenceStore;
import java.nio.file.Path;
public record ExportSettings(Path directory,boolean makeCommand,String commonInstructions) {
    public static final String DIRECTORY="exportDirectory",COMMAND="makeOpenCodeCommand";
    public static void defaults(IPreferenceStore store){
        store.setDefault(DIRECTORY,Path.of(System.getProperty("user.home"),"YrellAI").toString());
        store.setDefault(COMMAND,true);store.setDefault(TaskTemplates.COMMON,TaskTemplates.DEFAULT_COMMON);
        store.setDefault(TaskTemplates.KEY,TaskTemplates.encode(TaskTemplates.defaults()));
    }
    public static ExportSettings from(IPreferenceStore p){
        String path=p.getString(DIRECTORY).strip();
        if(path.isEmpty())throw new IllegalArgumentException("Export directory is empty");
        return new ExportSettings(Path.of(path).toAbsolutePath().normalize(),p.getBoolean(COMMAND),p.getString(TaskTemplates.COMMON));
    }
}
