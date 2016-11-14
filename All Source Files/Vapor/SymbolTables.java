package hw3;

import syntaxtree.*;
import visitor.*;
import java.util.*;

public class SymbolTables extends GJNoArguDepthFirst<Boolean> {

    public HashMap<String, ClassTable> classTables = new HashMap<String, ClassTable>();
    // subClasses(Child, Parent) for Child extends Parent
    public HashMap<String, String> subclasses = new HashMap<String, String>();
    public Set<String> rebuilt = new HashSet<String>();
    
    @Override
    public Boolean visit(Goal goal) {
        MainClass mc = goal.f0;
        NodeListOptional type_declarations = goal.f1;
        Boolean mc_succeeded = mc.accept(this);
        Boolean rest_succeeded = type_declarations.accept(this);
        
        //subclass relationships are now defined
        //need to build method tables and symbol tables for fields
        for (String child : subclasses.keySet()) {
            rebuildTables(child);
        }
        return (mc_succeeded && rest_succeeded);
    }

    @Override
    public Boolean visit(MainClass mc) {
        String mc_name = mc.f1.f0.toString();
        String method_name = "main";
        NodeListOptional mc_localVars = mc.f14;
        MethodTable mc_method = new MethodTable(method_name);
        mc_method.addLocalVariables(mc_localVars);
        ClassTable mc_table = new ClassTable(mc_name);
        mc_table.methods.put(method_name, mc_method);
        classTables.put(mc_name, mc_table);
        return true;
    }
    
    @Override
    public Boolean visit(TypeDeclaration td) {
        return td.f0.accept(this);
    }
    
    @Override
    public Boolean visit(ClassDeclaration cd) {
        String c_name = cd.f1.f0.toString();
        NodeListOptional varDeclarations = cd.f3;
        NodeListOptional methodDeclarations = cd.f4;
        ClassTable classTable = new ClassTable(c_name);
        Boolean validFields = classTable.addFields(varDeclarations);
        Boolean validMethods = classTable.addMethods(methodDeclarations);
        Boolean validClassDeclaration = (classTables.put(c_name, classTable) == null);
        return (validFields && validMethods && validClassDeclaration);
    }
    
    @Override
    public Boolean visit(ClassExtendsDeclaration ced) {
        String c_name = ced.f1.f0.toString();
        String parent_name = ced.f3.f0.toString();
        subclasses.put(c_name, parent_name);
        ClassTable parent = classTables.get(parent_name);
        NodeListOptional varDeclarations = ced.f5;
        NodeListOptional methodDeclarations = ced.f6;
        ClassTable classTable = new ClassTable(c_name);
        if (parent == null) {
            parent = new ClassTable(parent_name);
        }
        classTable.setParent(parent);
        classTable.addFields(varDeclarations);
        classTable.addMethods(methodDeclarations);
        classTables.put(c_name, classTable);
        return true;
    }
    
    /**
     * recursively rebuild the class's parent until you get to a parent that doesn't extend from anything
     * don't rebuild a class that's already been rebuilt
     * @param className 
     */
    void rebuildTables(String className) {
        if (subclasses.keySet().contains(className) && !rebuilt.contains(className)) {
            ClassTable childTable = classTables.get(className);
            String parentName = subclasses.get(className);
           
            rebuildTables(parentName);
            
            //save preexisting fields and values
            //this must be done element by element, as opposed to by using putAll()
            //because putAll will only do a shallow copy so when the soure Map gets changed,
            //so will the copy. That is bad so instead we do a deep copy element by element.
            LinkedHashMap<String, Type> tempFields = new LinkedHashMap<String, Type>();
            LinkedHashMap<String, MethodTable> tempMethods = new LinkedHashMap<String, MethodTable>();
            for (String k : childTable.fields.keySet()) {
                String fieldName = k;
                Type fieldType = childTable.fields.get(k);
                tempFields.put(fieldName, fieldType);
            }
            for (String k : childTable.methods.keySet()) {
                String methodName = k;
                MethodTable methodTable = childTable.methods.get(k);
                tempMethods.put(methodName, methodTable);
            }
            
            //clear preexisting fields and methods
            childTable.fields = new LinkedHashMap();
            childTable.methods = new LinkedHashMap();
            
            //copy parent's fields and methods
            ClassTable parentTable = classTables.get(parentName);
            LinkedHashMap<String, Type> parentFields = parentTable.fields;
            LinkedHashMap<String, MethodTable> parentMethods = parentTable.methods;
            for (String k : parentFields.keySet()) {
                String fieldName = k;
                Type fieldType = parentTable.fields.get(k);
                childTable.fields.put(fieldName, fieldType);
            }
            for (String k : parentMethods.keySet()) {
                String methodName = k;
                MethodTable methodTable = parentMethods.get(k);
                childTable.methods.put(methodName, methodTable);
            }
            //copy back the original, preexisting fields and methods
            for (String k : tempFields.keySet()) {
                String fieldName = k;
                Type fieldType = tempFields.get(k);
                childTable.fields.put(fieldName, fieldType);
            }
            for (String k : tempMethods.keySet()) {
                String methodName = k;
                MethodTable methodTable = tempMethods.get(k);
                childTable.methods.put(methodName, methodTable);
            }
            rebuilt.add(className);
        }
    }
    
    @Override
    public Boolean visit(NodeListOptional nlo) {
        Boolean allSucceeded = true;
        if (nlo.present()) {
            for (Node n : nlo.nodes) {
                if (!n.accept(this)) {
                    allSucceeded = false;
                }
            }
        }
        return allSucceeded;    
    }
}
