/* --- Copyright Jonathan Meyer 1997. All rights reserved. -----------------
 > File:        jasmin/src/jasmin/ClassFile.java
 > Purpose:     Uses a parser and the JAS package to create Java class files
 > Author:      Jonathan Meyer, 10 July 1996
 */

package jasmin;


/*
 * This class is a bit monolithic, and should probably be converted into
 * several smaller classes. However, for this specific application,
 * its acceptable.
 *
 */

import jas.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * A ClassFile object is used to represent the binary data that makes up a
 * Java class file - it also serves as the public
 * API to the Jasmin assembler, though users should beware: the API
 * is likely to change in future versions (though its such a small API
 * at the moment that changes are likely to have only a small impact).<p>
 *
 * To assemble a file, you first construct a jasmin.ClassFile object, then
 * call readJasmin() to read in the contents of a Jasmin assembly file, then
 * call write() to write out the binary representation of the class file.<p>
 *
 * There are a few other utility methods as well. See Main.java for an example
 * which uses all of the public methods provided in ClassFile.
 *
 * @author Jonathan Meyer
 * @version 1.05, 8 Feb 1997
 */
public class ClassFile {

    // state info for the class being built
    String filename;
    ClassEnv class_env;
    String class_name;
    String source_name;
    Scanner scanner;

    // state info for the current method being defined
    String method_name;
    String method_signature;
    short  method_access;
    ExceptAttr except_attr;
    Catchtable catch_table;
    LocalVarTableAttr var_table;
    LineTableAttr line_table;
    CodeAttr  code;
    Hashtable labels;

    int line_label_count, line_num;
    boolean auto_number;

    // state info for lookupswitch and tableswitch instructions
    Vector switch_vec;
    int low_value;
    int high_value;


    static final String BGN_METHOD = "bgnmethod:";
    static final String END_METHOD = "endmethod:";

    // number of errors reported in a file.
    int errors;

    //
    // Error reporting method
    //
    void report_error(String msg) {
        // Print out filename/linenumber/message
        System.err.print(filename + ":");
        System.err.print(scanner.line_num);
        System.err.println(": " + msg + ".");
        if (scanner.char_num >= 0) {
            System.err.println(scanner.line.toString());

            // Print out where on the line the scanner got to
            int i;
            for (i = 0; i < scanner.char_num; i++) {
                if (scanner.line.charAt(i) == '\t') {
                    System.err.print("\t");
                } else {
                    System.err.print(" ");
                }
            }
            System.err.println("^");
        }
        errors++;
    }

    //
    // called by the .source directive
    //
    void setSource(String name) {
        source_name = name;
    }


    //
    // called by the .class directive
    //
    void setClass(String name, short acc) {
        class_name = name;
        class_env.setClass(new ClassCP(name));
        class_env.setClassAccess(acc);
    }

    //
    // called by the .super directive
    //
    void setSuperClass(String name) {
        class_env.setSuperClass(new ClassCP(name));
    }

    //
    // called by the .implements directive
    //
    void addInterface(String name) {
        class_env.addInterface(new ClassCP(name));
    }


    //
    // called by the .field directive
    //
    void addField(short access, String name,
                                String sig, Object value) {
        if (value == null) {
            // defining a field which doesn't have an initial value

            class_env.addField(new Var(access, new AsciiCP(name),
                new AsciiCP(sig), null));

        } else {
            // defining a field with an initial value...

            // create a constant pool entry for the initial value
            CP cp = null;

            if (value instanceof Integer) {
                cp = new IntegerCP(((Integer)value).intValue());
            } else if (value instanceof Float) {
                cp = new FloatCP(((Float)value).floatValue());
            } else if (value instanceof Double) {
                cp = new DoubleCP(((Double)value).doubleValue());
            } else if (value instanceof Long) {
                cp = new LongCP(((Long)value).longValue());
            } else if (value instanceof String) {
                cp = new StringCP((String)value);
            }

            // add the field
            class_env.addField(new Var(access, new AsciiCP(name),
                               new AsciiCP(sig), new ConstAttr(cp)));
        }
    }

    //
    // called by the .method directive to start the definition for a method
    //
    void newMethod(String name, String signature, int access) {
        // set method state variables
        labels      = new Hashtable();
        method_name = name;
        code        = null;
        except_attr = null;
        catch_table = null;
        var_table   = null;
        line_table  = null;
        line_label_count  = 0;
        method_signature = signature;
        method_access    = (short)access;
    }

