/**
 * VerificationTypeInfo are used by StackMap attributes
 * @author $Author: Daniel Reynaud $
 * @version $Revision: 1.0 $
 */

package jas;

import java.io.*;

public class VerificationTypeInfo 
{
  private int tag;
  private ClassCP cls;
  private int index;

  public VerificationTypeInfo(String item) throws jasError
  { this(item, null); }

  public VerificationTypeInfo(String item, String val)
    throws jasError {
    if(item.equals("Top"))
        tag = 0;
    else if(item.equals("Integer"))
        tag = 1;
    else if(item.equals("Float"))
        tag = 2;
    else if(item.equals("Long"))
        tag = 4;
    else if(item.equals("Double"))
        tag = 3;
    else if(item.equals("Null"))
        tag = 5;
    else if(item.equals("UninitializedThis"))
        tag = 6;
    else if(item.equals("Object")) {
        tag = 7;
        if(val==null) throw new jasError("Object requires a class name");
        cls = new ClassCP(val);
    }
    else if(item.equals("Uninitialized")) {
        tag = 8;
        try {
            index = Integer.parseInt(val);
        } catch(Exception e) {
            throw new jasError("Uninitialized requires an integer");
        }
    }
    else throw new jasError("Unknown item object : "+item);
  }

  void resolve(ClassEnv e)
  {
    if(cls!=null) {
      cls.resolve(e);
      e.addCPItem(cls);
    }
  }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeByte(tag);
    if(cls!=null)
      out.writeShort(e.getCPIndex(cls));

// the following is not fully compliant to the CLDC spec !
    if(tag==8) { // Uninitialized
      if((index&0xFFFF) == index) // fits as a short
        out.writeShort(index);
      else
        out.writeInt(index);
    }
  }
}

