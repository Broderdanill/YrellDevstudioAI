package se.yrell.ai.ui;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;
import com.bmc.arsys.studio.model.item.IModelItem;
import se.yrell.ai.AIPlugin;
import se.yrell.ai.bmc.ContextCollector;
import se.yrell.ai.core.*;
/** Dockable, local-only task exporter. No AI API calls and no remote service. */
public final class AIView extends ViewPart {
    public static final String ID="se.yrell.ai.view";
    private final List<IModelItem> attached=new ArrayList<>();
    private final List<Path> nativeDefs=new ArrayList<>();
    private final AtomicInteger generation=new AtomicInteger();
    private org.eclipse.swt.widgets.List objects,defs;
    private Combo task;private Text notes;private Label status;
    private Button exportButton,remove,clear,preview;
    private volatile Job currentJob;private boolean closed;
    @Override public void createPartControl(Composite parent){
        Composite root=new Composite(parent,SWT.NONE);
        GridLayout layout=new GridLayout(1,false);layout.marginWidth=8;layout.marginHeight=8;layout.verticalSpacing=7;
        root.setLayout(layout);
        Composite header=new Composite(root,SWT.NONE);header.setLayout(new GridLayout(2,false));
        header.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        Label title=new Label(header,SWT.NONE);title.setText("Yrell AI Task Exporter");
        title.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        button(header,"Preferences...",()->openPreferences("se.yrell.ai.preferences"));
        Group selection=new Group(root,SWT.NONE);selection.setText("Selected saved AR definitions");
        selection.setLayout(new GridLayout(1,false));selection.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
        objects=new org.eclipse.swt.widgets.List(selection,SWT.MULTI|SWT.BORDER|SWT.V_SCROLL);
        GridData ol=new GridData(SWT.FILL,SWT.FILL,true,true);ol.heightHint=110;objects.setLayoutData(ol);
        Composite actions=buttons(selection,4);
        button(actions,"Add Object List selection",this::addCurrentSelection);
        remove=button(actions,"Remove selected",this::removeSelected);
        clear=button(actions,"Clear",()->{attached.clear();updateObjects();});
        preview=button(actions,"Preview context",()->prepare(true));
        Group taskGroup=new Group(root,SWT.NONE);taskGroup.setText("Task");
        taskGroup.setLayout(new GridLayout(2,false));taskGroup.setLayoutData(new GridData(SWT.FILL,SWT.TOP,true,false));
        new Label(taskGroup,SWT.NONE).setText("Purpose");
        task=new Combo(taskGroup,SWT.READ_ONLY);task.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        button(taskGroup,"Edit prompt templates...",()->openPreferences("se.yrell.ai.templates.preferences"));
        Label message=new Label(taskGroup,SWT.WRAP);
        message.setText("Templates are loaded from Preferences at export time. No server or API key required.");
        message.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        Label instruction=new Label(root,SWT.NONE);instruction.setText("Additional instructions (optional)");
        notes=new Text(root,SWT.BORDER|SWT.MULTI|SWT.WRAP|SWT.V_SCROLL);
        GridData ng=new GridData(SWT.FILL,SWT.FILL,true,false);ng.heightHint=72;notes.setLayoutData(ng);
        Group nativeGroup=new Group(root,SWT.NONE);nativeGroup.setText("Native .def files (optional)");
        nativeGroup.setLayout(new GridLayout(1,false));nativeGroup.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
        defs=new org.eclipse.swt.widgets.List(nativeGroup,SWT.BORDER|SWT.MULTI|SWT.V_SCROLL);
        GridData dl=new GridData(SWT.FILL,SWT.FILL,true,false);dl.heightHint=48;defs.setLayoutData(dl);
        Composite nativeButtons=buttons(nativeGroup,2);
        button(nativeButtons,"Add existing .def...",this::chooseDefs);
        button(nativeButtons,"Remove selected .def",this::removeDefs);
        Composite footer=buttons(root,2);
        exportButton=button(footer,"Export task for OpenCode",()->prepare(false));
        button(footer,"Context / Export settings...",()->openPreferences("se.yrell.ai.context.preferences"));
        status=new Label(root,SWT.WRAP);status.setText("Select saved objects in Developer Studio Object List, then right-click and choose 'Export with Yrell AI'.");
        GridData sg=new GridData(SWT.FILL,SWT.CENTER,true,false);sg.widthHint=500;status.setLayoutData(sg);
        refreshTemplates();updateObjects();
    }
    private static Composite buttons(Composite p,int count){Composite c=new Composite(p,SWT.NONE);
        GridLayout l=new GridLayout(count,false);l.marginWidth=0;l.marginHeight=0;c.setLayout(l);
        c.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));return c;}
    private static Button button(Composite p,String label,Runnable action){Button b=new Button(p,SWT.PUSH);b.setText(label);
        b.addListener(SWT.Selection,e->action.run());return b;}
    private void openPreferences(String id){PreferencesUtil.createPreferenceDialogOn(getSite().getShell(),id,
        new String[]{"se.yrell.ai.preferences","se.yrell.ai.context.preferences","se.yrell.ai.templates.preferences"},null).open();
        refreshTemplates();}
    private void refreshTemplates(){if(task==null||task.isDisposed())return;
        String previous=task.getText();task.removeAll();
        try{for(var t:TaskTemplates.decode(AIPlugin.get().getPreferenceStore().getString(TaskTemplates.KEY)))task.add(t.name()+" ["+t.id()+"]");}
        catch(Exception ex){status.setText("Prompt template error: "+ex.getMessage());return;}
        int existing=task.indexOf(previous);task.select(existing>=0?existing:0);
    }
    public void attach(List<IModelItem> selections){
        if(closed||objects==null||objects.isDisposed()||selections==null||selections.isEmpty())return;
        if(!attached.isEmpty()&&attached.get(0).getStore()!=selections.get(0).getStore()){
            if(!MessageDialog.openConfirm(getSite().getShell(),"Different AR server","Replace existing selection from another AR connection?"))return;
            attached.clear();
        }
        for(IModelItem item:selections){
            if(item==null||item.getStore()==null||item.getName()==null)continue;
            if(!attached.isEmpty()&&item.getStore()!=attached.get(0).getStore())continue;
            boolean exists=false;
            for(IModelItem prior:attached){if(prior.getStore()==item.getStore()&&
                    prior.getItemType().getTypeName().equals(item.getItemType().getTypeName())&&
                    prior.getName().equals(item.getName())){exists=true;break;}}
            if(!exists)attached.add(item);
        }
        updateObjects();
    }
    private void addCurrentSelection(){
        var selected=getSite().getWorkbenchWindow().getSelectionService().getSelection();
        if(selected instanceof IStructuredSelection list&&!list.isEmpty()){
            List<IModelItem> items=new ArrayList<>();
            for(Object item:list.toArray())if(item instanceof IModelItem m)items.add(m);
            if(!items.isEmpty()){attach(items);return;}
        }
        MessageDialog.openInformation(getSite().getShell(),"Yrell AI","Select AR objects in Object List and use the right-click action.");
    }
    private void removeSelected(){int[] ids=objects.getSelectionIndices();
        for(int i=ids.length-1;i>=0;i--)attached.remove(ids[i]);updateObjects();}
    private void updateObjects(){if(objects==null||objects.isDisposed())return;
        generation.incrementAndGet();Job old=currentJob;if(old!=null)old.cancel();
        objects.removeAll();for(IModelItem i:attached)objects.add(i.getItemType().getTypeName()+" · "+i.getName());
        boolean has=!attached.isEmpty();exportButton.setEnabled(has);remove.setEnabled(has);preview.setEnabled(has);clear.setEnabled(has);
        if(status!=null&&!status.isDisposed())status.setText(has?attached.size()+" selected saved definition(s)":"No AR objects selected");
    }
    private void chooseDefs(){FileDialog dlg=new FileDialog(getSite().getShell(),SWT.OPEN|SWT.MULTI);
        dlg.setText("Select existing BMC .def exports");dlg.setFilterExtensions(new String[]{"*.def","*.*"});
        String picked=dlg.open();if(picked==null)return;
        for(String name:dlg.getFileNames()){
            Path p=Path.of(dlg.getFilterPath(),name).toAbsolutePath().normalize();
            if(!p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".def"))continue;
            if(!nativeDefs.contains(p)){nativeDefs.add(p);defs.add(p.toString());}
        }
    }
    private void removeDefs(){int[] indexes=defs.getSelectionIndices();
        for(int i=indexes.length-1;i>=0;i--){nativeDefs.remove(indexes[i]);defs.remove(indexes[i]);}}
    private void prepare(boolean previewOnly){
        if(attached.isEmpty())return;
        var preferences=AIPlugin.get().getPreferenceStore();
        AISettings context=AISettings.from(preferences);ExportSettings exportSettings;
        List<TaskTemplates.Template> templates;
        try {exportSettings=ExportSettings.from(preferences);templates=TaskTemplates.decode(preferences.getString(TaskTemplates.KEY));}
        catch(Exception ex){MessageDialog.openError(getSite().getShell(),"Yrell AI","Settings error: "+ex.getMessage());return;}
        int idx=task.getSelectionIndex();if(idx<0||idx>=templates.size()){
            MessageDialog.openError(getSite().getShell(),"Yrell AI","Choose a task template first");return;}
        TaskTemplates.Template selected=templates.get(idx);
        String userNotes=notes.getText();List<IModelItem> items=List.copyOf(attached);
        List<Path> defFiles=List.copyOf(nativeDefs);
        int request=generation.incrementAndGet();Job previous=currentJob;if(previous!=null)previous.cancel();
        status.setText("Reading saved definitions...");
        Job job=new Job("Yrell AI: prepare local export"){
            @Override protected IStatus run(IProgressMonitor monitor){try{
                ContextCollector.Snapshot snap=ContextCollector.collect(items,context,monitor);
                if(monitor.isCanceled())return Status.CANCEL_STATUS;
                if(snap.objects()==0)throw new IllegalStateException("No object definitions could be read. Check the connection and permissions.");
                Display.getDefault().asyncExec(()->{
                    if(!current(request))return;
                    if(previewOnly||context.reviewBeforeExport()){
                        int result=new ContextPreviewDialog(getSite().getShell(),snap.text(),!previewOnly).open();
                        if(previewOnly){status.setText("Context previewed; nothing exported.");return;}
                        if(result!=org.eclipse.jface.window.Window.OK){status.setText("Export canceled.");return;}
                    }
                    writeTask(request,exportSettings,selected,items,defFiles,snap,userNotes);
                });
                return Status.OK_STATUS;
            }catch(Exception ex){showError(request,"Context collection failed",ex);
                return new Status(IStatus.ERROR,AIPlugin.ID,"Context collection failed",ex);}
        }};job.setUser(true);currentJob=job;job.schedule();
    }
    private void writeTask(int request,ExportSettings settings,TaskTemplates.Template selected,
        List<IModelItem> items,List<Path> defFiles,ContextCollector.Snapshot snap,String instructions){
        status.setText("Writing local task folder...");
        final String server=items.get(0).getStore().getName();
        List<String> labels=new ArrayList<>();
        for(IModelItem i:items)labels.add("["+i.getItemType().getTypeName()+": "+i.getName()+"]");
        Job job=new Job("Yrell AI: write OpenCode project"){
            @Override protected IStatus run(IProgressMonitor m){try{
                Path result=TaskExporter.export(new TaskExporter.Request(settings.directory(),server,labels,
                    snap.text(),selected,settings.commonInstructions(),instructions,
                    settings.makeCommand(),defFiles,snap.notes()));
                Display.getDefault().asyncExec(()->{if(current(request)){
                    status.setText("Exported: "+result);
                    MessageDialog.openInformation(getSite().getShell(),"Yrell AI – export complete",
                       "Task written to:\n"+result+"\n\nOpen this directory in OpenCode and run /yrell-task. " +
                       "Exported "+snap.objects()+" definition summaries and "+defFiles.size()+" existing .def file(s).");
                }});
                return Status.OK_STATUS;
            }catch(Exception ex){showError(request,"Export failed",ex);
                return new Status(IStatus.ERROR,AIPlugin.ID,"Task export failed",ex);}
        }};job.setUser(true);currentJob=job;job.schedule();
    }
    private boolean current(int id){return !closed&&id==generation.get()&&status!=null&&!status.isDisposed();}
    private void showError(int id,String title,Exception e){AIPlugin.error(title,e);
        Display.getDefault().asyncExec(()->{if(current(id)){
            status.setText(title+": "+e.getMessage());
            MessageDialog.openError(getSite().getShell(),"Yrell AI",title+"\n"+e.getMessage());
        }});}
    @Override public void setFocus(){if(objects!=null&&!objects.isDisposed())objects.setFocus();}
    @Override public void dispose(){closed=true;Job job=currentJob;if(job!=null)job.cancel();super.dispose();}
}
