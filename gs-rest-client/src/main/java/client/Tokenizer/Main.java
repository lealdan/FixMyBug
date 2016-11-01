

package client.Tokenizer;

import ch.qos.logback.core.subst.Token;
import java.util.List;

import java.io.IOException;

/*
 * Checks if the TokenizerBuilder class works.
 * @author Alex Griese
 * @author Jamie Emery
 */

public class Main {

    public static void main(String[] args) {
        try {

            // Testing the file type of the constructor.

            // Creates a new instance of TokenizerBuilder.
            TokenizerBuilder t = new TokenizerBuilder("./Tokenizer/src/main/java/carleton/comps/javaparser/examples/HelloWorld.java","File");


            // Prints the tokenized version of HelloWorld.java.
            //System.out.println(t.getString());

            //List<Token> tokens = t.getTokens();
            //System.out.println(t.tokensToString(tokens));

            // Gets a list of Tokens that come from the HelloWorld.java file between lines 1 and 6.
            //List<Token> l = t.betweenLines(0,4);

            // Prints out the list of Tokens in lines 1 to 6 in the HelloWorld.java file.
            //System.out.println(t.tokensToString(l));

            // Prints out the detokenized, tokenized code.
            //System.out.println(t.harmonize(t.getString()));

            /*========================================================================================================*/


            // Testing the String type of constructor.


            TokenizerBuilder x = new TokenizerBuilder("package carleton.comps.javaparser.examples;\n" +
                    "\n" +
                    "public class HelloWorld {\n" +
                    "   public static void main(String[] args) { \n" +
                    "      System.out.println(\"Hello, World\");\n" +
                    "   }\n" +
                    "}","String");

            // Prints the tokenized version of HelloWorld.java.
            //System.out.println(t.getString());

            //List<Token> tokens = t.getTokens();
            //System.out.println(t.tokensToString(tokens));

            // Gets a list of Tokens that come from the HelloWorld.java file between lines 1 and 6.
            //List<Token> l = t.betweenLines(3,3);

            // Prints out the list of Tokens in lines 1 to 6 in the HelloWorld.java file.
            //System.out.println(t.tokensToString(l));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
