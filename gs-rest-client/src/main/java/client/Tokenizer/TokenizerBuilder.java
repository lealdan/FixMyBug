package client.Tokenizer;

import client.Tokenizer.javaparser.JavaLexer;
import client.Tokenizer.javaparser.JavaParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import java.lang.IndexOutOfBoundsException;
import java.lang.IllegalArgumentException;

/**
 * Takes in code as a string or as a file, tokenizes and stores the code.
 *
 * @author Alex Griese
 * @author Jamie Emery
 */
public class TokenizerBuilder {

    // Holds the tokenized lines of code.
    private List<List<Token>> tokenizedLines;

    /*
     * Constructs an instance of the TokenizerBuilder given the code passed in as the code parameter.
     * @Param: Code, holds the code that is to be tokenized.
     * @Param: Type, what type of information the code is stored as.
     */
    public TokenizerBuilder(String code, String type) throws IOException {

        tokenizedLines = new ArrayList<List<Token>>();

        // This if statement is used to tokenize the code according to its type.
        if (type.equals("File")){

            // Checks to see if the file provided is a valid file.
            Scanner scanner;
            try {
                scanner = new Scanner(new File(code));

                // Goes through each line of code and converts it into tokenized code.
                while (scanner.hasNextLine()) {
                    tokenizedLines.add(generateTokens(new JavaLexer(new ANTLRInputStream(scanner.nextLine()))));
                }
            } catch (FileNotFoundException fe) {
                throw new FileNotFoundException("That is not a valid file!");
            }

        } else if (type.equals("String")) {

            // Splits each string by line and then tokenizes it.
            String[] lines = code.split("\n");
            for (String line: lines) {
                tokenizedLines.add(generateTokens(new JavaLexer(new ANTLRInputStream(line))));
            }
        } else {
            throw new IllegalArgumentException("TokenizerBuilder requires a String or File.");
        }
    }

    /*
     * Generates the tokens based on the code in the lexer.
     * @Param: lexedLine, a line of lexed code.
     * @Return: tokenizedLine, the lexed code is now returned as a tokenized line.
     */
    private List<Token> generateTokens(JavaLexer lexedLine) {

        // Converts the lexed line into a token steam and gets the tokens.
        CommonTokenStream stream = new CommonTokenStream(lexedLine);
        List<Token> tokenizedLine = stream.getTokens();

        // Unclear as to why this line is needed, but if it is not called the token stream seemingly returns no tokens
        // and nothing can be printed out.
        stream.getNumberOfOnChannelTokens();

        return tokenizedLine;
    }

    /*
     * Returns the tokenized code as a string.
     * @Return: the string version of the tokenized code.
     */
    public String getString() {

        // StringBuilder makes it easy to concatenate strings together.
        StringBuilder builder = new StringBuilder();

        // Goes through each tokenized line in tokenizedLines and gets the error type, token type and its value.
        // It then adds them to builder.
        for (List<Token> tokenizedLine: tokenizedLines) {
            builder.append(tokensToString(tokenizedLine));
        }
        return builder.toString();
    }

    /*
     * Returns a list of all the tokens.
     * @Return: allTokens, list of all tokens in code.
     */
    public List<Token> getTokens() {

        // Holds all the tokens in the code.
        List<Token> allTokens = new ArrayList<Token>();

        // Goes through each line and token and adds them to allTokens.
        for (List<Token> tokenizedLine: tokenizedLines) {
            for (Token t: tokenizedLine) {
                allTokens.add(t);
            }
        }
        return allTokens;
    }


    /*
     * Takes a list of tokens and returns the corresponding string.
     * @Param: tokens, a list of tokens to be converted into a string.
     * @Return: tokens as a string.
     */
    public static String tokensToString(List<Token> tokens) {
        return tokensToString(tokens, true);
    }

    /*
     * Takes a list of tokens and returns the corresponding string.
     * @Param: tokens, a list of tokens to be converted into a string.
     * @Param: verbose,.
     * @Return: tokens as a string.
     */
    public static String tokensToString(List<Token> tokens, boolean verbose) {

        StringBuilder builder = new StringBuilder();

        // Goes through each token, converts it into a string and adds it to builder.
        for (Token t : tokens) {
            if (verbose) {
                builder.append(t.getType() + " ");
            } else {
                builder.append(t.getType() + " ");
            }
        }

        return builder.toString();
    }

    /*
     * Based on the lines specified by the user via @param start and @param stop, returns the tokenized code.
     * @Param: start, first line of tokenized code to be returned.
     * @Param: stop, last line of tokenized code to be returned.
     * @Return: tokens, all tokens between start line and stop line.
     */
    public List<Token> betweenLines(int start, int stop) {

        if (start < 1 || stop > tokenizedLines.size()) {
            throw new IndexOutOfBoundsException("The lines you specified are out of range.");
        }

        // Holds the current line of tokenized code.
        List<Token> line;

        // Holds all the tokenized code.
        List<Token> tokens = new ArrayList<Token>();

        // Goes through each line of tokenized code specified and adds it to tokens.
        for (int i = start; i<=stop; i++) {
            line = tokenizedLines.get(i);

            for (Token t : line) {
                tokens.add(t);
            }
        }
        return tokens;

    }


    /*
     * Harmonizes the tokenized code. Non-buggy tokenized code should be passed in. Will take this and replace tokens
     * with appropriate variables, syntax, etc. Turns tokenized code into real code.
     * @Param: code, holds the tokenized code.
     * @Return: detokenizedCode, holds the de-tokenized code.
     */
    public String harmonize(String code) {

        // Splits the string by spaces, as these separate the individual tokens.
        String[] tokens = code.split(" ");

        // Holds the detokenized, tokenized code.
        StringBuilder builder = new StringBuilder();


        // De-tokenizes the tokenized code.



        // Converts each token into the appropriate grammatical expression.
        for (String t: tokens) {

            // Turns each token into an int, so we can get its literal name.
            int token = Integer.parseInt(t);

            // If the token is a specific token, gets its literal name.
            if ((token != 100) && ((token < 52) || (token > 57))) {
                builder.append(JavaParser.VOCABULARY.getLiteralName(token));
            }

            // TODO do something better here.
            // If the token is a non-specific token, just places its symbolic name instead.
            else {
                builder.append(JavaParser.VOCABULARY.getSymbolicName(token));
            }
        }


        // Converts builder into an actual string.
        String detokenizedCode = builder.toString();

        return detokenizedCode;
    }
}
