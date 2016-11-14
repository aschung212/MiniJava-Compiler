package hw3;

import syntaxtree.*;
import visitor.*;

public class J2V {
    
    public static void main(String[] args) {
        Goal goal = null;
        try{
            goal = new MiniJavaParser(System.in).Goal();
        } catch (ParseException ex) {
            System.out.println(ex.toString());
            System.exit(-1);
        }
        
        SymbolTables symbolTables = new SymbolTables();
        symbolTables.visit(goal);
        
        VaporConverter vaporConverter = new VaporConverter(symbolTables);
        vaporConverter.visit(goal);
    }
}
