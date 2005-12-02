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
  CP name, desc;
  SignatureAttr sig;
  ConstAttr const_attr;

  /**
   * @param vacc access permissions for the field
   * @param name name of the field
   * @param desc type of the field
   * @param cattr Extra constant value information. Passing this as
   * null will not include this information for the record.
   * @see RuntimeConstants
   */
  public Var(short vacc, CP name, CP desc, ConstAttr cattr)
  {
    this(vacc, name, desc, null, cattr);
  }


  /**
   * @param vacc access permissions for the field
   * @param name name of the field
   * @param desc type of the field
   * @param sig  signature of the field (can be null)
   * @param cattr Extra constant value information. Passing this as
   * null will not include this information for the record.
   * @see RuntimeConstants
   */
  public Var(short vacc, CP name, CP desc, String sig, ConstAttr cattr)
  {
    var_acc = vacc; this.name = name;
    this.desc = desc; const_attr = cattr;
    if(sig!=null)
        this.sig = new SignatureAttr(sig);
  }

  void resolve(ClassEnv e)
  {
    e.addCPItem(name);
    e.addCPItem(desc);
    if (const_attr != null)
      { const_attr.resolve(e); }
    if(sig!=null)
        sig.resolve(e);
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeShort(var_acc);
    out.writeShort(e.getCPIndex(name));
    out.writeShort(e.getCPIndex(desc));
    int nb = 0;
    
    if (const_attr != null)
      nb++;
    if(sig != null)
      nb++;
    out.writeShort(nb);
    if (const_attr != null)
      const_attr.write(e, out);
    if(sig != null)
      sig.write(e, out);
  }
}
