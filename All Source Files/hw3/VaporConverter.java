package hw3;

import visitor.*;
import syntaxtree.*;
import java.util.*;

public class VaporConverter extends DepthFirstVisitor {
    
    SymbolTables symbolTables = new SymbolTables();
    // definedVars = HashMap<actual_name, alias>
    HashMap<String, String> definedVars = new HashMap<String, String>();
    
    int indent = 0;
    int localVarCount = 0;
    int exprCount = 0;
    // block label counters
    int genericBlockCount = 0;
    int ifCount = 1;    //starts at 1 to aboid confusion with if0
    int whileCount = 0;
    int notCount = 0;
    int andCount = 0;
    int boundsCount = 0;
    
    /**
     * remember to set current class in each class declaration 
     * so that each method knows who called it
     */
    String currentClass = "";
    /**
     * remember to set current method in each method declaration
     * so that local environment gets correctly updated
     */
    String currentMethod = "";
    
    public VaporConverter(SymbolTables symbolTables) {
        this.symbolTables = symbolTables;
    }
    
    public String blockLabel() {
        String label = "block_" + genericBlockCount;
        return label;
    }
    
    public String bounds() {
        String label = "bounds_" + boundsCount;
        return label;
    }
    
    public String ifLabel() {
        String label = "if_true_" + ifCount;
        return label;
    }
    
    public String ifEnd() {
        String label = "if_end_" + ifCount;
        return label;
    }
    
    public String whileLabelTop() {
        String label = "while_top_" + whileCount;
        return label;
    }
    
    public String whileLabelEnd() {
        String label = "while_end_" + whileCount;
        return label;
    }
    
    public String setTrueLabel() {
        String label = "set_true_" + notCount;
        return label;
    }
    
    
    public String endNotLabel() {
        String label = "end_not_" + notCount;
        return label;
    }
    
    public String setFalseLabel() {
        String label = "set_false_" + andCount;
        return label;
    }
    
    public String endAndLabel() {
        String label = "end_and_" + andCount;
        return label;
    }
    
    public String newVar() {
        String var = "t." + localVarCount++;
        return var;
    }
    
    public String expressionResultInc() {
        String result = expressionResult();
        exprCount++;
        return result;
    }
    
    public String expressionResult() {
        String result = "expr_result_" + exprCount;
        return result;
    }
    
    public String prevExpressionResult() {
        exprCount--;
        return expressionResultInc();
    }
    
    @Override
    public void visit(Goal goal) {
        MainClass mc = goal.f0;
        NodeListOptional typeDeclarations = goal.f1;
        
        // print all virtual method tables so that they may be referenced
        for (String className : symbolTables.classTables.keySet()) {
            if (!className.equals(mc.f1.f0.toString())) {
                printVMT(className);
            }
        }
        
        mc.accept(this);
        typeDeclarations.accept(this);
        defineAllocArray();
    }
    
    @Override
    public void visit(MainClass mc) {
        String mc_name = mc.f1.f0.toString();
        definedVars.clear();
        currentClass = mc_name;
        currentMethod = "main";
        println("func Main()");
        indent++;
        NodeListOptional statements = mc.f15;
        statements.accept(this);
        println("ret\n");
        indent--;
        resetVarCounts();
    }
    
    @Override
    public void visit(ClassDeclaration cd) {
        resetVarCounts();
        definedVars.clear();
        String className = cd.f1.f0.toString();
        currentClass = className;
        NodeListOptional methods = cd.f4;
        methods.accept(this);
    }
    
    @Override
    public void visit(ClassExtendsDeclaration ced) {
        resetVarCounts();
        definedVars.clear();
        String className = ced.f1.f0.toString();
        currentClass = className;
        NodeListOptional methods = ced.f6;
        methods.accept(this);
    }
    
    @Override
    public void visit(MethodDeclaration md) {
        resetVarCounts();
        definedVars.clear();
        String methodName = md.f2.f0.toString();
        currentMethod = methodName;
        Node parameterNode = md.f4.node;
        NodeListOptional statements = md.f8;
        Expression retExpression = md.f10;
        
        String parameterString = "";
        if (parameterNode != null) {
            //if there are formal parameters, get them as strings
            FormalParameterList fplist = (FormalParameterList) parameterNode;
            parameterString = " " + parameterListToString(fplist);
        }
        String methodTitle = "func " + currentClass + "." + methodName + "(this" 
                                + parameterString +  ")";
        println(methodTitle);
        indent++;
        statements.accept(this);
        
        //return
        String result = expressionResult();
        retExpression.accept(this);
        println("ret " + result);
        println("");
        indent--;
    }
    
