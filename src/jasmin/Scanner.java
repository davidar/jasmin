/* --- Copyright Jonathan Meyer 1996. All rights reserved. -----------------
 > File:        jasmin/src/jasmin/Scanner.java
 > Purpose:     Tokenizer for Jasmin
 > Author:      Jonathan Meyer, 10 July 1996
 */

/* Scanner.java - class for tokenizing Jasmin files. This is rather
 * cheap and cheerful.
*/

package jasmin;

import jas.*;
import java_cup.runtime.*;
import java.util.*;
import java.io.InputStream;

class Scanner {
    InputStream inp;

    // single lookahead character
    int next_char;

    // temporary buffer
    char chars[];

    // true if we have not yet emitted a SEP ('\n') token. This is a bit
    // of a hack so to strip out multiple newlines at the start of the file
    // and replace them with a single SEP token. (for some reason I can't
    // write the CUP grammar to accept multiple newlines at the start of the
    // file)
    boolean is_first_sep;

    // Whitespace characters
    static final String WHITESPACE = " \n\t\r";

    // Separator characters
    static final String SEPARATORS = WHITESPACE + ":=";


    // used for error reporting to print out where an error is on the line
    public int line_num, char_num, token_line_num;
    public StringBuffer line;

    // used by the .set directive to define new variables.
    public Hashtable dict = new Hashtable();

    //
    // returns true if a character code is a whitespace character
    //
    protected static boolean whitespace(int c) {
        return (WHITESPACE.indexOf(c) != -1);
    }

    //
    // returns true if a character code is a separator character
    //
    protected static boolean separator(int c) {
        return (SEPARATORS.indexOf(c) != -1);
    }


    //
    // Advanced the input by one character
    //
    protected void advance() throws java.io.IOException
    {
        next_char = inp.read();
        if (next_char == '\n') {
            // a new line
            line_num++;
            char_num = 0;
            line.setLength(0);
        } else {
            line.append((char)next_char);
            char_num++;
        }
    }

    //
    // initialize the scanner
    //
    public Scanner(InputStream i) throws java.io.IOException
    {
	inp = i;
        line_num = 1;
        char_num = 0;
        line = new StringBuffer();
        chars = new char[512];
        is_first_sep = true;
        advance();
    }

    int readOctal(int firstChar) throws java.io.IOException {
        int d1, d2, d3;
        d1 = firstChar;
        advance();
        d2 = next_char;
        advance();
        d3 = next_char;
        return ((d1-'0')&7) * 64 + ((d2-'0')&7) * 8 + ((d3-'0')&7);
    }

    //
    // recognize and return the next complete token
    //
    public token next_token()
                throws java.io.IOException, jasError
    {

        token_line_num = line_num;

        for (;;) {
            switch (next_char) {

            case ';':
                // a comment
                do { advance(); } while (next_char != '\n');

            case '\n':
                // return single SEP token (skip multiple newlines
                // interspersed with whitespace or comments)
                for (;;) {
                    do { advance(); } while (whitespace(next_char));
                    if (next_char == ';') {
                        do { advance(); } while (next_char != '\n');
                    } else {
                        break;
                    }
                }
                if (is_first_sep) {
                    return next_token();
                }
                token_line_num = line_num;
                return new token(sym.SEP);

            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
            case '-':
            case '.':                       // a number
                {
                    int pos = 0;

                    // record that we have found first item
                    is_first_sep = false;

                    chars[0] = (char)next_char;
                    pos++;
                    for (;;) {
                        advance();
                        if (separator(next_char)) {
                            break;
                        }
                        chars[pos] = (char)next_char;
                        pos++;
                    }
                    String str = new String(chars, 0, pos);
                    token tok;

                    // This catches directives like ".method"
                    if ((tok = ReservedWords.get(str)) != null) {
                        return tok;
                    }

                    Number num;
                    try {
                        num = ScannerUtils.convertNumber(str);
                    } catch (NumberFormatException e) {
                        if (chars[0] == '.') {
                            throw new jasError("Unknown directive or badly formed number.");
                        } else {
                            throw new jasError("Badly formatted number");
                        }
                    }

                    if (num instanceof Integer) {
                        return new int_token(sym.Int, num.intValue());
                    } else {
                        return new num_token(sym.Num, num);
                    }
                }

            case '"':           // quoted strings
                {
                    int pos = 0;

                    is_first_sep = false;

                    for (;;) {
                        advance();
                        if (next_char == '\\') {
                            advance();
                            switch (next_char) {
                            case 'n': next_char = '\n'; break;
                            case 'r': next_char = '\r'; break;
                            case 't': next_char = '\t'; break;
                            case 'f': next_char = '\f'; break;
                            case 'b': next_char = '\b'; break;
                            // case 'u': next_char = 'u'; break;
                            case '"': next_char = '"'; break;
                            case '\'': next_char = '\''; break;

                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7':
				next_char = readOctal(next_char);
				break;
                            default:
				throw new jasError("Bad backslash escape sequence");
                            }
                        } else if (next_char == '"') {
                            break;
                        }
                        chars[pos] = (char)next_char;
                        pos++;
                    }
                    advance(); // skip close quote
                    return new str_token(sym.Str, new String(chars, 0, pos));
                }

            case ' ':
            case '\t':
            case '\r':              // whitespace
                advance();
                break;

            case '=':               // EQUALS token
                advance();
                is_first_sep = false;
                return new token(sym.EQ);

            case ':':               // COLON token
                advance();
                is_first_sep = false;
                return new token(sym.COLON);

            case -1:                // EOF token
                is_first_sep = false;
                char_num = -1;
                line.setLength(0);
                return new token(sym.EOF);

            default:
                {
                    // read up until a separatorcharacter

                    int pos = 0;
                    chars[0] = (char)next_char;
                    is_first_sep = false;

                    pos++;
                    for (;;) {
                        advance();
                        if (separator(next_char)) {
                            break;
                        }
                        chars[pos] = (char)next_char;
                        pos++;
                    }

                    // convert the byte array into a String
                    String str = new String(chars, 0, pos);

                    token tok;
                    if ((tok = ReservedWords.get(str)) != null) {
                        // Jasmin keyword or directive
                        return tok;
                    } else if (InsnInfo.contains(str)) {
                        // its a JVM instruction
                        return new str_token(sym.Insn, str);
                    } else if (str.charAt(0) == '$') {
                        // Perform variable substitution
                        Object v;
                        if ((v = dict.get(str.substring(1))) != null) {
                            return ((token)v);
                        }
                    } else {
                        // Unrecognized string token (e.g. a classname)
                        return new str_token(sym.Word, str);
                    }

                } /* default */
            } /* switch */
        } /* for */
    }

};

/* --- Revision History ---------------------------------------------------
--- Jonathan Meyer, Feb 8 1997
    Converted to be non-static
--- Jonathan Meyer, Oct 30 1996
    Added support for more \ escapes in quoted strings (including octals).
--- Jonathan Meyer, Oct 1 1996
    Added .interface and .implements
--- Jonathan Meyer, July 25 1996
    changed IN to IS. Added token_line_num, which is the line number of the
    last token returned by next_token().
--- Jonathan Meyer, July 24 1996 added mods to recognize '\r' as whitespace.
*/
