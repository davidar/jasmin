/**
 * StackMap attributes are part of the CLDC verification process
 * @author $Author: Daniel Reynaud $
 * @version $Revision: 1.0 $
 */

package jas;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;

public class StackMapAttr
{
  static CP attr = new AsciiCP("StackMap");
  private Vector frames;


  public StackMapAttr()
  { frames = new Vector(); }

  public void addFrame(StackMapFrame f) {
    frames.add(f);
  }

  public int size(ClassEnv e, CodeAttr ce) {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream bufout = new DataOutputStream(buf);

    // not fully compliant to the spec !
    try {
      bufout.writeShort(frames.size());
      Enumeration en = frames.elements();
      while(en.hasMoreElements())
        ((StackMapFrame)en.nextElement()).write(e, ce, bufout);
    } catch(IOException ex) {
      System.err.println("UNEXPECTED IO EXCEPTION");
      ex.printStackTrace();
    } catch(jasError ex) {
      System.err.println("UNEXPECTED JAS ERROR");
      ex.printStackTrace();
    }

    return 6+buf.toByteArray().length;
  }

  void resolve(ClassEnv e)
  { e.addCPItem(attr);

    Enumeration en = frames.elements();
    while(en.hasMoreElements())
      ((StackMapFrame)en.nextElement()).resolve(e);
  }

  void write(ClassEnv e, CodeAttr ce, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeShort(e.getCPIndex(attr));

    // writing to a buffer first, so that we can print the length
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream bufout = new DataOutputStream(buf);

    // not fully compliant to the spec !
    bufout.writeShort(frames.size());
    Enumeration en = frames.elements();
    while(en.hasMoreElements())
      ((StackMapFrame)en.nextElement()).write(e, ce, bufout);

    // length
    out.writeInt(buf.toByteArray().length);
    buf.writeTo(out);
  }
}

