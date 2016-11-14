import java.util.*;
import syntaxtree.*;

public class MethodTable {
    
    String name = "";
    Type returnType = null;
    HashMap<String, Type> parameters = new HashMap<String, Type>();
    HashMap<String, Type> localVariables = new HashMap<String, Type>();
    
    MethodTable(String name) {
        this.name = name;
    }
    
    MethodTable(String name, Type returnType) {
        this.name = name;
        this.returnType = returnType;
    }
    
    /**
     * 
     * @param p_name
     * @param p_type
     * @return true if parameter hasn't been added yet to the current
     *  method table
     */
    Boolean addParameter(String p_name, Type p_type) {
        return (parameters.put(p_name, p_type) == null);
    }
    
    /**
     * 
     * @param lv_name
     * @param lv_type
     * @return return true if local variable hasn't been declared yet
     *  in the current method table and doesn't override a parameter name
     */
    Boolean addLocalVar(String lv_name, Type lv_type) {
        Boolean onlyDeclaration = (localVariables.put(lv_name, lv_type) == null);
        Boolean notInParams = !(parameters.containsKey(lv_name));
        Boolean isValidVarDeclaration = (onlyDeclaration && notInParams);
        return isValidVarDeclaration;
    }
    
    Boolean addParameters(NodeOptional nodeOptional) {
        Boolean validParams = true;
        if (nodeOptional.node != null && nodeOptional.node instanceof FormalParameterList) {
            FormalParameterList fpList = (FormalParameterList) nodeOptional.node;
            FormalParameter first_param = fpList.f0;
            NodeListOptional rest_params = fpList.f1;
            String param_name = first_param.f1.f0.toString();
            Type param_type = first_param.f0;
            addParameter(param_name, param_type);
            if (rest_params != null) {
                for (Node n : rest_params.nodes) {
                    FormalParameterRest fp = (FormalParameterRest) n;
                    param_name = fp.f1.f1.f0.toString();
                    param_type = fp.f1.f0;
                    if (!addParameter(param_name, param_type)) {
                        validParams = false;
                    }
                }
            }
        }
        return validParams;
    }
    
    Boolean addLocalVariables(NodeListOptional locals) {
        Boolean validDeclarations = true;
        for (Node n : locals.nodes) {
            VarDeclaration vd = (VarDeclaration) n;
            String var_name = vd.f1.f0.toString();
            Type var_type = vd.f0;
            if (!addLocalVar(var_name, var_type)) {
                validDeclarations = false;
            }
        }
        return validDeclarations;
    }
}
