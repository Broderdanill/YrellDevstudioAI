package se.yrell.ai.bmc;

import java.util.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.bmc.arsys.studio.model.item.IModelItem;
import com.bmc.arsys.studio.ui.views.objectlist.actions.BaseObjectListAction;
import com.bmc.arsys.studio.ui.views.objectlist.actions.ObjectListActionProvider;
import com.bmc.arsys.studio.ui.views.objectlist.actions.ObjectListActionService;
import se.yrell.ai.AIPlugin;
import se.yrell.ai.ui.AIView;

/** BMC's own Object List menu hook, supports multi-selection and no AR calls on SWT UI thread. */
public final class AIObjectListActionProvider extends ObjectListActionProvider {
    @Override protected void doFillContextMenu(IMenuManager menu){
        if(!(getSelection() instanceof IStructuredSelection selection)||selection.isEmpty())return;
        List<IModelItem> items=new ArrayList<>();
        for(Object item:selection.toArray()) {
            if(!(item instanceof IModelItem m))return;
            if(m.getName()==null || m.getStore()==null) return;
            items.add(m);
        }
        Action ask=new Action("Export selected object(s) with Yrell AI..."){
            @Override public void run(){open(items);}
        };
        ask.setToolTipText("Prepare local prompts and definition snapshots for OpenCode (no AI server)");
        if(menu.find(ObjectListActionService.GROUP_ADDITIONS)!=null)
            menu.appendToGroup(ObjectListActionService.GROUP_ADDITIONS,ask);
        else menu.add(ask);
    }
    private void open(List<IModelItem> items){
        try {
            var page=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            AIView v=(AIView)page.showView(AIView.ID);
            v.attach(items);
        }catch(PartInitException|RuntimeException e){AIPlugin.error("Could not open AI Assistant view",e);}
    }
    @Override public List<BaseObjectListAction> getHookableActions(String id){return Collections.emptyList();}
}
