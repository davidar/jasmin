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
  private Vector<VerificationTypeInfo> stack, locals;
  private int offset;

  public StackMapFrame()
  { stack = new Vector<VerificationTypeInfo>();
    locals = new Vector<VerificationTypeInfo>();
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void addStackItem(String item, String val) throws jasError {
    stack.add(new VerificationTypeInfo(item, val));
  }

  public void addLocalsItem(String item, String val) throws jasError {
    locals.add(new VerificationTypeInfo(item, val));
  }

  void resolve(ClassEnv e)
  {
    Enumeration<VerificationTypeInfo> en = stack.elements();
    while(en.hasMoreElements())
      en.nextElement().resolve(e);

    en = locals.elements();
    while(en.hasMoreElements())
      en.nextElement().resolve(e);
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeShort(offset);         // offset
    out.writeShort(locals.size());  // number_of_locals
  //  System.out.println("number of local items "+locals.size());
    Enumeration<VerificationTypeInfo> en = locals.elements();
    while(en.hasMoreElements())
      en.nextElement().write(e, out);

    out.writeShort(stack.size());  // number_of_stack_items
  //  System.out.println("number of stack items "+stack.size());
    en = stack.elements();
    while(en.hasMoreElements())
      en.nextElement().write(e, out);
  }
}

