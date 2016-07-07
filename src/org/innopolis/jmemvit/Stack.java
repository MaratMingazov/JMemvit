




package org.innopolis.jmemvit;


import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.*;
import org.eclipse.swt.SWT;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaThread;





public class Stack extends ViewPart {

	private DebugEventListener jdiEventListener = null;
	private Tree tree = null;

	class RunnableForThread2 implements Runnable{
		public void run() {
			while (true) {
				try { Thread.sleep(1000); } catch (Exception e) { }
				Runnable task = () -> { VizualizateStackJava();};
				Display.getDefault().asyncExec(task);
			}			
		}
	}
	
	public void createPartControl(Composite parent) {
		
		createTree(parent);
		
		this.jdiEventListener = new DebugEventListener();
		DebugPlugin.getDefault().addDebugEventListener(this.jdiEventListener);
		//JDIDebugModel.addJavaBreakpointListener(this);
		
		Runnable runnable = new RunnableForThread2();
		Thread Thread2 = new Thread(runnable);
		Thread2.start();
	}

	@Override
	public void setFocus() {
	}	
	
	private void createTree(Composite parent){
		this.tree = new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.tree.setHeaderVisible(true);
		this.tree.setLinesVisible(true);		
		this.tree.setVisible(true);
		
		TreeColumn columnName = new TreeColumn(this.tree, SWT.LEFT);
		columnName.setText("frames:");
		columnName.setWidth(300);
	}
	
	
	private void VizualizateStackJava(){		
		if(jdiEventListener == null){return;}
		if (!jdiEventListener.isItUpdatedThread()){return;}

		IJavaThread CurrentThread =  jdiEventListener.getCurrentThread();	
		IStackFrame[] Frames = DebugEventListener.getStackFrames(CurrentThread);		
		
		if (Frames == null){return;}
		for (TreeItem item : tree.getItems()){item.dispose();}
	
		for (int i = 0; i< Frames.length; i++){

			IStackFrame frame = Frames[i];
			String FrameName = DebugEventListener.getStackFrameName(frame);
			
			TreeItem item = new TreeItem(tree, SWT.LEFT);
			item.setText(0, FrameName);	
					
			TreeItem subItem;
			
			subItem = new TreeItem(item, SWT.LEFT);
			int lineNumber = DebugEventListener.getStackFrameLineNumber(frame);
			subItem.setText(0, "line number : " + lineNumber);
	
			//subItem = new TreeItem(item, SWT.LEFT);
			//subItem.setText(0, "StackPointer : ");				
			
			//subItem = new TreeItem(item, SWT.LEFT);
			//subItem.setText(0, "ReturnAddress : ");			
		
			IVariable[] variables = DebugEventListener.getStackFrameVariables(frame);
			if (variables != null){
				for (IVariable variable : variables){
					String valueString = "";
					String referenceTypeName = "";
					
					try {valueString =  variable.getValue().toString();} catch (DebugException e) {}
					if (valueString.contains("id")){try {valueString = "@"+variable.getValue().hashCode();} catch (DebugException e) {}}
					try {if (variable.getReferenceTypeName().equals("java.lang.String") ){valueString = "@"+variable.getValue().hashCode();}} catch (DebugException e) {}
					try {referenceTypeName = variable.getReferenceTypeName();} catch (DebugException e) {}
					
					subItem = new TreeItem(item, SWT.LEFT);
					try {subItem.setText(0, variable.getReferenceTypeName());} catch (DebugException e) {}
					subItem.setText(0,referenceTypeName + " " + variable.toString() + " : " + valueString);					
				}
			}
		}
	}	
	
	
	

	

}