package fixmyplugin;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class FixMyBugView extends ViewPart {
	ScrolledComposite scrolledComposite;
	Composite composite;
	Group g1, g2, g3, g4;
	GridData gdt1,gdt2, gdt3, gdt4; 
	Text t1, t2, t3, t4;
	Button b1, b2, b3, b4;
	
    public FixMyBugView() {}
    
    /*
     * Manually added. Changes the contents of the fix boxes to match the received,
     * harmonized fixes.
     */
    public void update(String fix1, String fix2, String fix3, String fix4) {
    	t1.setText(fix1);
    	t2.setText(fix2);
    	t3.setText(fix3);
    	t4.setText(fix4);
    }
    
	@Override
	public void createPartControl(Composite parent) {
		// Tell the parent how to treat its contents.
		parent.setLayout(new FillLayout());
		
		
		// Create the scrolled composite, make it vertical. Let it expand both directions.
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
		scrolledComposite.setLayout(new FillLayout(SWT.VERTICAL));
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setMinSize(parent.computeSize(parent.getClientArea().width, SWT.DEFAULT ));
		// The listener changes the size appropriately when it's resized.
		scrolledComposite.addListener( SWT.Resize, event -> {
			  int width = scrolledComposite.getClientArea().width;
			  scrolledComposite.setMinSize( parent.computeSize( width, SWT.DEFAULT ) );
			} );
		
		
		// Nest a composite in the scrolledComposite. Give it a grid. 
		composite = new Composite(scrolledComposite, SWT.NONE);
		GridLayout outerGrid= new GridLayout();
		outerGrid.numColumns = 1;
		composite.setLayout(outerGrid);
		
		
		// Make the four groups and put them in the composite. Could be expanded to be programmatic.
		g1 = new Group(composite, SWT.NULL);
		g2 = new Group(composite, SWT.NULL);
		g3 = new Group(composite, SWT.NULL);
		g4 = new Group(composite, SWT.NULL);
		g1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		// Give them all grids.
		GridLayout innerGrid = new GridLayout();
		innerGrid.numColumns = 2;
        g1.setLayout(innerGrid);
        g2.setLayout(innerGrid);
        g3.setLayout(innerGrid);
        g4.setLayout(innerGrid);
        
        
     // Make each of the buttons, and tell them what to do when pressed.
        b1 = new Button(g1, SWT.PUSH);
        b2 = new Button(g2, SWT.PUSH);
        b3 = new Button(g3, SWT.PUSH);
        b4 = new Button(g4, SWT.PUSH);
        b1.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        b2.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        b3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        b4.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        b1.setText("Insert this fix");
        b2.setText("Insert this fix");
        b3.setText("Insert this fix");
        b4.setText("Insert this fix");
        b1.setToolTipText("Comments out the selected code and adds the replacement code after it.");
        b2.setToolTipText("Comments out the selected code and adds the replacement code after it.");
        b3.setToolTipText("Comments out the selected code and adds the replacement code after it.");
        b4.setToolTipText("Comments out the selected code and adds the replacement code after it.");
        b1.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
            	try {               
            	    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            	    if ( part instanceof ITextEditor ) {
            	        final ITextEditor editor = (ITextEditor)part;
            	        IDocumentProvider prov = editor.getDocumentProvider();
            	        IDocument doc = prov.getDocument( editor.getEditorInput() );
            	        ISelection sel = editor.getSelectionProvider().getSelection();
            	        if ( sel instanceof TextSelection ) {
            	            final TextSelection textSel = (TextSelection)sel;
            	            String newText = "/* [Pre-Fix] */\n" + 
            	            				 "//" + textSel.getText().replaceAll("\n", "\n//") + "\n" +
            	            			     "/* [Post-Fix] */\n" + 
            	            				  t1.getText() + "\n";
            	            doc.replace( textSel.getOffset(), textSel.getLength(), newText );
            	        }
            	    }
            	} catch ( Exception ex ) {
            	    ex.printStackTrace();
            	}  
            }
        });
        b2.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
            	try {               
            	    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            	    if ( part instanceof ITextEditor ) {
            	        final ITextEditor editor = (ITextEditor)part;
            	        IDocumentProvider prov = editor.getDocumentProvider();
            	        IDocument doc = prov.getDocument( editor.getEditorInput() );
            	        ISelection sel = editor.getSelectionProvider().getSelection();
            	        if ( sel instanceof TextSelection ) {
            	            final TextSelection textSel = (TextSelection)sel;
            	            String newText = "/* [Pre-Fix]\n" + textSel.getText() + "\n*/" + 
   	            				 "\n// [Post-Fix]\n" + t2.getText() + "\n";
            	            doc.replace( textSel.getOffset(), textSel.getLength(), newText );
            	        }
            	    }
            	} catch ( Exception ex ) {
            	    ex.printStackTrace();
            	}  
            }
        });
        b3.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
            	try {               
            	    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            	    if ( part instanceof ITextEditor ) {
            	        final ITextEditor editor = (ITextEditor)part;
            	        IDocumentProvider prov = editor.getDocumentProvider();
            	        IDocument doc = prov.getDocument( editor.getEditorInput() );
            	        ISelection sel = editor.getSelectionProvider().getSelection();
            	        if ( sel instanceof TextSelection ) {
            	            final TextSelection textSel = (TextSelection)sel;
            	            String newText = "/* [Pre-Fix]\n" + textSel.getText() + "\n*/" + 
   	            				 "\n// [Post-Fix]\n" + t3.getText() + "\n";
            	            doc.replace( textSel.getOffset(), textSel.getLength(), newText );
            	        }
            	    }
            	} catch ( Exception ex ) {
            	    ex.printStackTrace();
            	}  
            }
        });
        b4.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
            	try {               
            	    IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            	    if ( part instanceof ITextEditor ) {
            	        final ITextEditor editor = (ITextEditor)part;
            	        IDocumentProvider prov = editor.getDocumentProvider();
            	        IDocument doc = prov.getDocument( editor.getEditorInput() );
            	        ISelection sel = editor.getSelectionProvider().getSelection();
            	        if ( sel instanceof TextSelection ) {
            	            final TextSelection textSel = (TextSelection)sel;
            	            String newText = "/* [Pre-Fix]\n" + textSel.getText() + "\n*/" + 
   	            				 "\n// [Post-Fix]\n" + t4.getText() + "\n";
            	            doc.replace( textSel.getOffset(), textSel.getLength(), newText );
            	        }
            	    }
            	} catch ( Exception ex ) {
            	    ex.printStackTrace();
            	}  
            }
        });
        
        
        // Make each of the text windows. Prevent them from being manually edited, tell them
        // how to behave, and then set listeners to change their height when the text is
        // updated.
        gdt1 = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdt2 = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdt3 = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdt4 = new GridData(SWT.FILL, SWT.FILL, true, true);
        t1 = new Text(g1, SWT.WRAP);
        t2 = new Text(g2, SWT.WRAP);
        t3 = new Text(g3, SWT.WRAP);
        t4 = new Text(g4, SWT.WRAP);
        t1.setLayoutData(gdt1);
        t2.setLayoutData(gdt2);
        t3.setLayoutData(gdt3);
        t4.setLayoutData(gdt4);
        t1.setEditable(false);
        t2.setEditable(false);
        t3.setEditable(false);
        t4.setEditable(false);
        t1.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e)
            {
                Point computeSize = t1.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                gdt1.minimumHeight = computeSize.y;
                gdt1.minimumWidth = computeSize.x;
                g1.layout();
                composite.layout();
                scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        t2.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e)
            {
                Point computeSize = t2.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                gdt2.minimumHeight = computeSize.y;
                gdt2.minimumWidth = computeSize.x;
                g2.layout();
                composite.layout();
                scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        t3.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e)
            {
                Point computeSize = t3.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                gdt3.minimumHeight = computeSize.y;
                gdt3.minimumWidth = computeSize.x;
                g3.layout();
                composite.layout();
                scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        t4.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e)
            {
                Point computeSize = t4.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
                gdt4.minimumHeight = computeSize.y;
                gdt4.minimumWidth = computeSize.x;
                g4.layout();
                composite.layout();
                scrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
        
        
        // Tell the scrolledComposite what lives inside it.
		scrolledComposite.setContent(composite);
    }
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
