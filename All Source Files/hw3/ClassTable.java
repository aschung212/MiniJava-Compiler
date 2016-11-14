import java.util.*;
import syntaxtree.*;

public class ClassTable {
    String name = "";
    ClassTable parent;
    String parentName = "";
    LinkedHashMap<String, Type> fields = new LinkedHashMap<String, Type>();
    LinkedHashMap<String, MethodTable> methods = new LinkedHashMap<String, MethodTable>();
    HashMap<String, String> vaporAlias = new HashMap<String, String>();
    
    ClassTable() { // put here so java doesn't complain
        this.name = "";
    }
    
    ClassTable(String name) {
        this.name = name;
    }
    
    void setParent(ClassTable parent) {
        this.parent = parent;
        this.parent = parent;
        this.parentName = parent.name;
    }
    
    String getParentName() {
        return this.parentName;
    }
    
    ClassTable getParentTable() {
        return this.parent;
    }
    
    Boolean addField(String f_name, Type f_type) {
        return (fields.put(f_name, f_type) == null);
    }
    
    Boolean addMethod(String m_name, MethodTable m_table) {
        return (methods.put(m_name, m_table) == null);
    }
    
    Boolean addFields(NodeListOptional locals) {
        Boolean duplicate_field = false;
        for (Node n : locals.nodes) {
            VarDeclaration field = (VarDeclaration)n;
            Type field_type = field.f0;
            String field_name = field.f1.f0.toString();
            if (fields.put(field_name, field_type) != null) {
                duplicate_field = true;
            }
        }
        return !duplicate_field;        
    }
    
    Boolean addMethods(NodeListOptional locals) {
        Boolean validMethods = true;
        for (Node n : locals.nodes) {
            MethodDeclaration method = (MethodDeclaration) n;
            String method_name = method.f2.f0.toString();
            Type method_type = method.f1;
            NodeOptional parameterList = method.f4;
            NodeListOptional localVars = method.f7;
            
            MethodTable methodTable = new MethodTable(method_name, method_type);
            Boolean validParams = methodTable.addParameters(parameterList);
            Boolean validLocalVars = methodTable.addLocalVariables(localVars);
            if (!(validParams && validLocalVars)) {
                validMethods = false;
            }
            if (methods.put(method_name, methodTable) != null) {
                validMethods = false;
            }
        }
        return validMethods;
    }
    
    public void setAlias(String javaName, String alias) {
        vaporAlias.put(javaName, alias);
    }
}