    // Statement Productions
    //=====================================================
    /**
    * Grammar production:
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    @Override
    public void visit(Statement st) {
        st.f0.accept(this);
    }
    
    @Override
    public void visit(Block b) {
        println(blockLabel() + ":");
        indent++;
        b.f1.accept(this);
        indent--;
    }
    
    @Override
    public void visit(AssignmentStatement ass) {
        Identifier id = ass.f0;
        String alias = new String();
        Expression assignment = ass.f2;
        String name = id.f0.toString();
        if (definedVars.containsKey(name)) {
            alias = "[" + definedVars.get(name) + "]";
        } else {
            id.accept(this);
            alias = "[" + definedVars.get(name) + "]";
        }
        if (assignment.f0.choice instanceof PrimaryExpression) {
            printlnAssign(alias, (PrimaryExpression)assignment.f0.choice);
        } else {
            String result = expressionResult();
            assignment.accept(this);
            println(alias + " = " + result);
        }
    }
    
    @Override
    public void visit(IfStatement ifStatement) {
        Expression expression = ifStatement.f2;
        Statement ifTrue = ifStatement.f4;
        Statement ifFalse = ifStatement.f6;
        String expressionResult = expressionResult();
        expression.accept(this);
        String trueBranch = ifLabel() + ":";
        String gotoEnd = "goto :" + ifEnd();
        //if true, jump to true block. else, continue on to else statements
        String ifCondition = "if " + expressionResult + " goto :" + ifLabel();
        println(ifCondition);
        //else block (false branch)
        indent++;
        ifFalse.accept(this);
        println(gotoEnd);
        indent--;
        //true branch
        println(trueBranch);
        indent++;
        ifTrue.accept(this);
        println(gotoEnd);
        indent--;
        println(ifEnd() + ":");    
        ifCount++;
    }
    
    @Override
    public void visit(WhileStatement ws) {
        Expression condition = ws.f2;
        Statement statement = ws.f4;
        println(whileLabelTop() + ":");
        String result = expressionResult();
        condition.accept(this);
        println("if0 " + result + " goto :" + whileLabelEnd());
        indent++;
        statement.accept(this);
        println("goto :" + whileLabelTop());
        indent--;
        println(whileLabelEnd() + ":");        
        whileCount++;
    }
    
    @Override
    public void visit(PrintStatement ps) {
        Expression expression = ps.f2;
        String result = expressionResult();
        expression.accept(this);
        println("PrintIntS(" + result + ")");
    }
    
    // Expression Productions
    //=====================================================================
    /**
    * Grammar production:
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | PrimaryExpression()
    */
    @Override
    public void visit(Expression expression) {
        expression.f0.accept(this);
    }
    
    @Override
    public void visit(ExpressionRest expressionRest) {
        Expression expression = expressionRest.f1;
        expression.f0.accept(this);
    }
    
    @Override
    public void visit(AndExpression andExpression) {
        String result = expressionResultInc();
        PrimaryExpression lhs = andExpression.f0;
        PrimaryExpression rhs = andExpression.f2;
        String lhsVar = newVar();
        String rhsVar = newVar();
        printlnAssign(lhsVar, lhs);
        printlnAssign(rhsVar, rhs);
        println(result + " = 1");
        println("if0 " + lhsVar + " goto :" + setFalseLabel());
        println("if0 " + rhsVar + " goto :" + setFalseLabel());
        println("goto :" + endAndLabel());
        println(setFalseLabel() + ":");
        indent++;
        println(result + " = 0");
        indent--;
        println(endAndLabel());
        andCount++;
    }
    
    @Override
    public void visit(CompareExpression compare) {
        String result = expressionResultInc();
        PrimaryExpression lhs = compare.f0;
        PrimaryExpression rhs = compare.f2;
        String lhsVar = newVar();
        String rhsVar = newVar();
        printlnAssign(lhsVar, lhs);
        printlnAssign(rhsVar, rhs);
        println(result + " = LtS(" + lhsVar + " " + rhsVar + ")");
    }
    
    @Override
    public void visit(PlusExpression plus) {
        String result = expressionResultInc();
        PrimaryExpression lhs = plus.f0;
        PrimaryExpression rhs = plus.f2;
        String lhsVar = newVar();
        String rhsVar = newVar();
        printlnAssign(lhsVar, lhs);
        printlnAssign(rhsVar, rhs);
        println(result + " = Add(" + lhsVar + " " + rhsVar + ")");
    }
    
    @Override
    public void visit(MinusExpression minus) {
        String result = expressionResultInc();
        PrimaryExpression lhs = minus.f0;
        PrimaryExpression rhs = minus.f2;
        String lhsVar = newVar();
        String rhsVar = newVar();
        printlnAssign(lhsVar, lhs);
        printlnAssign(rhsVar, rhs);
        println(result + " = Sub(" + lhsVar + " " + rhsVar + ")");
    }
    
