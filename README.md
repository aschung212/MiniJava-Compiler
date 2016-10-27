# MiniJava-Compiler
a compiler for minijava

Typechecker will take a java program as input, lex it into symbol tokens, 
parse it into an abstract syntax tree comprised of these tokens 
(definitions can be found in jtb.out.jj), and then if the syntax tree is 
valid under the language specifications of MiniJava (definitions can be
found in minijava.jj), will typecheck it.


further specifications and annotated explanations of typechecking validity 
in the MiniJava subset of Java can be found in MiniJavaTypeSystem.pdf.

The typechecker is implemented as a depth first visitor.

Typechecker can be run and tested in the following way:

	java Typecheck < TestProgram.java

