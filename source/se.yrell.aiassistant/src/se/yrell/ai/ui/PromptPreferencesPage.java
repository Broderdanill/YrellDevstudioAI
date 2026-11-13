package se.yrell.ai.ui;
import java.util.*;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import se.yrell.ai.AIPlugin;
import se.yrell.ai.core.*;
/** Editable per-task templates, stored as JSON in Eclipse Preferences. */
public final class PromptPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
    private final List<TaskTemplates.Template> templates=new ArrayList<>();
    private Text common,name,prompt;private Combo selector;private Button delete,reset;
    private int current=-1;private boolean loading;
    public PromptPreferencesPage(){setPreferenceStore(AIPlugin.get().getPreferenceStore());
        setDescription("Edit the common instructions and task-specific templates. Changes apply to the next export.");}
    @Override public void init(IWorkbench workbench){}
    @Override protected Control createContents(Composite parent){
        Composite root=new Composite(parent,SWT.NONE);root.setLayout(new GridLayout(3,false));
        Label commonLabel=new Label(root,SWT.NONE);commonLabel.setText("Shared instructions");
        commonLabel.setLayoutData(new GridData(SWT.LEFT,SWT.TOP,false,false,3,1));
        common=multi(root,4,3);common.setToolTipText("Applied to every exported task");
        new Label(root,SWT.NONE).setText("Task template");
        selector=new Combo(root,SWT.READ_ONLY);selector.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        Button add=new Button(root,SWT.PUSH);add.setText("New...");add.addListener(SWT.Selection,e->createTemplate());
        new Label(root,SWT.NONE).setText("Name");
        name=new Text(root,SWT.BORDER);name.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        delete=new Button(root,SWT.PUSH);delete.setText("Delete");delete.addListener(SWT.Selection,e->deleteTemplate());
        Label detail=new Label(root,SWT.NONE);detail.setText("Prompt");
        detail.setLayoutData(new GridData(SWT.LEFT,SWT.TOP,false,false,3,1));
        prompt=multi(root,10,3);
        Label help=new Label(root,SWT.WRAP);help.setText("Variables: {{OBJECTS}}, {{SERVER}}, {{DATE}}, " +
          "{{USER_INSTRUCTIONS}}, {{CONTEXT_FILE}}, {{OUTPUT_DIR}}. " +
          "The content of a template is copied into each task folder at export time; changing Preferences does not alter earlier exports.");
        GridData h=new GridData(SWT.FILL,SWT.TOP,true,false,3,1);h.widthHint=580;help.setLayoutData(h);
        reset=new Button(root,SWT.PUSH);reset.setText("Restore selected built-in template");
        reset.setLayoutData(new GridData(SWT.LEFT,SWT.CENTER,false,false,3,1));
        reset.addListener(SWT.Selection,e->resetSelected());
        selector.addListener(SWT.Selection,e->switchTo(selector.getSelectionIndex()));
        values(false);return root;
    }
    private static Text multi(Composite root,int rows,int span){Text t=new Text(root,SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.V_SCROLL);
        GridData g=new GridData(SWT.FILL,SWT.FILL,true,false,span,1);g.heightHint=rows*20;g.widthHint=570;t.setLayoutData(g);return t;}
    private void values(boolean defaults){
        loading=true;templates.clear();var prefs=getPreferenceStore();
        try {templates.addAll(TaskTemplates.decode(defaults?prefs.getDefaultString(TaskTemplates.KEY):prefs.getString(TaskTemplates.KEY)));}
        catch(Exception e){templates.addAll(TaskTemplates.defaults());setMessage("Invalid prompt settings: loaded built-in defaults");}
        common.setText(defaults?prefs.getDefaultString(TaskTemplates.COMMON):prefs.getString(TaskTemplates.COMMON));
        current=-1;refresh(0);loading=false;
    }
    private void stash(){if(loading||current<0||current>=templates.size())return;
        TaskTemplates.Template old=templates.get(current);
        templates.set(current,new TaskTemplates.Template(old.id(),name.getText().strip(),prompt.getText()));}
    private void refresh(int index){loading=true;selector.removeAll();
        for(TaskTemplates.Template t:templates)selector.add(t.name()+" ["+t.id()+"]");
        if(index>=0&&index<templates.size()){
            current=index;selector.select(index);var t=templates.get(index);name.setText(t.name());prompt.setText(t.prompt());
        }else {current=-1;name.setText("");prompt.setText("");}
        boolean has=current>=0;name.setEnabled(has);prompt.setEnabled(has);delete.setEnabled(has);
        boolean builtin=has&&TaskTemplates.defaults().stream().anyMatch(t->t.id().equals(templates.get(current).id()));
        reset.setEnabled(builtin);loading=false;
    }
    private void switchTo(int next){stash();refresh(next);}
    private void createTemplate(){stash();InputDialog dlg=new InputDialog(getShell(),"New prompt template","Template name","My analysis",s->{
        if(s==null||s.strip().isEmpty())return "Enter a name";return null;});
        if(dlg.open()!=Window.OK)return;
        String name=dlg.getValue().strip(),base=TaskTemplates.slug(name),id=base;
        int n=2;while(exists(id))id=base+"-"+n++;
        templates.add(new TaskTemplates.Template(id,name,"Analyze the selected AR objects:\n{{OBJECTS}}\n{{USER_INSTRUCTIONS}}"));
        refresh(templates.size()-1);
    }
    private boolean exists(String id){for(var t:templates)if(t.id().equals(id))return true;return false;}
    private void deleteTemplate(){if(current<0)return;templates.remove(current);
        if(templates.isEmpty())templates.addAll(TaskTemplates.defaults());
        refresh(Math.min(current,templates.size()-1));}
    private void resetSelected(){if(current<0)return;stash();
        String id=templates.get(current).id();for(var d:TaskTemplates.defaults()){
            if(d.id().equals(id)){templates.set(current,d);refresh(current);return;}
        }
    }
    @Override public boolean performOk(){stash();try{
        for(var t:templates)if(t.name().isBlank())throw new IllegalArgumentException("Template name cannot be empty");
        getPreferenceStore().setValue(TaskTemplates.COMMON,common.getText());
        getPreferenceStore().setValue(TaskTemplates.KEY,TaskTemplates.encode(templates));
        setErrorMessage(null);return true;
    }catch(Exception e){setErrorMessage(e.getMessage());return false;}}
    @Override protected void performDefaults(){values(true);super.performDefaults();}
}
