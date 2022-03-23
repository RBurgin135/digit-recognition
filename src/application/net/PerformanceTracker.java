package application.net;
import java.awt.Graphics;
/**
 * keeps track of how the net performs on the data
 */
public class PerformanceTracker {
    private Graphics g;
    private int guessJustGone;
    private int correctJustGone;
    private int totalGuesses;
    private int correctGuesses;
    private double percentageCorrect;
    private final int[] guessTally;

    public PerformanceTracker(){
        totalGuesses = 0;
        correctGuesses = 0;
        guessTally = new int[]{0,0,0,0,0,0,0,0,0,0};
    }

    /**
     * records the performance of the net in the most recent round of testing
     * @param guess what the net guessed was the answer
     * @param correctAnswer the correct answer
     */
    public void recordPerformance(int guess, int correctAnswer){
        guessJustGone = guess;
        correctJustGone = correctAnswer;
        totalGuesses++;
        guessTally[guess]++;
        if(guess == correctAnswer){
            correctGuesses++;
        }
        percentageCorrect = ((double)correctGuesses/(double)totalGuesses)*100;
    }

    /**
     * shows the performance of the net to the user
     */
    public void outputPerformance(){
        System.out.println("\n\nPERFORMANCE:");
        System.out.println("Most recent guess: "+guessJustGone);
        System.out.println("Most recent correct answer: "+correctJustGone);
        System.out.println("Correct guesses: "+correctGuesses);
        System.out.println("Total guesses: "+totalGuesses);
        System.out.println("Percentage correct: "+percentageCorrect);
        System.out.println("Tally of guesses: "+tallyToString());
    }

    public int getCorrectJustGone(){
        return correctJustGone;
    }

    private String tallyToString(){
        StringBuilder sb = new StringBuilder();
        for(int i : guessTally){
            sb.append("\n\t").append(i).append(": ").append(i);
        }
        return sb.toString();
    }
}
