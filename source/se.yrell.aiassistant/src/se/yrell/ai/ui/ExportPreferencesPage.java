package se.yrell.ai.ui;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import se.yrell.ai.AIPlugin;
import se.yrell.ai.core.ExportSettings;
import java.nio.file.Path;
/** Local-only preferences; no model, API keys or network connections. */
public final class ExportPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
    private Text directory;private Button command;
    public ExportPreferencesPage(){setPreferenceStore(AIPlugin.get().getPreferenceStore());
        setDescription("Export selected AR definitions and prompts for local use in OpenCode. No AI server is required.");}
    @Override public void init(IWorkbench workbench){}
    @Override protected Control createContents(Composite parent){
        Composite p=new Composite(parent,SWT.NONE);p.setLayout(new GridLayout(3,false));
        new Label(p,SWT.NONE).setText("Export directory");
        directory=new Text(p,SWT.BORDER);directory.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        Button browse=new Button(p,SWT.PUSH);browse.setText("Browse...");
        browse.addListener(SWT.Selection,e->{DirectoryDialog dlg=new DirectoryDialog(getShell());
            dlg.setText("Select local task export directory");String old=directory.getText().strip();
            if(!old.isEmpty())dlg.setFilterPath(old);
            String chosen=dlg.open();if(chosen!=null)directory.setText(chosen);});
        command=new Button(p,SWT.CHECK);command.setText("Generate .opencode/commands/yrell-task.md");
        command.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false,3,1));
        Label help=new Label(p,SWT.WRAP);help.setText("The plugin saves snapshots of selected, saved Developer Studio objects " +
            "alongside editable prompt instructions. Run OpenCode in the generated task folder. " +
            "Native .def files can be attached from an existing Developer Studio export. " +
            "No requests are sent to an AI endpoint by this plugin.");
        GridData gd=new GridData(SWT.FILL,SWT.TOP,true,false,3,1);gd.widthHint=520;help.setLayoutData(gd);
        values(false);return p;
    }
    private void values(boolean defaults){IPreferenceStore s=getPreferenceStore();
        directory.setText(defaults?s.getDefaultString(ExportSettings.DIRECTORY):s.getString(ExportSettings.DIRECTORY));
        command.setSelection(defaults?s.getDefaultBoolean(ExportSettings.COMMAND):s.getBoolean(ExportSettings.COMMAND));}
    @Override public boolean performOk(){
        try {
            String value=directory.getText().strip();if(value.isEmpty())throw new IllegalArgumentException("Choose an export folder");
            Path path=Path.of(value).toAbsolutePath().normalize();
            if(path.getParent()==null)throw new IllegalArgumentException("Choose a directory below a filesystem root");
            getPreferenceStore().setValue(ExportSettings.DIRECTORY,path.toString());
            getPreferenceStore().setValue(ExportSettings.COMMAND,command.getSelection());
            setErrorMessage(null);return true;
        }catch(Exception ex){setErrorMessage(ex.getMessage());return false;}
    }
    @Override protected void performDefaults(){values(true);super.performDefaults();}
}
