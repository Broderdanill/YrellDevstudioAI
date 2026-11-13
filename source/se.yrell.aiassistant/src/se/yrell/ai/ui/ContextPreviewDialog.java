package se.yrell.ai.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/** Exact outbound context including question; no request until Send is explicitly confirmed. */
public final class ContextPreviewDialog extends Dialog {
    private final String preview;
    private final boolean canExport;
    public ContextPreviewDialog(Shell parent,String preview,boolean canExport){super(parent);this.preview=preview;this.canExport=canExport;}
    @Override protected void configureShell(Shell shell){super.configureShell(shell);shell.setText(canExport?"Review context before export":"Preview saved AR context");}
    @Override protected Control createDialogArea(Composite parent){
        Composite area=(Composite)super.createDialogArea(parent);
        area.setLayout(new GridLayout(1,false));
        Label label=new Label(area,SWT.WRAP);
        label.setText(canExport?"The following saved AR snapshot will be written to a local task directory. Review confidential values before exporting.":
                "This is the current saved-definition context (nothing has been exported).");
        label.setLayoutData(new GridData(SWT.FILL,SWT.CENTER,true,false));
        Text content=new Text(area,SWT.MULTI|SWT.READ_ONLY|SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
        content.setText(preview);
        GridData g=new GridData(SWT.FILL,SWT.FILL,true,true);g.widthHint=800;g.heightHint=500;
        content.setLayoutData(g);return area;
    }
    @Override protected void createButtonsForButtonBar(Composite parent){
        if(canExport){createButton(parent,IDialogConstants.OK_ID,"Export task",true);createButton(parent,IDialogConstants.CANCEL_ID,"Cancel",false);}
        else createButton(parent,IDialogConstants.OK_ID,"Close",true);
    }
}
