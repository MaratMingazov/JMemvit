package org.innopolis.jmemvit;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.Value;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.swt.SWT;


public class Heap extends ViewPart{
	private DebugEventListener jdiEventListener = null;
	private Tree treeOne;
	private Tree treeTwo;

	class RunnableForThread2 implements Runnable{
		public void run() {
			while (true) {
				try { Thread.sleep(1000); } catch (Exception e) { }
				Runnable task = () -> { VizualizateHeapJava();};
				Display.getDefault().asyncExec(task);
			}			
		}
	}
		
	@Override
	public void createPartControl(Composite parent) {
		
		createTreeOne(parent);
		createTreeTwo(parent);

		jdiEventListener = new DebugEventListener();
		DebugPlugin.getDefault().addDebugEventListener(jdiEventListener);
		
		Runnable runnable = new RunnableForThread2();
		Thread Thread2 = new Thread(runnable);
		Thread2.start();	
	}

	@Override
	public void setFocus() {		
	}
	

	
	private void createTreeOne(Composite parent){
		treeOne = new Tree(parent, SWT.MIN);
		treeOne.setHeaderVisible(true);
		treeOne.setLinesVisible(true);		
		treeOne.setVisible(true);

		
		TreeColumn columnName = new TreeColumn(treeOne, SWT.LEFT);
		columnName.setText("classes");
		columnName.setWidth(300);
	}
	
	private void createTreeTwo(Composite parent){
		treeTwo = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		treeTwo.setHeaderVisible(true);
		treeTwo.setLinesVisible(true);		
		treeTwo.setVisible(true);
		
		TreeColumn columnName = new TreeColumn(treeTwo, SWT.LEFT);
		columnName.setText("instances");
		columnName.setWidth(300);		
	}
	
	
	
	
	private void VizualizateHeapJava(){
		if(jdiEventListener == null){return;}		
		if (!jdiEventListener.isItUpdatedThread()){return;}
		
		IJavaThread CurrentThread =  jdiEventListener.getCurrentThread();	
		IStackFrame topFrame = DebugEventListener.getTopStackFrame(CurrentThread);		
		
		VirtualMachine JVM = DebugEventListener.getJVM(topFrame);	
		if (JVM == null){return;}
	
		for (TreeItem item : treeOne.getItems()){item.dispose();}
		for (TreeItem item : treeTwo.getItems()){item.dispose();}
		
		List<ReferenceType> AllMyClasses = DebugEventListener.getAllMyClasses(JVM);
		AllMyClasses = DebugEventListener.sortByHashCode(AllMyClasses);
		for(ReferenceType Class : AllMyClasses){
			VizualizateClass(Class);
			List<ObjectReference> Instances = Class.instances(0);
			for (ObjectReference instance : Instances){VizualizateClassInstance(instance);}	
		}

	}
	
	private void VizualizateClass(ReferenceType Class){

		int hashCode = Class.hashCode();
		
		TreeItem item = new TreeItem(treeOne, SWT.LEFT);
		item.setText(0, Class.toString() + " : @" + hashCode);	
		
		TreeItem subItem = new TreeItem(item, SWT.LEFT);
		subItem.setText(0, "this : @" + hashCode);
					
		List<Field> fields = Class.fields();
		for (Field field : fields){
			if (!field.isStatic()){continue;}
			String stringValue = "";
			Value value = Class.getValue(field);
			if (value != null){stringValue = value.toString();}
			if (stringValue.contains("id")){stringValue = "@"+Class.getValue(field).hashCode();}
			if (field.typeName() != null && value !=null && field.typeName().equals("java.lang.String") ){stringValue = "@"+Class.getValue(field).hashCode();}			
			
			subItem = new TreeItem(item, SWT.LEFT);
			subItem.setText(0, field.typeName() + " " + field.toString() + " : " + stringValue);		
		}	
	}
	
	private void VizualizateClassInstance(ObjectReference Instance){

		TreeItem item = new TreeItem(treeTwo, SWT.LEFT);
		item.setText(0, Instance.toString());

		List<ReferenceType> ParentClasses = new ArrayList<ReferenceType>();
		List<Method> methods = Instance.referenceType().allMethods();
		for (Method method : methods){
			ReferenceType Type =  method.declaringType();
			boolean isExist = false;
			for (ReferenceType ParentClass : ParentClasses){if (ParentClass.equals(Type)){isExist = true;}}
			if (!isExist){ParentClasses.add(Type);}
		}		
		ParentClasses = DebugEventListener.sortByHashCode(ParentClasses);
		
		//the first item placed to the end 
		ReferenceType temp = ParentClasses.get(0);
		ParentClasses.remove(0);
		ParentClasses.add(temp);
		
		for (ReferenceType ParentClass : ParentClasses){
			
			TreeItem subItem = new TreeItem(item, SWT.LEFT);
			subItem.setText(0, ParentClass.toString());
			
			TreeItem subsubItem = new TreeItem(subItem, SWT.LEFT);
			if (Instance.type() != null){subsubItem.setText(0, Instance.type().name());}
			subsubItem.setText(0, "this : @" + Instance.hashCode());

			
			subsubItem = new TreeItem(subItem, SWT.LEFT);
			subsubItem.setText(0, "class : @" + ParentClass.hashCode());
		

			List<Field> Parentfields = ParentClass.fields();
			for (Field field : Parentfields){
				if (field.isStatic()){continue;}
					String valueString = "";
					Value value = Instance.getValue(field);
					if(value == null){valueString = "null";}else{valueString = value.toString();}
					if (valueString.contains("id")){valueString = "@"+Instance.getValue(field).hashCode();}
					if (field.typeName() != null && value !=null && field.typeName().equals("java.lang.String")){valueString = "@"+Instance.getValue(field).hashCode();}	
				
					subsubItem = new TreeItem(subItem, SWT.LEFT);
					subsubItem.setText(0, ""+field.typeName() + " " + field + " : " + valueString);			
			}	
		}		
	}

}
