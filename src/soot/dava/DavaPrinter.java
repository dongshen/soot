/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Jerome Miecznikowski
 * Copyright (C) 2004-2005 Nomair A. Naeem
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package soot.dava;

import soot.dava.internal.AST.*;

import soot.*;
import java.util.*;
import soot.util.*;
import soot.jimple.*;
import soot.tagkit.*;
import java.io.*;
import soot.options.Options;

public class DavaPrinter {
    public DavaPrinter(Singletons.Global g) {
    }
    public static DavaPrinter v() {
        return G.v().soot_dava_DavaPrinter();
    }

    /** Prints the given <code>JimpleBody</code> to the specified <code>PrintWriter</code>. 
	
        Nomair A Naeem 10-MARCH-2005
	Method has become obsolete. Locals are now printed from within the ASTMethodNode.
	This allows for any analyses to be carried out on these locals during the
	many transformations on the AST
    **/
    /*
      private void printLocalsInBody(Body body, java.io.PrintWriter out) {
      if ((body instanceof DavaBody) == false)
      throw new RuntimeException("Only DavaBodies should use the DavaLocalPrinter");
      
      DavaBody davaBody = (DavaBody) body;
      
      // Print out local variables
      {
      Map typeToLocals =
      new DeterministicHashMap(body.getLocalCount() * 2 + 1, 0.7f);
      
      HashSet params = new HashSet();
      params.addAll(davaBody.get_ParamMap().values());
      params.addAll(davaBody.get_CaughtRefs());
      HashSet thisLocals = davaBody.get_ThisLocals();
      
      // Collect locals
      {
      Iterator localIt = body.getLocals().iterator();
      
      while (localIt.hasNext()) {
      Local local = (Local) localIt.next();
      
      if (params.contains(local) || thisLocals.contains(local))
      continue;
      
      List localList;
      
      String typeName;
      Type t = local.getType();
      
      typeName = t.toString();
      
      if (typeToLocals.containsKey(typeName))
      localList = (List) typeToLocals.get(typeName);
      else {
      localList = new ArrayList();
      typeToLocals.put(typeName, localList);
      }
      
      localList.add(local);
      }
      }
      
      InstanceInvokeExpr constructorExpr = davaBody.get_ConstructorExpr();
      if (constructorExpr != null) {
      
      if (davaBody.getMethod().getDeclaringClass().getName().equals(
      constructorExpr.getMethodRef().declaringClass().toString()))
      out.print("        this(");
      else
      out.print("        super(");
      
      Iterator ait = constructorExpr.getArgs().iterator();
      while (ait.hasNext()) {
      out.print(ait.next().toString());
      
      if (ait.hasNext())
      out.print(", ");
      }
      
      out.print(");\n\n");
      }
      
      // Print locals
      {
      Iterator typeIt = typeToLocals.keySet().iterator();
      
      while (typeIt.hasNext()) {
      String type = (String) typeIt.next();
      
      List localList = (List) typeToLocals.get(type);
      Object[] locals = localList.toArray();
      out.print("        ");
      if (type.equals("null_type"))
      out.print("Object");
      else
      out.print(type);
      out.print(" ");
      
      for (int k = 0; k < locals.length; k++) {
      if (k != 0)
      out.print(", ");
      
      out.print(((Local) locals[k]).getName());
      }
      
      out.println(";");
      }
      }
      
      if (!typeToLocals.isEmpty())
      out.println();
      }
      }
    */
    private void printStatementsInBody(Body body, java.io.PrintWriter out) {
        Chain units = ((DavaBody) body).getUnits();

        if (units.size() != 1) {
            throw new RuntimeException("DavaBody AST doesn't have single root.");
        }

        UnitPrinter up = new DavaUnitPrinter();
        ((ASTNode) units.getFirst()).toString(up);
        out.print( up.toString() );
    }

