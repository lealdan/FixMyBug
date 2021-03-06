package Filler;

import Filler.Tokenizer.DBAscii;
import Filler.Tokenizer.EdiToken;
import Filler.Tokenizer.TokenizerBuilder;
import Filler.Tokenizer.javaparser.*;
import org.antlr.v4.runtime.Token;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Class that handles the process of filling the database given input of two files
 */
public class DBFillerInterface {
    public static HashMap<Character,Character> ESCAPE_CHARACTERS = new HashMap<>();
    public static HashMap<Character,Character> SRETCARAHC_EPASCE = new HashMap<>();

    SQLiteDataSource dataSource;
    String tableName;
    public static int currentID = 1;
    private DiffMatchPatch differ = new DiffMatchPatch();

    public static final String DATABASE_TABLE_NAME = "master_table";

    //CONSTANTS ABOUT PROCESSING
    private static int MAX_LINES_TO_GRAB = 4;
    private static int PRECEEDING_LINES = 2;
    private static int TRAILING_LINES = 2;


    // constructior (takes database name as the argument)
    public DBFillerInterface(String fileName) {
        if (ESCAPE_CHARACTERS.isEmpty()) {
            ESCAPE_CHARACTERS.put('\0','0');
            ESCAPE_CHARACTERS.put('\'','\'');
            ESCAPE_CHARACTERS.put('\"','\"');
            ESCAPE_CHARACTERS.put('\b','b');
            ESCAPE_CHARACTERS.put('\n','n');
            ESCAPE_CHARACTERS.put('\r','r');
            ESCAPE_CHARACTERS.put('\t','t');
            ESCAPE_CHARACTERS.put('\\','\\');
            ESCAPE_CHARACTERS.put('%','%');
            ESCAPE_CHARACTERS.put('_','_');
        }
        if (SRETCARAHC_EPASCE.isEmpty()) {
            SRETCARAHC_EPASCE.put('0','\0');
            SRETCARAHC_EPASCE.put('\'','\'');
            SRETCARAHC_EPASCE.put('\"','\"');
            SRETCARAHC_EPASCE.put('b','\b');
            SRETCARAHC_EPASCE.put('n','\n');
            SRETCARAHC_EPASCE.put('r','\r');
            SRETCARAHC_EPASCE.put('t','\t');
            SRETCARAHC_EPASCE.put('\\','\\');
            SRETCARAHC_EPASCE.put('%','%');
            SRETCARAHC_EPASCE.put('_','_');
        }

        String url = "jdbc:sqlite:" + fileName;
        tableName = DATABASE_TABLE_NAME;
        boolean initialize = false;

        //try to connect to the DB
        try {
            initialize = SQLiteJDBCLoader.initialize();
            if (!initialize) throw new Exception("SQLite Library Not Loaded\n");
            dataSource = new SQLiteDataSource();
            dataSource.setUrl(url);
        }
        catch (Exception e) {
            System.out.println("Exception caught during database setup: \n");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Main externally visible method that digests two files with err code data and uploads the
     * resulting generated database entry into the database
     *
     * @param file1 text of file containing the eroneous code
     * @param file2 text of file containing the fixed code
     * @param messageLine the line number of the error message
     * @return
     */
    public boolean uploadToDatabase(String file1, String file2, int messageLine) {
        //TODO here is where a check goes for valid line syntax

        LinkedList<DiffMatchPatch.Diff> edits = differ.diffMain(file1, file2);
        differ.diffCleanupSemantic(edits);

        // Forgetting about files that have excessivley complicated diff profiles
        if (edits.size()>15) {
            return false;
        }


        // Processing the list so that the lines are clear
        List<DiffFinderHelper> processed = new ArrayList<>();
        int errline = 1;
        int fixline = 1;
        int candidateFixIndex = -1;
        for (DiffMatchPatch.Diff d: edits) {
            // Finding the first edit before the change error message;
            if (errline <= messageLine+1) {
                if (d.operation != DiffMatchPatch.Operation.EQUAL) {
                    candidateFixIndex = processed.size();
                }
            }

            processed.add(new DiffFinderHelper(errline, fixline, d));
            int i = d.text.split("\r\n|\r|\n", -1).length - 1;
            if (d.operation == DiffMatchPatch.Operation.EQUAL) {
                errline += i;
                fixline += i;
            } else if (d.operation == DiffMatchPatch.Operation.DELETE) {
                errline += i;
            } else {
                fixline += i;
            }
        }
        // Logic to find lines
        int errLineStart = processed.get(candidateFixIndex).errStartLine;
        int fixLineStart = processed.get(candidateFixIndex).fixStartLine;
        int errLinesRemaining = MAX_LINES_TO_GRAB;
        int fixLinesRemaining = MAX_LINES_TO_GRAB;

        //System.out.println("errline: " + errLineStart);
        //System.out.println("fixline: " + fixLineStart);

        // If the diff is not the end of the file
        if (candidateFixIndex+1!=processed.size()) {
            errLinesRemaining = MAX_LINES_TO_GRAB - (processed.get(candidateFixIndex+1).errStartLine
                    - processed.get(candidateFixIndex).errStartLine) - 1;

            // Currently not used in processing
            fixLinesRemaining = MAX_LINES_TO_GRAB - (processed.get(candidateFixIndex+1).fixStartLine
                    - processed.get(candidateFixIndex).fixStartLine) - 1;
        }
        // If it is the last line of the file TODO
        else {
            DiffFinderHelper diff = processed.get(processed.size()-1);
            int newlines = diff.diff.text.split("\r\n|\r|\n", -1).length-1;
            errLinesRemaining = MAX_LINES_TO_GRAB - (diff.diff.operation== DiffMatchPatch
                    .Operation.DELETE?newlines:0) -1;
            fixLinesRemaining = MAX_LINES_TO_GRAB - (diff.diff.operation== DiffMatchPatch
                    .Operation.INSERT?newlines:0) -1;
        }


        int errLineEnd = errLineStart + MAX_LINES_TO_GRAB - errLinesRemaining - 1 + TRAILING_LINES;
        int fixLineEnd = fixLineStart + MAX_LINES_TO_GRAB - fixLinesRemaining - 1 + TRAILING_LINES;
        int curIndex = candidateFixIndex;

        // Loop through until we eat our entire fix line allowence on a deletion
        while (curIndex > 0 && errLinesRemaining > 0) {
            curIndex--;
            DiffFinderHelper h = processed.get(curIndex);
            if (h.diff.operation== DiffMatchPatch.Operation.EQUAL) {
                //IF an equals eats all of our allowence, don't add any lines
                if(errLineStart - h.errStartLine > errLinesRemaining) {
                    errLineStart = errLineStart - PRECEEDING_LINES;
                    fixLineStart = fixLineStart - PRECEEDING_LINES;
                    break;
                }

            } else {
                // Case where it eats all the remaining lines
                if (errLineStart - h.errStartLine >= errLinesRemaining)  {
                    errLineStart = errLineStart - errLinesRemaining - PRECEEDING_LINES;
                    fixLineStart = h.fixStartLine - PRECEEDING_LINES;
                    break;

                // case where it doesnt
                } else {
                    errLinesRemaining = errLinesRemaining - (errLineStart - h.errStartLine);
                    errLineStart = h.errStartLine;
                    fixLineStart = h.fixStartLine;
                }
            }
        }

        System.out.println("Trying to split files, errlineStart: " + errLineStart + "  errLineEnd: " + errLineEnd + "  fixLineStart: "+fixLineStart+"   fixLineEnd: "+fixLineEnd);
        Insert(createDatabaseEntry(file1, file2, errLineStart, errLineEnd, fixLineStart, fixLineEnd));
        return true;
    }

    private class DiffFinderHelper {
        int errStartLine;
        int fixStartLine;
        DiffMatchPatch.Diff diff;

        public DiffFinderHelper(int errStartLine, int fixStartLine, DiffMatchPatch.Diff d) {
            this.errStartLine = errStartLine;
            this.fixStartLine = fixStartLine;
            this.diff = d;
        }
    }


    /**
     * Insert method that takes in parameters that match the master_table columns
     * This method then connects to the database and adds the data to the master_table
     */
    public final void Insert(DatabaseEntry entry) {
        try {
            entry.escape();
            //System.out.println("filling to database: " + entry.toString());
            Connection connection = dataSource.getConnection();
            int rs = connection.createStatement()
                    .executeUpdate("INSERT INTO \"" + tableName + "\" VALUES ("
                            + currentID++ + ", \"" + entry.getBuggyCode() + "\" , \"" + entry
                            .getBuggyCodeAssignments() + "\", \"" + entry.getFixedCode() + "\", \"" +
                            entry.getFixedCodeAssignments() + "\");");
            /*System.out.println("INSERT INTO \"" + tableName + "\" VALUES ("
                    + currentID++ + ", \"" + entry.getBuggyCode() + "\" , \"" + entry
                    .getBuggyCodeAssignments() + "\", \"" + entry.getFixedCode() + "\", \"" +
                    entry.getFixedCodeAssignments() + "\");");*/
                    sanitizeForJsonTransmission(entry);
                    System.out.println(entry);
                    connection.close();
        } catch (Exception ex) { //SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }


    /**
     * Method that takes two files and associated lines and builds a database entry to send
     */
    public DatabaseEntry createDatabaseEntry(String errCode, String fixCode,
                                             int errStartLine, int errEndLine, int fixStartLine,
                                             int fixEndLine) {
        try {
            // Ensure lines are valid?
            // SOME CODE

            // Reading each file into tokens
            List<EdiToken> errFileTokens = (new TokenizerBuilder(errCode, "String")).getTokens();
            List<EdiToken> fixFileTokens = (new TokenizerBuilder(fixCode, "String")).getTokens();

            List<Integer> errFileAssignments = new ArrayList<Integer>(errFileTokens.size());
            List<Integer> fixFileAssignments = new ArrayList<Integer>(fixFileTokens.size());

            //Dictionary that holds the string token assignments
            HashMap<String, Integer> ambigousAssignments = new HashMap<String, Integer>();

            int assignedVariables = 0;

            // Assigning disamibuation to tokens of the err file
            for (EdiToken t: errFileTokens) {
                if (TokenizerBuilder.isAmbiguousToken(t)) {
                    if (ambigousAssignments.containsKey(t.getText())) {
                        errFileAssignments.add(ambigousAssignments.get(t.getText()));
                    } else {
                        ambigousAssignments.put(t.getText(), ++assignedVariables);
                        errFileAssignments.add(assignedVariables);
                    }
                }
                else {
                    errFileAssignments.add(0);
                }
            }

            // Assigning disamibuation to tokens of the err file
            for (EdiToken t: fixFileTokens) {
                if (TokenizerBuilder.isAmbiguousToken(t)) {
                    if (ambigousAssignments.containsKey(t.getText())) {
                        fixFileAssignments.add(ambigousAssignments.get(t.getText()));
                    } else {
                        ambigousAssignments.put(t.getText(), ++assignedVariables);
                        fixFileAssignments.add(assignedVariables);
                    }
                }
                else {
                    fixFileAssignments.add(0);
                }
            }

            // Prune the lists to only account for the specified lines
            int errTokenStartIndex = 0;
            int errTokenEndIndex = -1;
            int fixTokenStartIndex = 0;
            int fixTokenEndIndex = -1;

            boolean inWindow = false;
            for (int i = 0; i < errFileTokens.size(); i++) {
                if (!inWindow && errFileTokens.get(i).getLine()>=errStartLine) {
                    errTokenStartIndex = i;
                    inWindow = true;
                } else if (inWindow && errFileTokens.get(i).getLine()>errEndLine){
                    errTokenEndIndex = i -1;
                    break;
                }
            }
            if (errTokenEndIndex < 1) {
                errTokenEndIndex = errFileTokens.size()-1;
            }

            // Determining which caracters in the tokens are in the target lines
            inWindow = false;
            for (int i = 0; i < fixFileTokens.size(); i++) {
                if (!inWindow && fixFileTokens.get(i).getLine()>=fixStartLine) {
                    fixTokenStartIndex = i;
                    inWindow = true;
                } else if (inWindow && fixFileTokens.get(i).getLine()>fixEndLine){
                    fixTokenEndIndex = i -1;
                    break;
                }
            }
            if (fixTokenEndIndex < 1) {
                fixTokenEndIndex = fixFileTokens.size()-1;
            }

            /*System.out.println("File A: " + errCode);
            System.out.println("A Tokens: " + errFileTokens);
            System.out.println("A File Assignments: " + errFileAssignments);

            System.out.println("\n\n" + ambigousAssignments.toString() + "\n\n");


            System.out.println("File B: " + fixCode);
            System.out.println("B Tokens: " + fixFileTokens);
            System.out.println("B File Assignments: " + fixFileAssignments);

            // Pruning everything to the correct lines
            System.out.println("File A Grabbed Lines: " + errFileTokens.subList(errTokenStartIndex,
                            errTokenEndIndex));
            System.out.println("File B Grabbed Lines: " + fixFileTokens.subList(fixTokenStartIndex,
                    fixTokenEndIndex));*/
            String buggy_code = DBAscii.tokensToAsciiFormat(errFileTokens.subList
                    (errTokenStartIndex, errTokenEndIndex));
            String buggy_code_assignnments = DBAscii.toAsciiFormat(errFileAssignments.subList(
                    errTokenStartIndex,errTokenEndIndex));

            String fix_code = DBAscii.tokensToAsciiFormat(fixFileTokens.subList(fixTokenStartIndex,
                    fixTokenEndIndex));
            String fix_code_assignnments = DBAscii.toAsciiFormat(fixFileAssignments.subList(
                    fixTokenStartIndex,fixTokenEndIndex));

            // Conversion and building the thing
            // TODO: NOTE, ID PRODUCED HERE IS BOGUS FOR THE TIME BEING
            DatabaseEntry out = new DatabaseEntry(0,buggy_code,buggy_code_assignnments,
                    fix_code,fix_code_assignnments);

            return out;

        }
        catch (IllegalArgumentException e) {
            System.out.println("Something went wrong with the tokenizer");
        }
        catch (IOException e) {
            System.out.println("Could not read file");
        }
        return null;
    }

    /**
     * Mehtod that builds a database index table of the supllied table argument in the string
     * based on the Ngrams for error_code in the given table. The size of the Ngrams is determined
     * by the ngramSize argument.
     *
     * @return count of the total number of table rows that were created in the index
     */
    public int createIndex(int ngramsize, String table) throws SQLException {
        // Delete the existing index

        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        Statement statement2 = connection.createStatement();
        statement.executeUpdate("DROP TABLE" +
                " IF EXISTS " + table + "_" + ngramsize + "gramindex;");

        // Create new table
        statement.executeUpdate("CREATE TABLE " + table + "_"
                + ngramsize + "gramindex (id INTEGER, hash INTEGER);");

        ResultSet rows = statement.executeQuery("SELECT * from "
                + table);

        // Iterate through the master table to find ngrams
        int counter = 0;
        while (true) {
            if (rows.next()) {
                int id = rows.getInt("id");
                String errCode = DatabaseEntry.unescapeString(rows.getString("buggy_code"));

                // populate the the array with each ngram
                for (int i = ngramsize; i <= errCode.length(); i++) {
                    StringBuilder b = new StringBuilder();
                    for (int j = i - ngramsize; j < i; j++) {
                        b.append(errCode.charAt(j));
                    }
                    int hash = b.toString().hashCode();
                    counter++;
                    statement2.executeUpdate("INSERT INTO " + table + "_"
                            + ngramsize + "gramindex VALUES (" + id + "," + hash + ");");
                }
            } else break;

        }
        connection.close();
        return counter;
    }


    public static DatabaseEntry sanitizeForJsonTransmission(DatabaseEntry e) {
            e.setBuggyCode(e.buggyCodeAsList().stream().map(Object::toString).collect
                    (Collectors.joining(" ")).toString());
            e.setFixedCode(e.fixedCodeAsList().stream().map(Object::toString).collect
                    (Collectors.joining(" ")).toString());
            e.setBuggyCodeAssignments(e.buggyAssignmentsAsList().stream().map(Object::toString).collect
                    (Collectors.joining(" ")).toString());
            e.setFixedCodeAssignments(e.fixedAssignmentsAsList().stream().map
                    (Object::toString).collect(Collectors.joining(" ")).toString());


        return e;
    }
}
