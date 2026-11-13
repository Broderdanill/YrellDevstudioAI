package se.yrell.ai.ui;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import se.yrell.ai.AIPlugin;
import se.yrell.ai.core.AISettings;
/** Settings applied when creating the next read-only snapshot. */
public final class ContextPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {
    private Button related,fields,redact,review;
    private Text maxObjects,maxRelated,maxFields,maxActions,maxChars;
    public ContextPreferencesPage(){setPreferenceStore(AIPlugin.get().getPreferenceStore());
        setDescription("Select how much SAVED AR definition data is exported. Limits and redaction are applied before writing.");}
    @Override public void init(IWorkbench workbench){}
    @Override protected Control createContents(Composite parent){
        Composite root=new Composite(parent,SWT.NONE);root.setLayout(new GridLayout(2,false));
        related=check(root,"Also load directly related Active Links and Filters for selected forms");
        fields=check(root,"Include fields for selected forms");
        redact=check(root,"Redact common credential patterns in generated context");
        review=check(root,"Preview the exact context before exporting");
        maxObjects=number(root,"Maximum total objects (1–100)");
        maxRelated=number(root,"Maximum related workflow (0–100)");
        maxFields=number(root,"Maximum fields per form (0–5000)");
        maxActions=number(root,"Maximum actions per If/Else branch (1–1000)");
        maxChars=number(root,"Maximum context characters (1000–1000000)");
        Label note=new Label(root,SWT.WRAP);note.setText("Selected objects are read from the connected server's SAVED definitions. " +
            "Unsaved editor changes and live record values are not included. " +
            "Automatic related-object discovery is best-effort; verify the object list and the exported manifest. " +
            "Redaction cannot guarantee removal of every secret.");
        GridData g=new GridData(SWT.FILL,SWT.TOP,true,false,2,1);g.widthHint=520;note.setLayoutData(g);
        values(false);return root;
    }
    private static Button check(Composite root,String title){Button b=new Button(root,SWT.CHECK);b.setText(title);
        b.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false,2,1));return b;}
    private static Text number(Composite root,String title){new Label(root,SWT.NONE).setText(title);
        Text t=new Text(root,SWT.SINGLE|SWT.BORDER);t.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));return t;}
    private void values(boolean defaults){IPreferenceStore p=getPreferenceStore();
        related.setSelection(defaults?p.getDefaultBoolean(AISettings.RELATED):p.getBoolean(AISettings.RELATED));
        fields.setSelection(defaults?p.getDefaultBoolean(AISettings.FIELDS):p.getBoolean(AISettings.FIELDS));
        redact.setSelection(defaults?p.getDefaultBoolean(AISettings.REDACT):p.getBoolean(AISettings.REDACT));
        review.setSelection(defaults?p.getDefaultBoolean(AISettings.REVIEW):p.getBoolean(AISettings.REVIEW));
        maxObjects.setText(String.valueOf(defaults?p.getDefaultInt(AISettings.MAX_OBJECTS):p.getInt(AISettings.MAX_OBJECTS)));
        maxRelated.setText(String.valueOf(defaults?p.getDefaultInt(AISettings.MAX_RELATED):p.getInt(AISettings.MAX_RELATED)));
        maxFields.setText(String.valueOf(defaults?p.getDefaultInt(AISettings.MAX_FIELDS):p.getInt(AISettings.MAX_FIELDS)));
        maxActions.setText(String.valueOf(defaults?p.getDefaultInt(AISettings.MAX_ACTIONS):p.getInt(AISettings.MAX_ACTIONS)));
        maxChars.setText(String.valueOf(defaults?p.getDefaultInt(AISettings.MAX_CHARS):p.getInt(AISettings.MAX_CHARS)));
    }
    @Override public boolean performOk(){try{
        int mo=read(maxObjects,1,100),mr=read(maxRelated,0,100),mf=read(maxFields,0,5000),ma=read(maxActions,1,1000),mc=read(maxChars,1000,1000000);
        IPreferenceStore p=getPreferenceStore();p.setValue(AISettings.RELATED,related.getSelection());
        p.setValue(AISettings.FIELDS,fields.getSelection());p.setValue(AISettings.REDACT,redact.getSelection());
        p.setValue(AISettings.REVIEW,review.getSelection());p.setValue(AISettings.MAX_OBJECTS,mo);
        p.setValue(AISettings.MAX_RELATED,mr);p.setValue(AISettings.MAX_FIELDS,mf);
        p.setValue(AISettings.MAX_ACTIONS,ma);p.setValue(AISettings.MAX_CHARS,mc);
        setErrorMessage(null);return true;
    }catch(Exception ex){setErrorMessage(ex.getMessage());return false;}}
    private static int read(Text t,int min,int max){try{int n=Integer.parseInt(t.getText().strip());
        if(n<min||n>max)throw new NumberFormatException();return n;
    }catch(Exception ex){throw new IllegalArgumentException("Limit must be "+min+"–"+max);}}
    @Override protected void performDefaults(){values(true);super.performDefaults();}
}
