/**
 * This attribute is used to embed extended debug information
 * @author $Author: Daniel Reynaud $
 * @version $Revision: 1.0 $
 */

package jas;

import java.io.*;

public class SourceDebugExtensionAttr
{
  static CP attr = new AsciiCP("SourceDebugExtension");

  String debug;

  /**
   * Create a source file attribute.
   * @param name Name of the source file
   * @see ClassEnv#setSource
   */

  public SourceDebugExtensionAttr(String debug)
  { this.debug = debug; }

  void resolve(ClassEnv e)
  { e.addCPItem(attr); }

  void write(ClassEnv e, DataOutputStream out)
    throws IOException, jasError
  {
    out.writeShort(e.getCPIndex(attr));

    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream dat = new DataOutputStream(buf);
    dat.writeUTF(debug);
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));

// need to write the length of the attribute first
    out.writeInt(2+in.readUnsignedShort());
// and then write the actual utf8 string
    buf.writeTo(out);
  }
}

