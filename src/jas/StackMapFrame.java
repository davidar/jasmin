/**
 * StackMapFrame are part of StackMap attributes
 * @author $Author: Daniel Reynaud $
 * @version $Revision: 1.0 $
 */

package jas;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;

public class StackMapFrame
{
  private Vector stack, locals;
  private int offset;
  private Label off_label;

  public StackMapFrame()
  { stack = new Vector();
    locals = new Vector();
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void setOffset(Label label) {
    off_label = label;
  }

  public void addStackItem(String item, String val) throws jasError {
    stack.add(new VerificationTypeInfo(item, val));
  }

  public void addLocalsItem(String item, String val) throws jasError {
    locals.add(new VerificationTypeInfo(item, val));
  }

  public boolean isEmpty()
  { return stack.isEmpty() && locals.isEmpty(); }

  void resolve(ClassEnv e)
  {
    Enumeration en = stack.elements();
    while(en.hasMoreElements())
      ((VerificationTypeInfo)en.nextElement()).resolve(e);

    en = locals.elements();
    while(en.hasMoreElements())
      ((VerificationTypeInfo)en.nextElement()).resolve(e);
  }

  void write(ClassEnv e, CodeAttr ce, DataOutputStream out)
    throws IOException, jasError
  {
    if(off_label!=null) off_label.writeOffset(ce, null, out);
    else out.writeShort(offset);    // offset
    out.writeShort(locals.size());  // number_of_locals
  //  System.out.println("number of local items "+locals.size());
    Enumeration en = locals.elements();
    while(en.hasMoreElements())
      ((VerificationTypeInfo)en.nextElement()).write(e, ce, out);

    out.writeShort(stack.size());  // number_of_stack_items
  //  System.out.println("number of stack items "+stack.size());
    en = stack.elements();
    while(en.hasMoreElements())
      ((VerificationTypeInfo)en.nextElement()).write(e, ce, out);
  }
}

