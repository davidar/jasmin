/**
 * Used to make up new field entries. Fields for a class can have
 * an additional "ConstantValue" attribute associated them,
 * which the java compiler uses to represent things like
 * static final int blah = foo;
 *
 * @author $Author: jonmeyerny $
 * @version $Revision: 1.1 $
 */

package jas;

import java.io.*;

public class Var
{
  short var_acc;
  CP name, sig;
  ConstAttr const_attr;

  /**
   * @param vacc access permissions for the field
   * @param name name of the field
   * @param sig type of the field
   * @param cattr Extra constant value information. Passing this as
   * null will not include this information for the record.
   * @see RuntimeConstants
   */

  public Var(short vacc, CP name, CP sig, ConstAttr cattr)
  {
    var_acc = vacc; this.name = name;
    this.sig = sig; const_attr = cattr;
  }

  void resolve(ClassEnv e)
  {
    e.addCPItem(name);
    e.addCPItem(sig);
    if (const_attr != null)
      { const_attr.resolve(e); }
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeShort(var_acc);
    out.writeShort(e.getCPIndex(name));
    out.writeShort(e.getCPIndex(sig));
    if (const_attr != null)
      { out.writeShort(1); const_attr.write(e, out); }
    else
      { out.writeShort(0); }
  }
}