    @Override
    public void visit(TimesExpression times) {
        String result = expressionResultInc();
        PrimaryExpression lhs = times.f0;
        PrimaryExpression rhs = times.f2;
        String lhsVar = newVar();
        String rhsVar = newVar();
        printlnAssign(lhsVar, lhs);
        printlnAssign(rhsVar, rhs);
        println(result + " = MulS(" + lhsVar + " " + rhsVar + ")");
    }
    
    @Override
    public void visit(ArrayLookup lookup) {
        PrimaryExpression array = lookup.f0;
        PrimaryExpression index = lookup.f2;
        String result = expressionResultInc();
        String indexResult = expressionResult();
        index.accept(this);
        String validBounds = newVar();
        println(validBounds + " = Lt(0 " + indexResult + ")");
        println("if " + validBounds + " goto " + bounds());
        indent++;
        println("Error(\"array index out of bounds\")");
        indent--;
        println(bounds() + ":");
        boundsCount++;
        String arr_name = expressionResult();
        array.accept(this);
        String size = newVar();
        String indexVar = newVar();
        println(indexVar + " = MulS(4 " + indexResult + ")");
        println(indexVar + " = Add(4 " + indexVar + ")");
        println(size + " = [" + arr_name + " + 0]");
        println(validBounds + " = Lt(" + indexVar + " " + size + ")");
        println("if " + validBounds + " goto " + bounds());
        indent++;
        println("Error(\"array index out of bounds\")");
        indent--;
        println(bounds() + ":");
        boundsCount++;
        println(result + " = [" + arr_name + " + " + indexVar + "]");
    }
    
    @Override
    public void visit(ArrayLength length) {
        String result = expressionResultInc();
        PrimaryExpression array = length.f0;
        String arrayName = newVar();
        printlnAssign(arrayName, array);
        println(result + " = [" + arrayName + " + 0]");
    }
    
    @Override
    public void visit(MessageSend message) {
        String result = expressionResultInc();
        PrimaryExpression callingVar = message.f0;
        String methodName = message.f2.f0.toString();
        NodeOptional args = message.f4;
        String arguments = "";
        if (args.present()) {
            ExpressionList exprList = (ExpressionList) args.node;
            Expression firstExpression = exprList.f0;
            NodeListOptional restExpressions = exprList.f1;
            String firstArg = expressionResult();
            String firstArg_v = newVar();
            firstExpression.accept(this);
            println(firstArg_v + " = " + firstArg);
            arguments = arguments + " " + firstArg_v; 
            if (restExpressions.present()) {
                for (Node n : restExpressions.nodes) {
                    String restArg = expressionResult();
                    String restArg_v = newVar();
                    n.accept(this);
                    println(restArg_v + " = " + restArg);
                    arguments = arguments + " " + restArg_v;
                }
            }
        }
        String methodResult = expressionResult();
        callingVar.accept(this);
        ClassTable classTable = symbolTables.classTables.get(currentClass);
        int pos = new ArrayList<String>(classTable.methods.keySet()).indexOf(methodName);
        pos = pos*4;
        String s = newVar();
        println(methodResult + " = [" + methodResult + " + " + pos + "]");
        println(result + " = call " + methodResult + "(this" + arguments + ")");      
    }
    
    // Primary Expressions
    //=============================================================================
    /**
    * Grammar production:
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */

    @Override
    public void visit(PrimaryExpression primary) {
        primary.f0.accept(this);
    }
    
    @Override
    public void visit(IntegerLiteral integer) {
        String result = expressionResultInc();
        println(result + " = " + integer.f0.toString());
    }
    
    @Override
    public void visit(TrueLiteral t) {
        String result = expressionResultInc();
        println(result + " = 1");
    }
    
    @Override
    public void visit(FalseLiteral f) {
        String result = expressionResultInc();
        println(result + " = 0");
    }
    
    @Override
    public void visit(Identifier id) {
        String result = expressionResultInc();
        String name = id.f0.toString();
        if (!definedVars.containsKey(name)) {
            // first declaration
            ClassTable classTable = symbolTables.classTables.get(currentClass);
            MethodTable methodTable = classTable.methods.get(currentMethod);
            if (methodTable.parameters.containsKey(name) || methodTable.localVariables.containsKey(name)) {
                definedVars.put(name, name);
                println(result + " = " + name);
            } else if (classTable.fields.containsKey(name)) {
                int pos = new ArrayList<String>(classTable.fields.keySet()).indexOf(name);
                pos += 1; // first 4 bytes are the vmt
                pos = pos * 4;
                println(result + " = [this+" + pos + "]");
                definedVars.put(name, result);
            }
        } else {
            println(result + " = " + definedVars.get(name));
        }       
    }
    
