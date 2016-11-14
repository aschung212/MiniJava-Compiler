import syntaxtree.*;
import visitor.*;
import java.util.*;

/**
 *  The type checker for MiniJava
 */
public class Typecheck extends GJDepthFirst<Type, Map<String, Type>>{
    
    public static final boolean DESCRIBE_ERRORS = true;
    
    public Set<String> classes = new HashSet<String>();
    
    // first id is child, second id is parent
    public HashMap<String, String> subclasses = new HashMap<>();
    
    // outer key is class name, inner key is method name, inner value is a list of the 
    // expected argument types (the first element in the list is the return type of the method)
    public HashMap<String, HashMap<String, List<Type>>> methodTables = new HashMap<>();
    
    public static void main (String [] args) {
        Typecheck typechecker = new Typecheck();
        try {
            Goal goal = new MiniJavaParser(System.in).Goal();
            HashMap<String, Type> a = new HashMap<String, Type>();

            /*TODO:
            X    -1) populate global class table with available class names
            X    -2) create method tables for all classes
            X    -3) create table that describes all child-parent class extends relationships
            X    -4) check no circular subclassing
            X    -5) check no method overloading
            X    -6) check all identifiers (fields, method names) in all classes are distinct
            X    -7) check that main class type checks
            X    -8) check that all other classes type check
            */
            
            // (1 + 2 + 3) create symbol tables for classes and verify unique class names
            //==========================================================================================
            NodeListOptional typeDeclarations = goal.f1;
            MainClass mc = goal.f0;
            String mc_name = mc.f1.f0.toString();
            typechecker.classes.add(mc_name);
            typechecker.getClassesAndMethodTables(typechecker, typeDeclarations, a);
                        
            // (4) verify that there are no subclass cycles
            //========================================================================
            if (!typechecker.noCyclicalSubtyping(typechecker)) {
                typechecker.typeError("cannot extend from a subclass");
            }
            
            // (5) verify that there is no method overloading
            //=========================================================================
            if (!typechecker.noOverloading(typechecker)) {
                typechecker.typeError("no overloading");
            }
            
            // (6 + 7 + 8) check that main class and other classes type check
            //=========================================================================
            goal.accept(typechecker, a);
            System.out.println("Program type checked successfully");
        } catch (ParseException e) {
            //only for testing purposes, dont actually do this!
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public Type visit(Goal g, Map<String, Type> a) {
        MainClass mainClass = g.f0;
        NodeListOptional otherClasses = g.f1;
        mainClass.accept(this, a);
        otherClasses.accept(this, a);
        return null;
    }
    
    // METHODS AND VARIABLES
    //======================================================================
    
    public Type visit(VarDeclaration vd, Map<String, Type> a) {
        Type t = vd.f0;
        String varName = vd.f1.f0.toString();
        //need to verify that identifier is a valid type name
        if (t.f0.choice instanceof Identifier) {
            Identifier t_id = (Identifier)t.f0.choice;
            String typeName = t_id.f0.toString();
            if (a.get(typeName) == null) {
                typeError("invalid local variable type");
            }
        }
        //add local variable to local type environment
        a.put(varName, t);
        return t;
    }
    
    public Type visit(MethodDeclaration md, Map<String, Type> a_parent) {
        //each method should have its own type environment
        Map<String, Type> a = new HashMap<>();
        a.putAll(a_parent);
        Type methodType = md.f1;
        Node parameters = md.f4.node; // can be null
        NodeListOptional localVariables = md.f7;
        NodeListOptional statements = md.f8;
        Expression retExpression = md.f10;
        Set<String> parameterNames = new HashSet<>();
        Set<String> localVars = new HashSet<>();

        // check that methodtype is a valid type
        // this must be done after we have created a table of all available class types
        methodType.accept(this, a);
        
        // verify that parameters is either null, or a formalparameterlist
        if (parameters != null && parameters instanceof FormalParameterList) {
            FormalParameterList fplist = (FormalParameterList)parameters;
            FormalParameter first_param = fplist.f0;
            NodeListOptional rest_params = fplist.f1;
            //create a list of the names (identifiers) of all formal parameters
            String param_name = first_param.f1.f0.toString();
            parameterNames.add(param_name);
            if (rest_params != null) {
                for (int i = 0; i < rest_params.size(); i++) {
                    if (rest_params.elementAt(i) instanceof FormalParameterRest) {
                        FormalParameter fp = ((FormalParameterRest)rest_params.elementAt(i)).f1;
                        Identifier fp_id = fp.f1;
                        param_name = fp_id.f0.toString();
                        if (!parameterNames.add(param_name)) {
                            typeError("Cannot have multiple parameters with same name");
                        }
                    }//added current parameter name
                }//added all parameter names
            }//close null check
            //verify all parameters type check and add them to the current environment
            ((FormalParameterList)parameters).accept(this, a);
        }
        
        //verify that all local variables are distinct
        if (localVariables != null) {
            for (int i = 0; i < localVariables.size(); i++) {
                Node lv_node = localVariables.elementAt(i);
                if (lv_node instanceof VarDeclaration) {
                    VarDeclaration lv = (VarDeclaration)lv_node;
                    Identifier lv_id = lv.f1;
                    String lv_name = lv_id.f0.toString();
                    if (!localVars.add(lv_name)) {
                        typeError("Cannot have multiple local variables with same name");
                    }
                }
            }
        }
        
        //verify that no local variable and parameter share a name
        for (String param_name : parameterNames) {
            if (localVars.contains(param_name)) {
                typeError("Cannot have local variable with same name as parameter");
            }
        }
        
        localVariables.accept(this, a);
        statements.accept(this, a);
        Type retType = retExpression.accept(this, a);
        if (!isSubClass(typeToString(methodType), typeToString(retType))) {
            typeError("Unexpected return type");
        }
        //verify that none of the local variables and parameters have the same identifier
        
        return null;
    }
    
    public Type visit(FormalParameterList fpl, Map<String, Type> a) {
        FormalParameter first_param = fpl.f0;
        NodeListOptional rest_params = fpl.f1;
        if (rest_params != null) {
            for (int i = 0; i < rest_params.size(); i++) {
                Node param = rest_params.elementAt(i);
                if (!(param instanceof FormalParameterRest)) {
                    typeError("expected formal parameter");
                }
            }
        }
        // we must call the accept visitor method to add these parameters to the environment
        first_param.accept(this, a);
        rest_params.accept(this, a);
        return null;
    }
    
    public Type visit(FormalParameter fp, Map<String, Type> a) {
        Type t = fp.f0;
        String name = fp.f1.f0.toString();
        a.put(name, t);
        return null;
    }
    
    public Type visit(FormalParameterRest fpr, Map<String, Type> a) {
        FormalParameter fp = fpr.f1;
        return fp.accept(this, a);
    }
    
    // CLASS DECLARATIONS
    //==============================================================================
    
    public Type visit(MainClass mc, Map<String, Type> a_parent) {
        //dont think we need to create a copy, but doing it just to be safe
        Map<String, Type> a = new HashMap<String, Type>();
        a.putAll(a_parent);
        String mainClassId = mc.f1.f0.toString();
        a.put("this", createIdentifierType(mainClassId));
        // all statements in the main class type check in the environment
        // consisting of only the main class's local variables
        NodeListOptional localVars = mc.f14;
        NodeListOptional statements = mc.f15;
        if (localVars != null) {
            Set<String> distinctLocalVars = new HashSet<String>();
            // add local variables to type environment 'a' (should be empty initially)
            // and type check statements against 'a'
            for (int i = 0; i < localVars.size(); i++) {
                Node lv_node = localVars.elementAt(i);
                if (lv_node instanceof VarDeclaration) {
                    VarDeclaration lv = (VarDeclaration)lv_node;
                    Identifier lv_id = lv.f1;
                    String lv_name = lv_id.f0.toString();
                    if (!distinctLocalVars.add(lv_name)) {
                        typeError("local variables cannot have same name as each other");
                    }
                    //verify that local variable type checks and add it to the current environment
                    lv.accept(this, a);
                } else {
                    typeError("expected variable declaration");
                }
            }//done checking all local variables in main class
        }//close null check
        if (statements != null) {
            for (int i = 0; i < statements.size(); i++) {
                Node st_node = statements.elementAt(i);
                if (!(st_node instanceof Statement)) {
                    typeError("expected statement");
                }
                Statement st = (Statement)st_node;
                st.accept(this, a);
            }//all statements type check
        }//close null check
        return null;
    }
    
    public Type visit(ClassDeclaration cd, Map<String, Type> a_parent) {
        // each class should have its own local type environment
        Map<String,Type> a = new HashMap<>();
        a.putAll(a_parent);
        String className = cd.f1.f0.toString();
        a.put("this", createIdentifierType(className));
        NodeListOptional variableDeclarations = cd.f3;
        NodeListOptional methodDeclarations = cd.f4;
        Set<String> distinctFieldNames = new HashSet<String>();
        Set<String> distinctMethodNames = new HashSet<String>();
        if (variableDeclarations != null) {
            for (int i = 0; i < variableDeclarations.size(); i++) {
                Node fieldNode = variableDeclarations.elementAt(i);
                if (!(fieldNode instanceof VarDeclaration)) {
                    typeError("expected variable declaration");
                }
                VarDeclaration field = (VarDeclaration)fieldNode;
                //type check field declaration and add it to current environment
                field.accept(this, a);
                String fieldId = field.f1.f0.toString();
                if (!distinctFieldNames.add(fieldId)) {
                    // attempting to overload local variable name
                    typeError("variable name " + fieldId + " already declared in this class");
                }
            }//all fields type check and have distinct names
        }//close null check
        if (methodDeclarations != null) {
            for (int i = 0; i < methodDeclarations.size(); i++) {
                Node methodNode = methodDeclarations.elementAt(i);
                if (!(methodNode instanceof MethodDeclaration)) {
                    typeError("expected method declaration");
                }
                MethodDeclaration methodDeclaration = (MethodDeclaration)methodNode;
                methodDeclaration.accept(this, a);
                String methodName = methodDeclaration.f2.f0.toString();
                if (!distinctMethodNames.add(methodName)) {
                    typeError("method names must be distinct");
                }
            }//all method declarations type check and have distinct names
        }//close null check
        return null;
    }
    
    public Type visit(ClassExtendsDeclaration ced, Map<String, Type> a_parent) {
        // each class should have its own local type environment
        Map<String,Type> a = new HashMap<>();
        a.putAll(a_parent);
        String className = ced.f1.f0.toString();
        a.put("this", createIdentifierType(className));
        NodeListOptional variableDeclarations = ced.f5;
        NodeListOptional methodDeclarations = ced.f6;
        Set<String> distinctFieldNames = new HashSet<String>();
        Set<String> distinctMethodNames = new HashSet<String>();
        if (variableDeclarations != null) {
            for (int i = 0; i < variableDeclarations.size(); i++) {
                // make sure we actually have a variable decalaration
                Node fieldNode = variableDeclarations.elementAt(i);
                if (!(fieldNode instanceof VarDeclaration)) {
                    typeError("expected field declaration");
                }
                // verify that the field type checks and add it to the environment
                VarDeclaration field = (VarDeclaration)fieldNode;
                field.accept(this, a);
                String fieldId = field.f1.f0.toString();
                if (!distinctFieldNames.add(fieldId)) {
                    // attempting to declare local variable more than once
                    typeError("variable name " + fieldId + " already declared in this class");
                }
            }//all fields type check and have distinct names
        }//close null check
        if (methodDeclarations != null) {
            for (int i = 0; i < methodDeclarations.size(); i++) {
                // make sure we actually have a method declaration
                Node methodNode = methodDeclarations.elementAt(i);
                if (!(methodNode instanceof MethodDeclaration)) {
                    typeError("expected method declaration");
                }
                //verify that the method declaration type checks int he current environment
                MethodDeclaration methodDeclaration = (MethodDeclaration)methodNode;
                methodDeclaration.accept(this, a);
                String methodName = methodDeclaration.f2.f0.toString();
                if (!distinctMethodNames.add(methodName)) {
                    typeError("method names must be distinct");
                }
            }//all methods type check and have distinct names
        }//close null check
        variableDeclarations.accept(this, a);
        methodDeclarations.accept(this, a);
        return null;
    }
    
    // STATEMENT OPTIONS
    //=====================================================
    
    // Statement is a wrapper class for each of 
    // the statement options below
    public Type visit(Statement s, Map<String, Type> a) {
        return s.f0.accept(this, a);
    }
    
    //represents a list of multiple statements
    public Type visit(Block b, Map<String, Type> a) {
        NodeListOptional statementList = b.f1;
        return statementList.accept(this, a);
    }
    
    // assigning a value to a variable
    public Type visit(AssignmentStatement as, Map<String, Type> a) {
        Identifier lhs_id = as.f0;
        Expression expr = as.f2;
        Type lhs_t = lhs_id.accept(this, a);
        Type rhs_t = expr.accept(this, a);
        String lhsAsString = typeToString(lhs_t);
        String rhsAsString = typeToString(rhs_t);
        if (!isSubClass(rhsAsString, lhsAsString)) {
            typeError("tried to assign a " + rhsAsString + " to a " + lhsAsString);
        }
        return null;
    }
    
    // assigning a value to an indexed location in an array
    public Type visit(ArrayAssignmentStatement aas, Map<String, Type> a) {
        Identifier arrayName = aas.f0;
        Expression index = aas.f2;
        Expression value = aas.f5;
        Type arr_type = arrayName.accept(this, a); //the array must be present in the env
        if (!typeToString(arr_type).equals("array")){
            typeError("cannot index nonarray variable" + arrayName.f0.toString());
        }
        Type index_t = index.accept(this, a);
        if (!typeToString(index_t).equals("int")) {
            typeError("cannot pass non integer expression into index field of array");
        }
        Type value_t = value.accept(this, a);
        if (!typeToString(value_t).equals("int")) {
            typeError("array values must be of type int");
        }
        return null;
    }
    
    // if statements (must have an else clause)
    public Type visit(IfStatement is, Map<String, Type> a) {
        Expression condition = is.f2;
        Statement iftrue = is.f4;
        Statement iffalse = is.f6;
        Type condition_t = condition.accept(this, a);
        if (!(condition_t.f0.choice instanceof BooleanType)) {
            typeError("condition for if statement must be boolean type");
        }
        //verify that both if and else statements typecheck in current environment
        iftrue.accept(this, a);
        iffalse.accept(this, a);
        return null;
    }
        
    // while statements
    public Type visit(WhileStatement ws, Map<String, Type> a) {
        Expression condition = ws.f2;
        Statement statement = ws.f4;
        Type condition_t = condition.accept(this, a);
        if (!(condition_t.f0.choice instanceof BooleanType)) {
            typeError("while condition must be a boolean type");
        }
        // body of while statement must typecheck
        statement.accept(this, a);
        return null;
    }
    
    // printing to output (can only print ints)
    public Type visit(PrintStatement ps, Map<String, Type> a) {
        Expression exp = ps.f2;
        Type exp_t = exp.accept(this, a);
        if (!(exp_t.f0.choice instanceof IntegerType)) {
            typeError("can only print integer types");
        }
        return null;
    }
    
    // EXPRESSION OPTIONS
    // ====================================================================
    
    // wrapper class for expression options listed below
    public Type visit(Expression expr, Map<String, Type> a) {
        return expr.f0.accept(this, a);
    }
    
    public Type visit(ExpressionList exprList, Map<String, Type> a) {
        Expression firstExpr = exprList.f0;
        NodeListOptional restExprs = exprList.f1;
        firstExpr.accept(this, a);
        restExprs.accept(this, a);
        return null;
    }
    
    public Type visit(ExpressionRest exprRest, Map<String, Type> a) {
        Expression expr = exprRest.f1;
        return expr.accept(this, a);
    }
    
    // a && b (both a and b must be boolean types)
    public Type visit(AndExpression ae, Map<String, Type> a) {
        PrimaryExpression lhs = ae.f0;
        PrimaryExpression rhs = ae.f2;
        Type lhs_t = lhs.accept(this, a);
        Type rhs_t = rhs.accept(this, a);
        String lhs_t_asString = typeToString(lhs_t);
        String rhs_t_asString = typeToString(rhs_t);
        if (lhs_t_asString.equals("boolean") && rhs_t_asString.equals("boolean")) {
            return createBoolType();
        }
        else
            typeError("cannot evaluate && on nonboolean expressions");
            return null;
    }
    
    // a < b --> boolean (both a and b must be integer types)
    public Type visit(CompareExpression ce, Map<String, Type> a) {
        PrimaryExpression lhs = ce.f0;
        PrimaryExpression rhs = ce.f2;
        Type lhs_t = lhs.accept(this, a);
        Type rhs_t = rhs.accept(this, a);
        if (!bothInts(lhs_t, rhs_t)) {
            typeError("cannot compare noninteger types");
        }
        return createBoolType();
    }
    
    // a+b --> integer (both a and b must be integer types)
    public Type visit(PlusExpression pe, Map<String, Type> a) {
        PrimaryExpression lhs = pe.f0;
        PrimaryExpression rhs = pe.f2;
        Type lhs_t = lhs.accept(this, a);
        Type rhs_t = rhs.accept(this, a);
        if (!bothInts(lhs_t, rhs_t)) {
            typeError("cannot add noninteger types");
        }
        return createIntegerType();
    }
    
    // a-b --> integer (both a and b must be integer types)
    public Type visit(MinusExpression me, Map<String, Type> a) {
        PrimaryExpression lhs = me.f0;
        PrimaryExpression rhs = me.f2;
        Type lhs_t = lhs.accept(this, a);
        Type rhs_t = rhs.accept(this, a);
        if (!bothInts(lhs_t, rhs_t)) {
            typeError("cannot subtract noninteger types");
        }
        return createIntegerType();
    }
    
    // a * b --> integer (both a and b must be integer types)
    public Type visit(TimesExpression te, Map<String, Type> a) {
        PrimaryExpression lhs = te.f0;
        PrimaryExpression rhs = te.f2;
        Type lhs_t = lhs.accept(this, a);
        Type rhs_t = rhs.accept(this, a);
        if (!bothInts(lhs_t, rhs_t)) {
            typeError("cannot multiply noninteger types");
        }
        return createIntegerType();
    }
    
    // a[b] --> integer (a must be array type, b must be integer type)
    public Type visit(ArrayLookup al, Map<String, Type> a) {
        PrimaryExpression arr = al.f0;
        PrimaryExpression index = al.f2;
        Type arr_t = arr.accept(this, a);
        Type index_t = index.accept(this, a);
        if (!(arr_t.f0.choice instanceof ArrayType)) {
            typeError("cannot dereference nonarrays");
        } else if (!(index_t.f0.choice instanceof IntegerType)) {
            typeError("array index must evaluate to type int");
        }
        return createIntegerType();
    }
    
    // a.length --> integer (a must be of array type)
    public Type visit(ArrayLength al, Map<String, Type> a) {
        PrimaryExpression arr = al.f0;
        Type arr_t = arr.accept(this, a);
        if (!(arr_t.f0.choice instanceof ArrayType)) {
            typeError("cannot get length of nonarray type");
        }
        return createIntegerType();
    }
    
    // C.f(x1,...,xn) --> methodType(f)
    // f must exist in class C
    // types of x1,..,xn must be subclasses of types in function signature
    public Type visit(MessageSend ms, Map<String, Type> a_parent) {
        //might not need to make a copy of the environment, but just to be safe...
        Map<String, Type> a = new HashMap<String, Type>();
        a.putAll(a_parent);
        PrimaryExpression classType = ms.f0;
        Identifier method_id = ms.f2;
        String method_name = method_id.f0.toString();
        NodeOptional argumentList = ms.f4;
        Type classType_t = classType.accept(this, a);
        if (!(classType_t.f0.choice instanceof Identifier)) {
            typeError(typeToString(classType_t) + " does not have member functions to call");
        }
        Identifier classMethodBelongsTo_node = (Identifier)classType_t.f0.choice;
        String classMethodBelongsTo = classMethodBelongsTo_node.f0.toString();
        List<Type> expectedSignature = new ArrayList<Type>();
        //verify that method exists in method table of methodBelongsTo or one of its superclasses
        boolean methodFound = false;
        HashMap<String, List<Type>> methodTable = methodTables.get(classMethodBelongsTo);
        if (methodTable != null) {
            if (!methodTable.containsKey(method_name)) {
                String parent = classMethodBelongsTo;
                //if method not found in class's method table, check superclasses' method tables
                while(subclasses.get(parent) != null) {
                    parent = subclasses.get(parent);
                    methodTable = methodTables.get(parent);
                    if (methodTable != null && methodTable.containsKey(method_name)) {
                        methodFound = true;
                        expectedSignature = methodTable.get(method_name);
                        break;
                    }
                }
            } else {
                methodFound = true;
                expectedSignature = methodTable.get(method_name);
            }
        }
        if (!methodFound) {
            typeError("method " + method_name + "does not exist in " + classMethodBelongsTo + "'s environment");
        }

        //verify that arguments type check
        if (argumentList != null) {
            argumentList.accept(this, a);
        }

        //check that arguments are subclasses of expected types
        // 0) get list of argument types
        // 1) get list of expected types 
        // 2) verify argument list and expected parameter types list are same length
        // 3) check each argument against expected type for valid subclass

        // (0) get list of argument types
        List<Type> argTypes = new ArrayList<Type>();
        Type argType = null;
        if (argumentList.node != null) {
            if (!(argumentList.node instanceof ExpressionList)) {
                typeError("arguments must be expressions");
            }
            ExpressionList argList = (ExpressionList)argumentList.node;
            Expression firstArg = argList.f0;
            NodeListOptional restArgs = argList.f1;
            // verify first arg type checks and add first argument type to list
            argType = firstArg.accept(this, a);
            argTypes.add(argType);
            // add the rest of the argument types to the list in order
            for (int i = 0; i < restArgs.size(); i++) {
                // verify that current argument is an expression
                Node arg = restArgs.elementAt(i);
                if (!(arg instanceof ExpressionRest)) {
                    typeError("arguments must be expressions");
                } else {
                    // verify current arg type checks and add it to the list
                    argType = ((ExpressionRest)arg).f1.accept(this, a);
                    argTypes.add(argType);
                }
            }
        }// argTypes has types of all arguments passed to method
        // (2)
        // argTypes is missing return type (first element in expectedSignature)
        if (argTypes.size() != (expectedSignature.size()-1)) {
            typeError("incorrect number of arguments passed to method");
        }
        
        // (3) check each argument is a subclass of the expeted type
        for (int i = 0; i < argTypes.size(); i++) {
            Type arg_t = argTypes.get(i);
            String argTypeAsString = typeToString(arg_t);
            Type expected_t = expectedSignature.get(i+1);
            String expectedTypeAsString = typeToString(expected_t);
            if (!isSubClass(argTypeAsString, expectedTypeAsString)) {
                typeError("invalid argument type passed into method " + method_name + " at position " + i);
            }
        }
        
        Type methodType = expectedSignature.get(0);
        return methodType;
    }
    
    // PRIMARY EXPRESSION OPTIONS
    // ======================================================================
    
    public Type visit(PrimaryExpression pe, Map<String, Type> a) {
        if (pe.f0.choice instanceof Identifier) {
            Identifier id = (Identifier) pe.f0.choice;
            String name = id.f0.toString();
            if (a.get(name) == null && classes.contains(name)) {
                return createIdentifierType(name);    
            }
        }
        return pe.f0.accept(this, a);
    }
    
    public Type visit(IntegerLiteral il, Map<String, Type> a) {
        return createIntegerType();
    }
    
    public Type visit(TrueLiteral tl, Map<String, Type> a) {
        return createBoolType();
    }
    
    public Type visit(FalseLiteral fl, Map<String, Type> a) {
        return createBoolType();
    }
    
    public Type visit(ThisExpression te, Map<String, Type> a) {
        //should return the type of the current class
        //the value should have been set when type checking the class declaration
        return a.get("this");
    }
    
    public Type visit(ArrayAllocationExpression aae, Map<String, Type> a) {
        Expression arraySize = aae.f3;
        Type shouldBeInt = arraySize.accept(this, a);
        if (!(shouldBeInt.f0.choice instanceof IntegerType)) {
            typeError("cannot use a noninteger value to designate size of array");
        }
        return createArrayType();
    }
    
    public Type visit(AllocationExpression ae, Map<String, Type> a) {
        String id = ae.f1.f0.toString();
        Type t = a.get(id);
        if (id == null) {
            // identifier not found in type environment a
            typeError("Could not find " + id + " in current type environment");
        }
        return t;
    }
    
    public Type visit(NotExpression ne, Map<String, Type> a) {
        Expression toBeNegated = ne.f1;
        Type exprType = toBeNegated.accept(this, a);
        if (!(exprType.f0.choice instanceof BooleanType)) {
            typeError("cannot negate a non boolean expression");
        }
        return exprType;
    }

    public Type visit(BracketExpression be, Map<String, Type> a) {
        Expression expr = be.f1;
        return expr.accept(this, a);
    }
    
    // BASICS
    //==============================================================
    
    /**
    * checks to make sure that the identifier 'i' 
    * is in the current symbol table 'a'.
    * returns the type 't' of identifier 'i'.
    */
    public Type visit(Identifier i, Map<String, Type> a) {
        String id = i.f0.toString();
        Type t = a.get(id);
        if (t == null) {
            // identifier not found in type environment a
            typeError(id + " not declared");
        } // else we found t in the type environment a
        return t;
    }
    
    public Type visit(Type type, Map<String, Type> a) {
        if (type.f0.choice instanceof Identifier) {
            String typename = ((Identifier)type.f0.choice).f0.toString();
            if (!classes.contains(typename)){
                typeError("type " + typename + " does not exist");
            }
        }
        return type;
    }
    
    // OPTIONALS
    //==========================================================
    
    public Type visit(NodeOptional no, Map<String, Type> a) {
        if (no.node == null) {
            return null;
        }
        return no.node.accept(this, a);
    }
    
    public Type visit(NodeListOptional nlo, Map<String, Type> a) {
        if (nlo.present()) {
            for (int i = 0; i < nlo.size(); i++) {
                nlo.elementAt(i).accept(this, a);
            }
        }
        return null;
    }
    
    // HELPER METHODS
    //==================================================
    public void typeError(String message) {
        if (DESCRIBE_ERRORS) {
            println(message);
        } else if (!DESCRIBE_ERRORS) {
            println("type error");
        }
        System.exit(-1);
    }    
    
    public boolean booleanTypeOrLiteral(String x) {
        return (x.equals("true") 
                || x.equals("false") 
                || x.equals("boolean"));
    }
    
    public Type createBoolType() {
        BooleanType booleanType = new BooleanType();
        NodeChoice nodeChoice = new NodeChoice(booleanType);
        Type type = new Type(nodeChoice);
        return type;   
    }
    
    public Type createIntegerType() {
        IntegerType integerType = new IntegerType();
        NodeChoice nodeChoice = new NodeChoice(integerType);
        Type type = new Type(nodeChoice);
        return type;
    }
    
    public Type createArrayType() {
        ArrayType arrayType = new ArrayType();
        NodeChoice nodeChoice = new NodeChoice(arrayType);
        Type type = new Type(nodeChoice);
        return type;
    }
    
    public Type createIdentifierType(String id) {
        NodeToken nt = new NodeToken(id);
        Identifier identifier = new Identifier(nt);
        NodeChoice nodeChoice = new NodeChoice(identifier);
        Type type = new Type(nodeChoice);
        return type;
    }
    
    public boolean bothInts(Type lhs, Type rhs) {
        return ((lhs.f0.choice instanceof IntegerType) 
                && (rhs.f0.choice instanceof IntegerType));
    }
    
    public String typeToString(Type t) {
        Node n = t.f0.choice;
        String asString = null;
        if (n instanceof IntegerType) {
            asString = new String("int");
        } else if (n instanceof BooleanType) {
            asString = new String("boolean");
        } else if (n instanceof ArrayType) {
            asString = new String("array");
        } else if (n instanceof Identifier) {
            Identifier idn = (Identifier)n;
            asString = idn.f0.toString();
        }
        return asString;
    }    
    
    /**
     * the subclasses table must have been created and populated
     * before this method can be called!!!
     * @param child checks if this parameter is a subclass of target
     * @param target checks if this parameter is a superclass of child
     * @return returns true if there was a directed path in the 
     *          subclasses mapping from child to target, indicating
     *          that target is an ancestor of child.
     */
    public boolean isSubClass(String child, String target) {
        String parent = null;
        if (child.equals(target)) {
            // all classes are subclasses of themselves
            return true;
        }
        else while (subclasses.containsKey(child)) {
            parent = subclasses.get(child);
            if (parent.equals(target)){
                return true;
            }
            child = parent;
        }
        return false;
    }
    
    public void getClassesAndMethodTables(Typecheck typechecker, NodeListOptional typeDeclarations , Map<String,Type> a) {
        if (typeDeclarations != null) {
            for (int i = 0; i < typeDeclarations.size(); i++) {
                String typename = null;
                HashMap<String, List<Type>> methodNamesToSignatures = new HashMap<>();
                if (typeDeclarations.elementAt(i) instanceof TypeDeclaration) {
                    TypeDeclaration td = (TypeDeclaration) typeDeclarations.elementAt(i);
                    Node c = td.f0.choice;
                    NodeListOptional methodDeclarations = null;
                    if (c instanceof ClassDeclaration) {
                        ClassDeclaration c_cd = (ClassDeclaration)c;
                        typename = c_cd.f1.f0.toString();
                        methodDeclarations = c_cd.f4;
                        //populate methodNamesToSignatures for each entry in methodDeclarations
                        createMethodTables(methodDeclarations, methodNamesToSignatures);
                    }
                    else if (c instanceof ClassExtendsDeclaration) {
                        ClassExtendsDeclaration ced = (ClassExtendsDeclaration)c;
                        methodDeclarations = ced.f6;                            
                        typename = ced.f1.f0.toString();
                        // (3) create a structure that tracks all child-parent relationships
                        String id_parent = ced.f3.f0.toString();
                        if (typename.equals(id_parent)) {
                            // cannot say class A extends A
                            typechecker.typeError("cannot extend from self");
                        }
                        typechecker.subclasses.put(typename, id_parent);
                        //populate methodNamesToSignatures for each entry in methodDeclarations
                        createMethodTables(methodDeclarations, methodNamesToSignatures);
                    }
                }
                // link class name to the hashmap of its method tables
                if (typename != null) {
                    typechecker.methodTables.put(typename, methodNamesToSignatures);
                    Type id_type = typechecker.createIdentifierType(typename);
                    //add type to environment
                    a.put(typename, id_type);
                    // add the class to the set of declared classes
                    if (!typechecker.classes.add(typename)) {
                        //if the class declaration id already exists in the class table then it is being declared more than once
                        typechecker.typeError("class names must be unique");
                    }//verified current class is distinct
                }//close null check
            }//verified all classes are distinct and created their methodtables
        }//close null check
    }
    
    public void createMethodTables(NodeListOptional methodDeclarations, Map<String, List<Type>> methodNamesToSignatures) {
        /*
        - get each member function
        - from each member function, get the formal parameter list and the method name
        - from the formal parameter list, create a list of types
        - create a hashtable entry that maps the method name to this list of types
        */
        if (methodDeclarations != null) {
            for (int j = 0; j < methodDeclarations.size(); j++) {
                List<Type> methodSignature = new ArrayList<Type>();
                if (methodDeclarations.elementAt(j) instanceof MethodDeclaration) {
                    MethodDeclaration member_func = (MethodDeclaration) methodDeclarations.elementAt(j);
                    String method_name = member_func.f2.f0.toString();
                    Type func_type = member_func.f1;
                    // 1st element of method signature must be return type of the method
                    methodSignature.add(func_type);
                    Node func_params_node = member_func.f4.node;
                    if (func_params_node != null && func_params_node instanceof FormalParameterList) {
                        FormalParameterList func_params = (FormalParameterList)func_params_node;
                        FormalParameter first_param = func_params.f0;
                        NodeListOptional rest_params = func_params.f1; // can be null
                        Type first_param_t = first_param.f0;
                        methodSignature.add(first_param_t);
                        if (rest_params != null) {
                            for (int k = 0; k < rest_params.size(); k++) {
                                if (rest_params.elementAt(k) instanceof FormalParameterRest) {
                                    FormalParameterRest param = (FormalParameterRest)rest_params.elementAt(k);
                                    Type param_t = param.f1.f0;
                                    methodSignature.add(param_t);
                                }//done with current parameter
                            }//done with all parameters for current method
                        }//close null check (has more than 1 parameter)
                    }//close null check (has any parameters at all)
                    // at this point, the methodSignature list has all parameter and return tyes. link it to the method name
                    methodNamesToSignatures.put(method_name, methodSignature);
                }//done with current method
            }//done with all methods for current ClassDeclaration
        }
    }
    
    public boolean noCyclicalSubtyping(Typecheck t) {
        boolean noCycles = true;
        for (String child: t.subclasses.keySet()) {
            Set<String> family = new HashSet<String>();
            family.add(child);
            String ancestor = t.subclasses.get(child);
            while (t.subclasses.containsKey(ancestor)) {
                if (!family.add(ancestor)) {
                    noCycles = false;
                    break;
                }
                ancestor = t.subclasses.get(ancestor);
            }
        }
        return noCycles;
    }
    
    public boolean noOverloading(Typecheck typechecker) {
        // only need to check classes that extend other classes
        // must have already verified that ther is no cyclical subclassing
        /*
        - for each class that has a superclass, check if any of its methods exist in any of its super classes
        - if the method name is found in a super class's method table, verify that the method signatures are the same
        - if method name is not found, go to next method in current class's method table
        */
        boolean nooverloads = true;
        for (String child : typechecker.subclasses.keySet()) {
            HashMap<String, List<Type>> child_methodTable = typechecker.methodTables.get(child);
            String parent = child;
            while (typechecker.subclasses.get(parent) != null) {
                //while there is a superclass to check against...
                parent = typechecker.subclasses.get(parent);
                HashMap<String, List<Type>> parent_methodTable = typechecker.methodTables.get(parent);
                if (child_methodTable != null && parent_methodTable != null) {
                    for (String methodName : child_methodTable.keySet()) {
                        List<Type> child_methodSig = child_methodTable.get(methodName);
                        if (parent_methodTable.containsKey(methodName)) {
                            List<Type> parent_methodSig = parent_methodTable.get(methodName);
                            if (child_methodSig.size() != parent_methodSig.size()) {
                                nooverloads = false;
                            } else {
                                //child and current ancestor both have the current method in their method table
                                for (int p = 0; p < child_methodSig.size(); p++) {
                                    String child_paramType = typechecker.typeToString(child_methodSig.get(p));
                                    String parent_paramType = typechecker.typeToString(parent_methodSig.get(p));
                                    if (!child_paramType.equals(parent_paramType)) {
                                        nooverloads = false;
                                    }//current type in signature is same in both method signatures
                                }
                            }
                        }//done checking current method against current ancestor
                    }//done checking all methods against current ancestor
                }//close null check
            }//done checking child against all supertypes
        }//done checking all classes with superclasses
        return nooverloads;
    }
    
    void println(String s) {
        System.out.println(s);
    }
    
    
    
    
    
    // CALLING TYPECHECK FROM AN EXTERNAL CLASS FOR J2V CONVERSION:
    //======================================================================
    
    public Typecheck doTypecheck(Goal goal) {
        HashMap<String, Type> a = new HashMap<String, Type>();

            /*TODO:
            X    -1) populate global class table with available class names
            X    -2) create method tables for all classes
            X    -3) create table that describes all child-parent class extends relationships
            X    -4) check no circular subclassing
            X    -5) check no method overloading
            X    -6) check all identifiers (fields, method names) in all classes are distinct
            X    -7) check that main class type checks
            X    -8) check that all other classes type check
            */
            
            // (1 + 2 + 3) create symbol tables for classes and verify unique class names
            //==========================================================================================
            NodeListOptional typeDeclarations = goal.f1;
            MainClass mc = goal.f0;
            String mc_name = mc.f1.f0.toString();
            classes.add(mc_name);
            getClassesAndMethodTables(this, typeDeclarations, a);
                        
            // (4) verify that there are no subclass cycles
            //========================================================================
            if (!noCyclicalSubtyping(this)) {
                typeError("cannot extend from a subclass");
            }
            
            // (5) verify that there is no method overloading
            //=========================================================================
            if (!noOverloading(this)) {
                typeError("no overloading");
            }
            
            // (6 + 7 + 8) check that main class and other classes type check
            //=========================================================================
            goal.accept(this, a);
            return this;
    }
}
