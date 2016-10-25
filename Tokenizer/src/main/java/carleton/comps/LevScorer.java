package carleton.comps;

import org.antlr.v4.runtime.Token;

import java.util.List;

public class LevScorer implements SimilarityScorer{

    /**
     * Uses a Levenstein distance calculation algorith in order to clalculate
     * the edit distance between two strings
     * @param queryString
     * @param tokens
     * @return
     */
    @Override
    public List<Integer> rankSimilarity(List<Token> queryString, List<List<Token>> tokens) {
        //Integer[][] scores = new int[queryString.size()][tokens]
        return null;
    }

    /**
     * Uses a Levenstein distance calculation algorith in order to clalculate
     * the edit distance between two strings
     * @param queryString
     * @param tokens
     * @return
     */
    //@Override
    public static int rankSimilarity(List<Token> queryString, List<Token>
            tokens) {
        if (queryString.size() == 0 || tokens.size() == 0) return 0;

        int[][] scores = new int[queryString.size() + 1][tokens.size() + 1];
        for (int i = 1; i < scores.length; i++) {
            for (int j = 1; j < scores[0].length; j++) {
                int match = scores[i-1][j-1];
                if (queryString.get(i-1).getType() == tokens.get(j-1).getType
                        ()) match++;
                int left = scores[i-1][j];
                int right = scores [i][j-1];
                int max = Math.max(match, Math.max(left, right));
                scores[i][j] = max;
            }
        }
        return scores[scores.length][scores[0].length];
    }
}
