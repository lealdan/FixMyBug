package server;

import java.sql.ResultSet;
import java.sql.SQLException;
//import javax.swing.JOptionPane;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteJDBCLoader;
import server.javaparser.LevScorer;

//to create database
import java.sql.*;
import java.util.*;

public class DatabaseServer {

    SQLiteDataSource dataSource;
    String tableName;

    // Default database operation variables
    public static final int DEFAULT_NGRAM_SIZE = 4;
    public static final String DATABASE_TABLE_NAME = "test_tokens";
    public static final int MIN_SIMILAR_TO_PULL = 4;
    public static final double DEFAULT_PERCENT_TO_PULL = 0.5;
    public static final int DEFAULT_USER_RETURN = 2;

    // The table entries
    public static final String[] DATABASE_TABLE_FORMAT = {"id", "buggyCode",
            "fixedCode"};

    public DatabaseServer(String fileName) {
        String url = "jdbc:sqlite:" + fileName;
        System.out.println("Making new database, url = " + url);
        tableName = "master_table";
        boolean initialize = false;
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

    public static void createNewDatabase(String fileName) {
        String url = "jdbc:sqlite:./" + fileName;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is: " + meta.getDriverName());

                System.out.println("A new database has been created.");
            }

        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public final String SelectFrom(String column, String inputStr) {
        return SelectFrom(column, inputStr, tableName);
    }


    public final String SelectFrom(String column, String inputStr, String table) {

        try {
            System.out.println("Input String: " + inputStr + ".");
            ResultSet rs = dataSource.getConnection().createStatement()
                    .executeQuery("select " + column + " from \"" + table + "\" where buggy_code = \"" + inputStr + "\";");

            String result = rs.getString(column);
            if(result == null) {
                System.out.println("No results found\n");
                return "";
            }
            return result;
        }
        catch (Exception ex) { //SQLException ex) {
            System.out.println(ex.getMessage());
            return "";
        }
    }

    public final DatabaseEntry SelectAll(String inputStr) {
        DatabaseEntry queryResult = new DatabaseEntry();

        try {
            System.out.println("Input String: " + inputStr + ".");
            ResultSet rs = dataSource.getConnection()
                    .createStatement().executeQuery("select * from \"" + tableName + "\" where buggy_code = \"" + inputStr + "\";");

            rs.next();
            if (rs.isAfterLast()) {//ID starts at 1, so 0 marks a null return value (i.e.
            // no results)
                System.out.println("No results found\n");
                return queryResult;
            }

            queryResult = new DatabaseEntry(rs);


            //Display values
            /*System.out.print("ID: " + id);
            System.out.print(", Bug Type: " + bug_type);
            System.out.print(", Buggy: '" + buggy_code + "'");
            System.out.print("Sending Fixed Code: '" + fixed_code + "'");
            System.out.println(", Count: " + count);
            String result = "ID: " + id + ", Buggy Code: " + buggy_code +
                          ", Fixed Code: " + fixed_code + ", Count: " + count;
            return queryResult;*/
        }
        catch (Exception ex) { //SQLException ex) {
            System.out.println(ex.getMessage());
            return queryResult;
        }
        return queryResult;
    }

    public final void Insert(int id, int bug_type, String buggy_code, String fixed_code, int count) {
        try {
            int rs = dataSource.getConnection().createStatement()
                .executeUpdate("INSERT INTO \"" + tableName + "\" VALUES ("
                + id + ", " + bug_type + ", \"" + buggy_code + "\", \"" + fixed_code + "\", " + count + ");");
            System.out.println("Changes: " + rs);
        }
        catch (Exception ex) { //SQLException ex) {
            System.out.println(ex.getMessage());
        }

    }

    public final void RemoveByID(int id) {
        try {
            int rs = dataSource.getConnection().createStatement()
                .executeUpdate("DELETE FROM \"" + tableName + "\" WHERE id="
                        + id + ";");
            System.out.println("Changes: " + rs);
        }
        catch (Exception ex) { //SQLException ex) {
            System.out.println(ex.getMessage());
        }

    }

    public final void RemoveByBug(String bug) {
        try {
            int rs = dataSource.getConnection().createStatement()
                .executeUpdate("DELETE FROM \"" + tableName + "\" WHERE buggy_code="
                + bug + ";");
            System.out.println("Changes: " + rs);
        }
        catch (Exception ex) { //SQLException ex) {
            System.out.println(ex.getMessage());
        }

    }




    public void createIndex(int ngramsize, String table) throws SQLException {
        // Delete the existing index

        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        Statement statement2 = connection.createStatement();
        statement.executeUpdate("DROP TABLE" +
                " IF EXISTS "+ table+"_"+ngramsize+"gramindex;");

        // Create new table
        statement.executeUpdate("CREATE TABLE " + table + "_"
                + ngramsize + "gramindex (id INTEGER, hash INTEGER);");

        ResultSet rows = statement.executeQuery("SELECT * from "
                + table);

        while (true) {
            System.out.println(rows.getRow());
            if (rows.next()) {
                System.out.print(rows.getRow());
                System.out.println("!");
                int id = rows.getInt("id");
                String errCode = rows.getString("error_tokens");
                String[] tokens = errCode.split(" ");

                // populate the the array with each ngram
                for (int i = ngramsize; i <= tokens.length; i++) {
                    StringBuilder b = new StringBuilder();
                    for (int j = i - ngramsize; j < i; j++) {
                        b.append(tokens[j] + " ");
                    }
                    int hash = b.toString().hashCode();
                    statement2.executeUpdate("INSERT INTO "+ table + "_"
                    + ngramsize + "gramindex VALUES ("+id+","+hash+");");
                }
            }
            else break;

        }
        System.out.println(rows.getRow());
        connection.close();

    }

    /**
     * Search that querys the database and returns a list with an Entry pair
     * containing the id of every occurance in the Table of each ngram from
     * the query list sorted by occurance.
     * @param query
     * @param ngramsize
     * @param table
     * @return
     * @throws SQLException
     */
    public List<Map.Entry<Integer, Integer>> querySearch(String query, int ngramsize, String table) throws SQLException {
        //TODO make sure this test works for finding emtpy tables
        String indexTableName = table + "_" + ngramsize + "gramindex";
        Map<Integer, Integer> masterRow = new HashMap<>();

//        if (dataSource.getConnection().createStatement().executeQuery("IF " +
//                        "EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES"
//                "WHERE TABLE_NAME = N'" + table + "')" +
//        "BEGIN" +
//        "PRINT 'Table Exists'" +
//        "END))").get

        String[] qTokens = query.split(" ");
        for (int i = ngramsize; i <= qTokens.length; i++) {
            StringBuilder b = new StringBuilder();
            for (int j = i - ngramsize; j < i; j++) {
                b.append(qTokens[j] + " ");
            }
            System.out.println("qTokens String: " + b.toString());
            int hash = b.toString().hashCode();
            System.out.println(hash);

            //
            ResultSet ngramSet = dataSource.getConnection().createStatement()
                    .executeQuery("SELECT id FROM " + indexTableName + " " +
                            "WHERE hash = " + hash);

            // Grab every in in result set and put it into the map
            while (ngramSet.next()) {
                int masterid = ngramSet.getInt("id");
                masterRow.put(masterid, (masterRow.containsKey(masterid)?
                        masterRow.get(masterid):0) + 1);
            }
        }

        // Sort the map and return a list of integers sorted
        List<Map.Entry<Integer,Integer>> results = new ArrayList(masterRow.entrySet
                ());
        Collections.sort(results, (Map.Entry o1, Map.Entry o2) -> (int)o2
                .getValue() - (int)o1.getValue()
        );
        return results;
    }

    // TODO figure out what this returns in the grand scheme of things

    /**
     * Method that uses default values of the database server in order to grab
     * all ngram similar code in the database and filter it to return only
     * the most similar code to the user for transmission to the client
     * @param userQuery
     * @return
     * @throws SQLException
     */
    public List<String> getMostSimilarEntries(String userQuery) throws SQLException {
        List<Map.Entry<Integer, Integer>> sortedQuery = querySearch
                (userQuery, DEFAULT_NGRAM_SIZE, DATABASE_TABLE_NAME);
        System.out.println(sortedQuery);

        List<Integer> rowsToPull = new ArrayList<>();
        for (int i = 0; (i < MIN_SIMILAR_TO_PULL || i <
                DEFAULT_PERCENT_TO_PULL*sortedQuery.size()) && i < sortedQuery.size();
             i++) {
            System.out.println("i: " + i);
            rowsToPull.add(sortedQuery.get(i).getKey());
        }

        StringJoiner j = new StringJoiner(",", "SELECT * FROM " +
                ""+DATABASE_TABLE_NAME+" WHERE id IN (", ");");
        for (int i : rowsToPull) {
            j.add(Integer.toString(i));
        }
        System.out.println(j.toString());
        ResultSet shortList = dataSource.getConnection().createStatement()
                .executeQuery(j.toString());

        List<DatabaseEntry> entryList = new ArrayList<>();
        System.out.println(shortList.getRow());
        while(shortList.next()) {
                System.out.println(shortList.getRow());
                System.out.println("making a database entry");
                entryList.add(new DatabaseEntry(shortList));

        }

        // Sorting the rows by a secondary algorithm
        for (DatabaseEntry e : entryList) {
            e.setSimilarity( computeSecondarySimilarity(userQuery, e));
            System.out.println("ID: " + e.getId() + "  " + e.getSimilarity());
        }
        Collections.sort(entryList, (DatabaseEntry a, DatabaseEntry b) -> (int)(b.getSimilarity() - a.getSimilarity()));

        List<String> output = new ArrayList<>();
        for (DatabaseEntry e : entryList) output.add(e.toString());
        return output;
    }

    private double computeSecondarySimilarity(String userQuery, DatabaseEntry entry) {
        List<Integer> q = new ArrayList<>();
        List<Integer> e = new ArrayList<>();

        for (String s : userQuery.split(" ")) { q.add(Integer.parseInt(s)); }
        for (String s : entry.getBuggyCode().split(" ")) { e.add(Integer.parseInt(s)); }
        return LevScorer.scoreSimilarity(q, e);
    }


    public static void main(String[] args) {
        DatabaseServer db = new DatabaseServer("/FixMyBugDB/TEST_DATABASE");
        try {
            db.createIndex(Integer.parseInt("4"), "test_tokens");
            System.out.println(db.getMostSimilarEntries("100 100 100 100 100 110 100 100 100 60"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    //     //String fix1 = db.SelectFrom("fixed_code","somebuggg");
    //     //System.out.println("somefffix - " + fix1);
    //     //String fix2 = db.SelectAll("somebug2");
    //     //System.out.println(fix2);
    //     //db.RemoveByID(4);
    //     //db.RemoveByID(5);
    //     //db.RemoveByID(6);
    //     //addToTable(args[0], 6, 5, "somebuggg", "somefffix", 2);
    //     //addToTable(args[0], 4, 2, "somebug2", "somefix2", 2);
    //     //addToTable(args[0], 5, 4, "somebug3", "somefix3", 2);
    }

}
