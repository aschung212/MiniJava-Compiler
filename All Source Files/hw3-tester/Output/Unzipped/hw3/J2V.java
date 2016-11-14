import syntaxtree.*;
import visitor.*;

public class J2V {
    
    //testing flags
    public static boolean printClassInfo = false;
    
    public static void main(String[] args) {
        //TODO: remove jhax
        J2V jhax = new J2V();
        Goal goal = null;
        // get parsed AST for input file
        try{
            goal = new MiniJavaParser(System.in).Goal();
        } catch (ParseException ex) {
            System.out.println(ex.toString());
            System.exit(-1);
        }
        
        SymbolTables symbolTables = new SymbolTables();
        symbolTables.visit(goal);
        
        if (printClassInfo) {
            System.out.println("class tables: ");
            for (String className : symbolTables.classTables.keySet()) {
                System.out.println(className + "'s methods:");
                for (String methodName : symbolTables.classTables.get(className).methods.keySet()) {
                    System.out.println("     " + methodName + "()");
                }
                System.out.println(className + "'s fields:");
                for (String fieldName : symbolTables.classTables.get(className).fields.keySet()) {
                    System.out.println("     " + fieldName + " :: " + (jhax.typeToString((Type)symbolTables.classTables.get(className).fields.get(fieldName))));
                }
            }
        }
        
        VaporConverter vaporConverter = new VaporConverter(symbolTables);
        vaporConverter.visit(goal);
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
    
}