    public void printTo(SootClass cl, PrintWriter out) {
        {

            String curPackage = cl.getJavaPackageName();

            if (curPackage.equals("") == false) {
                out.println("package " + curPackage + ";");
                out.println();
            }

            IterableSet packagesUsed = new IterableSet();

            if (cl.hasSuperclass()) {
                SootClass superClass = cl.getSuperclass();
                packagesUsed.add(superClass.getJavaPackageName());
            }

            Iterator interfaceIt = cl.getInterfaces().iterator();
            while (interfaceIt.hasNext()) {
                String interfacePackage =
                    ((SootClass) interfaceIt.next()).getJavaPackageName();
                if (packagesUsed.contains(interfacePackage) == false)
                    packagesUsed.add(interfacePackage);
            }

            Iterator methodIt = cl.methodIterator();
            while (methodIt.hasNext()) {
                SootMethod dm = (SootMethod) methodIt.next();

                if (dm.hasActiveBody())
                    packagesUsed =
                        packagesUsed.union(
                            ((DavaBody) dm.getActiveBody()).get_PackagesUsed());

                Iterator eit = dm.getExceptions().iterator();
                while (eit.hasNext()) {
                    String thrownPackage =
                        ((SootClass) eit.next()).getJavaPackageName();
                    if (packagesUsed.contains(thrownPackage) == false)
                        packagesUsed.add(thrownPackage);
                }

                Iterator pit = dm.getParameterTypes().iterator();
                while (pit.hasNext()) {
                    Type t = (Type) pit.next();

                    if (t instanceof RefType) {
                        String paramPackage =
                            ((RefType) t).getSootClass().getJavaPackageName();
                        if (packagesUsed.contains(paramPackage) == false)
                            packagesUsed.add(paramPackage);
                    }
                }

                Type t = dm.getReturnType();
                if (t instanceof RefType) {
                    String returnPackage =
                        ((RefType) t).getSootClass().getJavaPackageName();
                    if (packagesUsed.contains(returnPackage) == false)
                        packagesUsed.add(returnPackage);
                }
            }

            Iterator fieldIt = cl.getFields().iterator();
            while (fieldIt.hasNext()) {
                SootField f = (SootField) fieldIt.next();

                if (f.isPhantom())
                    continue;

                Type t = f.getType();

                if (t instanceof RefType) {
                    String fieldPackage =
                        ((RefType) t).getSootClass().getJavaPackageName();
                    if (packagesUsed.contains(fieldPackage) == false)
                        packagesUsed.add(fieldPackage);
                }
            }

            if (packagesUsed.contains(curPackage))
                packagesUsed.remove(curPackage);

            if (packagesUsed.contains("java.lang"))
                packagesUsed.remove("java.lang");

            Iterator pit = packagesUsed.iterator();
            while (pit.hasNext())
                out.println("import " + (String) pit.next() + ".*;");

            if (packagesUsed.isEmpty() == false)
                out.println();

            packagesUsed.add("java.lang");
            packagesUsed.add(curPackage);

            Dava.v().set_CurrentPackageContext(packagesUsed);
            Dava.v().set_CurrentPackage(curPackage);
        }

        // Print class name + modifiers
        {
            String classPrefix = "";

            classPrefix =
                classPrefix + " " + Modifier.toString(cl.getModifiers());
            classPrefix = classPrefix.trim();

            if (!cl.isInterface()) {
                classPrefix = classPrefix + " class";
                classPrefix = classPrefix.trim();
            }

            out.print(classPrefix + " " + cl.getShortJavaStyleName());
        }

        // Print extension
        if (cl.hasSuperclass()
            && !(cl.getSuperclass().getName().equals("java.lang.Object")))
            out.print(" extends " + cl.getSuperclass().getName() + "");

        // Print interfaces
        {
            Iterator interfaceIt = cl.getInterfaces().iterator();

            if (interfaceIt.hasNext()) {
                if( cl.isInterface() ) out.print(" extends ");
                else out.print(" implements ");

                out.print("" + ((SootClass) interfaceIt.next()).getName() + "");

                while (interfaceIt.hasNext())
                    out.print(
                        ", " + ((SootClass) interfaceIt.next()).getName() + "");
            }
        }

        out.println();
        out.println("{");

        // Print fields
        {
            Iterator fieldIt = cl.getFields().iterator();
            if (fieldIt.hasNext()) {
                while (fieldIt.hasNext()) {
                    SootField f = (SootField) fieldIt.next();

                    if (f.isPhantom())
                        continue;

		    if( f.isFinal() && f.isStatic() ){
			Type fieldType = f.getType();
			if(fieldType instanceof DoubleType && f.hasTag("DoubleConstantValueTag")){
			    double val = ((DoubleConstantValueTag)f.getTag("DoubleConstantValueTag")).getDoubleValue();
			    out.println("    " + f.getDeclaration() + " = " + val + ";");
			}
			else if (fieldType instanceof FloatType && f.hasTag("FloatConstantValueTag")){
			    float val = ((FloatConstantValueTag)f.getTag("FloatConstantValueTag")).getFloatValue();
			    out.println("    " + f.getDeclaration() + " = " + val + "f;");
			}
			else if (fieldType instanceof LongType && f.hasTag("LongConstantValueTag")){
			    long val = ((LongConstantValueTag)f.getTag("LongConstantValueTag")).getLongValue();
			    out.println("    " + f.getDeclaration() + " = " + val + "l;");
			}
			else if (fieldType instanceof CharType && f.hasTag("IntegerConstantValueTag")){
			    int val = ((IntegerConstantValueTag)f.getTag("IntegerConstantValueTag")).getIntValue();
			    out.println("    " + f.getDeclaration() + " = '" + ((char)val) + "';");
			}
			else if (fieldType instanceof BooleanType && f.hasTag("IntegerConstantValueTag")){
			    int val = ((IntegerConstantValueTag)f.getTag("IntegerConstantValueTag")).getIntValue();
			    if (val ==0)
				out.println("    " + f.getDeclaration() + " = false;");
			    else
				out.println("    " + f.getDeclaration() + " = true;");
			}
			else if ( (fieldType instanceof IntType || fieldType instanceof ByteType || fieldType instanceof ShortType) && 
				  f.hasTag("IntegerConstantValueTag")){
			    int val = ((IntegerConstantValueTag)f.getTag("IntegerConstantValueTag")).getIntValue();
			    out.println("    " + f.getDeclaration() + " = " + val + ";");
			}
			else if(f.hasTag("StringConstantValueTag")){
			    String val = ((StringConstantValueTag)f.getTag("StringConstantValueTag")).getStringValue();
			    out.println("    " + f.getDeclaration() + " = \"" + val + "\";");
			}
			else{
			    //System.out.println("Couldnt find type of field"+f.getDeclaration());
			    out.println("    " + f.getDeclaration() + ";");
			}
		    } //field is static final
		    else{
			out.println("    " + f.getDeclaration() + ";");
		    }
                }
            }
        }

        // Print methods
        {
            Iterator methodIt = cl.methodIterator();

            if (methodIt.hasNext()) {
                if (cl.getMethodCount() != 0)
                    out.println();

                while (methodIt.hasNext()) {
                    SootMethod method = (SootMethod) methodIt.next();

                    if (method.isPhantom())
                        continue;

                    if (!Modifier.isAbstract(method.getModifiers())
                        && !Modifier.isNative(method.getModifiers())) {
                        if (!method.hasActiveBody())
                            throw new RuntimeException(
                                "method "
                                    + method.getName()
                                    + " has no active body!");
                        else
                            printTo(method.getActiveBody(), out);

                        if (methodIt.hasNext())
                            out.println();
                    } else {
                        out.print("    ");
                        out.print(method.getDavaDeclaration());
                        out.println(";");

                        if (methodIt.hasNext())
                            out.println();
                    }
                }
            }
        }
        out.println("}");
    }

    /**
     *   Prints out the method corresponding to b Body, (declaration and body),
     *   in the textual format corresponding to the IR used to encode b body.
     *
     *   @param out a PrintWriter instance to print to.
     */
    private void printTo(Body b, PrintWriter out) {
        b.validate();

        String decl = b.getMethod().getDavaDeclaration();

        {
            out.println("    " + decl);
            for( Iterator tIt = b.getMethod().getTags().iterator(); tIt.hasNext(); ) {
                final Tag t = (Tag) tIt.next();
                if (Options.v().print_tags_in_output()){
                    out.println(t);
                }
            }
            out.println("    {");


	    /*
	      The locals are now printed out from within the toString method of ASTMethodNode
	      Nomair A Naeem 10-MARCH-2005
	    */
            //printLocalsInBody(b, out);
        }

        printStatementsInBody(b, out);

        out.println("    }");

    }

}
