/* --- Copyright Jonathan Meyer 1996. All rights reserved. -----------------
 > File:        jasmin/src/jasmin/ReservedWords.java
 > Purpose:     Reserved words for Jasmin
 > Author:      Jonathan Meyer, 10 July 1996
 */

package jasmin;

import java.util.Hashtable;
import java_cup.runtime.*;

abstract class ReservedWords {
    static Hashtable reserved_words;

    public static token get(String name) {
    	return (token)reserved_words.get(name);
    }

    public static boolean contains(String name) {
    	return reserved_words.get(name) != null;
    }

    //
    // scanner initializer - sets up reserved_words table
    //
    static {
        reserved_words = new Hashtable();

        // Jasmin directives
        reserved_words.put(".catch", new token(sym.DCATCH));
        reserved_words.put(".class", new token(sym.DCLASS));
        reserved_words.put(".end", new token(sym.DEND));
        reserved_words.put(".field", new token(sym.DFIELD));
        reserved_words.put(".implements", new token(sym.DIMPLEMENTS));
        reserved_words.put(".interface", new token(sym.DINTERFACE));
        reserved_words.put(".limit", new token(sym.DLIMIT));
        reserved_words.put(".line", new token(sym.DLINE));
        reserved_words.put(".method", new token(sym.DMETHOD));
        reserved_words.put(".set", new token(sym.DSET));
        reserved_words.put(".source", new token(sym.DSOURCE));
        reserved_words.put(".super", new token(sym.DSUPER));
        reserved_words.put(".throws", new token(sym.DTHROWS));
        reserved_words.put(".var", new token(sym.DVAR));

        // reserved_words used in Jasmin directives
        reserved_words.put("from", new token(sym.FROM));
        reserved_words.put("method", new token(sym.METHOD));
        reserved_words.put("to", new token(sym.TO));
        reserved_words.put("is", new token(sym.IS));
        reserved_words.put("using", new token(sym.USING));

        // Special-case instructions
        reserved_words.put("tableswitch", new token(sym.TABLESWITCH));
        reserved_words.put("lookupswitch", new token(sym.LOOKUPSWITCH));
        reserved_words.put("default", new token(sym.DEFAULT));

        // Access flags
        reserved_words.put("public", new token(sym.PUBLIC));
        reserved_words.put("private", new token(sym.PRIVATE));
        reserved_words.put("protected", new token(sym.PROTECTED));
        reserved_words.put("static", new token(sym.STATIC));
        reserved_words.put("final", new token(sym.FINAL));
        reserved_words.put("synchronized", new token(sym.SYNCHRONIZED));
        reserved_words.put("volatile", new token(sym.VOLATILE));
        reserved_words.put("transient", new token(sym.TRANSIENT));
        reserved_words.put("native", new token(sym.NATIVE));
        reserved_words.put("interface", new token(sym.INTERFACE));
        reserved_words.put("abstract", new token(sym.ABSTRACT));
    }
}