    @Override
    public void visit(ThisExpression t) {
        String result = expressionResultInc();
        println(result + " = [this]");
    }
    
    @Override
    public void visit(ArrayAllocationExpression allocation) {
        String result = expressionResultInc();
        Expression sizeExpression = allocation.f3;
        String sizeResult = expressionResult();
        sizeExpression.accept(this);
        String expandedSize = newVar();
        println(expandedSize + " = MulS(4 " + sizeResult + ")");
        println(result + " = call :AllocArray(" + expandedSize + ")");
    }
    
    @Override
    public void visit(AllocationExpression allocation) {
        String className = allocation.f1.f0.toString();
        ClassTable classTable = symbolTables.classTables.get(className);
        int size = classTable.fields.size();
        size += 1; //this is for the vmt
        size = size * 4;
        String result = expressionResultInc();
        println(result + " = HeapAllocZ(" + size + ")");
        println("[" + result + "] = :vmt_" + className);
        }
    
    @Override
    public void visit(NotExpression not) {
        Expression expression = not.f1;
        expression.f0.accept(this);
        String uninverted = prevExpressionResult();
        println("if0 " + uninverted + " goto :" + setTrueLabel());
        indent++;
        println(uninverted + " = 0");
        println("goto :" + endNotLabel());
        indent--;
        println(setTrueLabel() + ":");
        indent++;
        println(uninverted + " = 1");
        indent--;
        println(":" + endNotLabel());
    }
    
    @Override
    public void visit(BracketExpression bracket) {
        Expression expression = bracket.f1;
        expression.accept(this);
    }
    
    // Generic Node Types
    //==============================================================
    
    @Override
    public void visit(NodeListOptional optList) {
        if (optList.present()) {
            for (Node n : optList.nodes) {
                n.accept(this);
            }
        }
    }
    
    @Override
    public void visit(NodeOptional optNode) {
        if (optNode.present()) {
            optNode.node.accept(this);
        }
    }
    
    @Override
    public void visit(NodeList nodeList) {
        for (Node n : nodeList.nodes) {
            n.accept(this);
        }
    }   
    
    
    // helper methods
    //================================================================
    
    public String parameterListToString(FormalParameterList fplist) {
        String params = fplist.f0.f1.f0.toString();
        List<Node> restParams = fplist.f1.nodes;
        for (Node n : restParams) {
            FormalParameterRest p = (FormalParameterRest)n;
            String p_name = p.f1.f1.f0.toString();
            params = params + " " + p_name;
        }
        return params;
    }
    
    
    /**
     * do NOT reset block label counts
     */
    public void resetVarCounts() {
        localVarCount = 0;
        exprCount = 0;
    }
    
    public void defineAllocArray() {
        println("func AllocArray(size)");
        indent++;
        println("bytes = MulS(size 4)");
        println("bytes = Add(bytes 4)");
        println("v = HeapAllocZ(bytes)");
        println("[v] = size");
        println("ret v");
        indent--;
    }
    
    public void printVMT(String className) {
        ClassTable ct = symbolTables.classTables.get(className);
        println("const vmt_" + className);
        indent++;
        for (String methodName : ct.methods.keySet()) {
            println(":" + className + "." + methodName);
        }
        indent--;
        println("");
    }
    
    public void print(String out) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  "); // each indent is 2 spaces long
        }
        System.out.print(out);
    }
    
    public void println(String out) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  "); // each indent is 2 spaces long
        }
        System.out.println(out);
    }
    
    public void printlnAssign(String varName, PrimaryExpression assignment) {
        Node choice = assignment.f0.choice;
        //do inline assignment if there are no nested expressions
        if (choice instanceof Identifier) {
          String name = ((Identifier)choice).f0.toString();
          if (definedVars.containsKey(name)) {
              println(varName + " = " + definedVars.get(name));
          } else {
                assignment.accept(this);
                println(varName + " = " + prevExpressionResult());
          }
        } else if (choice instanceof IntegerLiteral
                || choice instanceof TrueLiteral
                || choice instanceof FalseLiteral
                || choice instanceof Identifier
                || choice instanceof ThisExpression
                || choice instanceof AllocationExpression) {
            assignment.accept(this);
            println(varName + " = " + prevExpressionResult());
        } else {
            //evaluate nested expressions and assign var to result of final evaluation
            String result = expressionResult();
            assignment.accept(this);
            println(varName + " = " + result);
        }
    }
}
