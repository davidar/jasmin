/**
 * This is used to encapsulate a CodeAttr so it can be added
 * into a class.
 * @see ClassEnv#addMethod
 * @author $Author: jonmeyerny $
 * @version $Revision: 1.1 $
 */
package jas;

import java.io.*;

public class Method
{
  short acc;
  CP name, desc;
  CodeAttr code;
  ExceptAttr excepts;
  SignatureAttr sig;

  /**
   * @param macc method access permissions. It is a combination
   * of the constants provided from RuntimeConstants
   * @param name CP item representing name of method.
   * @param desc CP item representing descnature for object
   * @param code The actual code for the object
   * @param ex Any exceptions associated with object
   */
  public Method (short macc, CP name, CP desc, CodeAttr cd, ExceptAttr ex)
  {
    this(macc, name, desc, null, cd, ex);
  }


  /**
   * @param macc method access permissions. It is a combination
   * of the constants provided from RuntimeConstants
   * @param name CP item representing name of method.
   * @param desc CP item representing the descriptor for object
   * @param sig String item representing signature for object (can be null)
   * @param code The actual code for the object
   * @param ex Any exceptions associated with object
   * @param s A StackMap attribute associated with the object (can be null)
   */
  public Method (short macc, CP name, CP desc, SignatureAttr sig,
                 CodeAttr cd, ExceptAttr ex)
  {
    acc = macc;
    this.name = name;
    this.desc = desc;
    this.sig = sig;
    code = cd;
    excepts = ex;
  }

  void resolve(ClassEnv e)
  {
    e.addCPItem(name);
    e.addCPItem(desc);
    if (code != null)  code.resolve(e);
    if (excepts != null)  excepts.resolve(e);
    if (sig != null)  sig.resolve(e);
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    short cnt = 0;
    out.writeShort(acc);
    out.writeShort(e.getCPIndex(name));
    out.writeShort(e.getCPIndex(desc));
    if (code != null) cnt ++;
    if (excepts != null) cnt++;
    if (sig != null) cnt++;
    out.writeShort(cnt);
    if (code != null) code.write(e, out);
    if (excepts != null) excepts.write(e, out);
    if (sig != null) sig.write(e, out);
  }
}