    //
    // called by the .end method directive to end the definition for a method
    //
    void endMethod() throws jasError {

        if (code != null) {

            plantLabel(END_METHOD);

            if (catch_table != null) {
                code.setCatchtable(catch_table);
            }

            if (var_table != null) {
                code.setLocalVarTable(var_table);
            }
            if (line_table != null) {
                code.setLineTable(line_table);
            }
        }

        class_env.addMethod(method_access, method_name, method_signature,
                            code, except_attr);

        // clear method state variables
        code        = null;
        labels      = null;
        method_name = null;
        code        = null;
        except_attr = null;
        catch_table = null;
        line_table  = null;
        var_table   = null;
    }

    //
    // plant routines - these use addInsn to add instructions to the
    //                  code for the current method.
    //

    //
    // used for instructions that take no arguments
    //
    void plant(String name) throws jasError {
        InsnInfo insn = InsnInfo.get(name);
        autoNumber();
        if (insn.args.equals("")) {
            _getCode().addInsn(new Insn(insn.opcode));
        } else if (insn.name.equals("wide")) {
            // don't do anything for this one...
        } else {
            throw new jasError("Missing arguments for instruction " + name);
        }
    }

    //
    // used for iinc
    //
    void plant(String name, int v1, int v2) throws jasError {
        autoNumber();
        if (name.equals("iinc")) {
            _getCode().addInsn(new IincInsn(v1, v2));
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // used for instructions that take an integer parameter
    //
    void plant(String name, int val) throws jasError {
        InsnInfo insn = InsnInfo.get(name);
        CodeAttr code = _getCode();
        autoNumber();

        if (insn.args.equals("i")) {
            code.addInsn(new Insn(insn.opcode, val));
        } else if (insn.args.equals("constant")) {
            code.addInsn(new Insn(insn.opcode, new IntegerCP(val)));
        } else if (insn.args.equals("bigconstant")) {
            code.addInsn(new Insn(insn.opcode, new LongCP(val)));
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // used for ldc and other instructions that take a numeric argument
    //
    void plant(String name, Number val) throws jasError {
        InsnInfo insn = InsnInfo.get(name);
        CodeAttr code = _getCode();
        autoNumber();

        if (insn.args.equals("i") && (val instanceof Integer)) {
            code.addInsn(new Insn(insn.opcode, val.intValue()));
        } else if (insn.args.equals("constant")) {
            if (val instanceof Integer || val instanceof Long) {
                code.addInsn(new Insn(insn.opcode,
                             new IntegerCP(val.intValue())));
            } else if (val instanceof Float || val instanceof Double) {
                code.addInsn(new Insn(insn.opcode,
                             new FloatCP(val.floatValue())));
            }
        } else if (insn.args.equals("bigconstant")) {
            if (val instanceof Integer || val instanceof Long) {
                code.addInsn(new Insn(insn.opcode,
                             new LongCP(val.longValue())));
            } else if (val instanceof Float || val instanceof Double) {
                code.addInsn(new Insn(insn.opcode,
                             new DoubleCP(val.doubleValue())));
            }
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // used for ldc <quoted-string>
    //
    void plantString(String name, String val) throws jasError {
        InsnInfo insn = InsnInfo.get(name);
        autoNumber();

        if (insn.args.equals("constant")) {
            _getCode().addInsn(new Insn(insn.opcode, new StringCP(val)));
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // used for invokeinterface and multianewarray
    //
    void plant(String name, String val, int nargs)
            throws jasError
    {
        InsnInfo insn = InsnInfo.get(name);
        CodeAttr code = _getCode();
        autoNumber();

        if (insn.args.equals("interface")) {
            String split[] = ScannerUtils.splitClassMethodSignature(val);
            code.addInsn(new InvokeinterfaceInsn(
                         new InterfaceCP(split[0], split[1],
                         split[2]), nargs));

        } else if (insn.args.equals("marray")) {
            code.addInsn(new MultiarrayInsn(new ClassCP(val), nargs));
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // used for instructions that take a word as a parameter
    // (e.g. branches, newarray, invokemethod)
    //
    void plant(String name, String val) throws jasError {
        InsnInfo insn = InsnInfo.get(name);
        CodeAttr code = _getCode();
        autoNumber();

        if (insn.args.equals("method")) {
            String split[] = ScannerUtils.splitClassMethodSignature(val);
            code.addInsn(new Insn(insn.opcode,
                         new MethodCP(split[0], split[1], split[2])));
        } else if (insn.args.equals("constant")) {
            code.addInsn(new Insn(insn.opcode, new ClassCP(val)));
        } else if (insn.args.equals("atype")) {
            int atype = 0;
            if (val.equals("boolean")) {
                atype = 4;
            } else if (val.equals("char")) {
                atype = 5;
            } else if (val.equals("float")) {
                atype = 6;
            } else if (val.equals("double")) {
                atype = 7;
            } else if (val.equals("byte")) {
                atype = 8;
            } else if (val.equals("short")) {
                atype = 9;
            } else if (val.equals("int")) {
                atype = 10;
            } else if (val.equals("long")) {
                atype = 11;
            } else {
                throw new jasError("Bad array type: " + name);
            }
            code.addInsn(new Insn(insn.opcode, atype));
        } else if (insn.args.equals("label")) {
            code.addInsn(new Insn(insn.opcode, getLabel(val)));
        } else if (insn.args.equals("class")) {
            code.addInsn(new Insn(insn.opcode, new ClassCP(val)));
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // used for instructions that take a field and a signature as parameters
    // (e.g. getstatic, putstatic)
    //
    void plant(String name, String v1, String v2)
            throws jasError
    {
        InsnInfo info = InsnInfo.get(name);
        CodeAttr code = _getCode();
        autoNumber();

        if (info.args.equals("field")) {
            String split[] = ScannerUtils.splitClassField(v1);
            code.addInsn(new Insn(info.opcode,
                         new FieldCP(split[0], split[1], v2)));
        } else {
            throw new jasError("Bad arguments for instruction " + name);
        }
    }

    //
    // Lookupswitch instruction
    //
    void newLookupswitch() throws jasError {
        switch_vec = new Vector();
        autoNumber();
    };

    void addLookupswitch(int val, String label)
            throws jasError {
        switch_vec.addElement(new Integer(val));
        switch_vec.addElement(getLabel(label));
    };

    void endLookupswitch(String deflabel) throws jasError {
        int n = switch_vec.size() >> 1;
        int offsets[] = new int[n];
        Label labels[] = new Label[n];
        Enumeration e = switch_vec.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            offsets[i] = ((Integer)e.nextElement()).intValue();
            labels[i] = (Label)e.nextElement();
            i++;
        }
        _getCode().addInsn(new LookupswitchInsn(getLabel(deflabel),
                          offsets, labels));
    }

    //
    // Tableswitch instruction
    //
    void newTableswitch(int lowval) throws jasError {
        newTableswitch(lowval, -1);
    };

    void newTableswitch(int lowval, int hival) throws jasError {
        switch_vec = new Vector();
        low_value = lowval;
        high_value = hival;
        autoNumber();
    };

    void addTableswitch(String label) throws jasError {
        switch_vec.addElement(getLabel(label));
    };

    void endTableswitch(String deflabel) throws jasError {
        int n = switch_vec.size();
        Label labels[] = new Label[n];
        Enumeration e = switch_vec.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            labels[i] = (Label)e.nextElement();
            i++;
        }
        if (high_value != -1 && (high_value != low_value + n - 1)) {
            report_error("tableswitch - given incorrect value for <high>");

        }
        _getCode().addInsn(new TableswitchInsn(low_value, low_value + n - 1,
                          getLabel(deflabel), labels));

    }


    // Used by the parser to tell ClassFile what the line number
    // for the next statement is. ClassFile's autoNumber mechanism
    // uses this info.
    void setLine(int l) { line_num = l; }

    //
    // If auto_number is true, output debugging line number table
    // for Jasmin assembly instructions.
    //
    void autoNumber() throws jasError {
        if (auto_number) {
            // use the line number of the last token
            addLineInfo(line_num);
        }
    }

    //
    // Label management
    //

    //
    // gets the Label object for a label, creating it if it doesn't exist
    //
    Label getLabel(String name) throws jasError {

        // check that we are inside of a method definition
        if (method_name == null) {
            throw new jasError( "illegal use of label outside of method definition");
        }

        Label lab = (Label)labels.get(name);
        if (lab == null) {
            lab = new Label(name);
            labels.put(name, lab);
        }
        return lab;
    }

    //
    // defines a label
    //
    void plantLabel(String name) throws jasError {
        // unsure what happens if you use a label twice?
        _getCode().addInsn(getLabel(name));
    }

    //
    // used by the .var directive
    //
    void addVar(String startLab, String endLab,
                              String name, String sig, int var_num)
               throws jasError {
        if (startLab == null) {
            startLab = BGN_METHOD;
        }

        if (endLab == null) {
            endLab = END_METHOD;
        }
        Label slab, elab;
        slab = getLabel(startLab);
        elab = getLabel(endLab);

        if (var_table == null) {
            var_table = new LocalVarTableAttr();
        }

        var_table.addEntry(new LocalVarEntry(slab, elab, name, sig, var_num));
    }

    //
    // used by .line directive
    //
    void addLineInfo(int line_num) throws jasError {
        String l = "L:" + (line_label_count++);
        if (line_table == null) {
            line_table = new LineTableAttr();
        }
        plantLabel(l);
        line_table.addEntry(getLabel(l), line_num);
    }

    void addLine(int line_num) throws jasError {
        if (!auto_number) {
            addLineInfo(line_num);
        }
    }

    //
    // used by the .throws directive
    //
    void addThrow(String name) throws jasError {

        // check that we are inside of a method definition
        if (method_name == null) {
            throw new jasError( "illegal use of .throw outside of method definition");
        }

        if (except_attr == null) {
            except_attr = new ExceptAttr();
        }
        except_attr.addException(new ClassCP(name));
    }

    //
    // used by the .catch directive
    //
    void addCatch(String name, String start_lab, String end_lab,
                                String branch_lab) throws jasError {
        ClassCP class_cp;

        // check that we are inside of a method definition
        if (method_name == null) {
            throw new jasError( "illegal use of .catch outside of method definition");
        }

        if (catch_table == null) {
            catch_table = new Catchtable();
        }

        if (name.equals("all")) {
            class_cp = null;
        } else {
            class_cp = new ClassCP(name);
        }

        catch_table.addEntry(getLabel(start_lab), getLabel(end_lab),
                             getLabel(branch_lab), class_cp);
    }

    //
    // used by the .limit stack directive
    //
    void setStackSize(short v) throws jasError {
        _getCode().setStackSize(v);
    }

    //
    // used by the .limit vars directive
    //
    void setVarSize(short v) throws jasError {
        _getCode().setVarSize(v);
    }

    // --- Private stuff ---


    //
    // returns the code block, creating it if it doesn't exist
    //
    CodeAttr _getCode() throws jasError {

        // check that we are inside of a method definition
        if (method_name == null) {
            throw new jasError( "illegal use of instruction outside of method definition");
        }

        if (code == null) {
            code = new CodeAttr();
            plantLabel(BGN_METHOD);
        }

        return (code);
    }


    // PUBLIC API TO JASMIN:
	
    /** Makes a new ClassFile object, used to represent a Java class file.
      * You can then use readJasmin to read in a class file stored in
      * Jasmin assembly format.
      */

    public ClassFile() { }

    /**
      * Parses a Jasmin file, converting it internally into a binary
      * representation.
      * If something goes wrong, this throws one of
      * an IOException, or a jasError, or one of a few other exceptions.
      * I'll tie this down more formally in the next version.
      *
      * @param input is the stream containing the Jasmin assembly code for the
      *        class.
      *
      * @param name is the name of the stream. This name will be
      *        concatenated to error messages printed to System.err.
      *
      * @param numberLines true if you want Jasmin to generate line
      *        numbers automatically, based on the assembly source, or
      *        false if you are using the ".line" directive and don't
      *        want Jasmin to help out.
      */
    public void readJasmin(InputStream input, String name,
                           boolean numberLines)
                   throws IOException, Exception {
        // initialize variables for error reporting
        errors = 0;
        filename = name;
	source_name = name;

        // if numberLines is true, we output LineTableAttr's that indicate what line
        // numbers the Jasmin code falls on.
        auto_number = numberLines;

        // Parse the input file
        class_env = new ClassEnv();

	scanner = new Scanner(input);
        parser parse_obj = new parser(this, scanner);

        if (false) {
            // for debugging
            // parse_obj.debug_parse();
        } else {
            parse_obj.parse();
        }
    }

    /**
     * Returns the number of warnings/errors encountered while parsing a file.
     * 0 if everything went OK.
     */
    public int errorCount() {
        return errors;
    }

    /**
     * Returns the name of the class in the file (i.e. the string given to
     * the .class parameter in Jasmin)
     *
     */
    public String getClassName() {
        return class_name;
    }

    /**
     * Writes the binary data for the class represented by this ClassFile
     * object to the specified
     * output stream, using the Java Class File format. Throws either an
     * IOException or a jasError if something goes wrong.
     */
    public void write(OutputStream outp) throws IOException, jasError {
        class_env.setSource(source_name);
        class_env.write(new DataOutputStream(outp));
    }
};

/* --- Revision History ---------------------------------------------------
--- Jonathan Meyer, April 11 1997 
    Fixed bug where source_name was not being set in class_env.
--- Jonathan Meyer, Mar 1 1997
    Renamed "Jasmin" class "ClassFile".
--- Jonathan Meyer, Feb 8 1997
    Converted to non-static. Made a public API. Split off InsnInfo to a
    separate file.
--- Jonathan Meyer, Oct 1 1996
    Added addInterface method, used by the .implements directive.
--- Jonathan Meyer, July 25 1996
    Added setLine and line_num, to fix problem with autoNumber.
    Added report_error method.
--- Jonathan Meyer, July 24 1996 added version constant.
*/
