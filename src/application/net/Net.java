package application.net;

import java.util.concurrent.ThreadLocalRandom;

public class Net {
    //meta
    private final PerformanceTracker tracker;
    private final int     batchDepth; //how many tests to complete a batch
    private int            batchProg; //how many tests through the current batch have been executed

    //net architecture
    private final int     noOfLayers; //number of layers/depth of the neural net
    private final int[]    noOfUnits; //width of each of the layers
    private final int[]  noOfWeights; //number of weights for each layer

    //net composition
    private final double[][][]      w; //w[layer][unit][destination unit]: matrix holding all the weights for the units in the net
    private final double[][]        z; //z[layer][unit]: matrix with the result of the calculations, which when applied to a sigmoid result in the activation
    private final double[][]        a; //a[layer][unit]: matrix holding all the activations of the net in the most recent round of testing
    private final double[][]        b; //b[layer][unit]: the bias for each unit
    private final int[]             y; //y[output unit]: the correct results the net should have outputted

    //net performance assessment and adjustment
    private final double[][][][]    n; //N[layer][unit][weight][nudge]: matrix holding all the nudges, which will then be averaged to edit the values of the weights
    private final double[][]        e; //e[layer][unit]: matrix containing the error values for the units, to be used when recurring in back propagation
    private double                  c; //C: cost of the net, value of how well the net performed

    /**
     * constructor
     */
    public Net() {
        //meta
        tracker = new PerformanceTracker();
        batchDepth = 1000;
        batchProg = 0;

        //net architecture
        noOfLayers = 4;
        noOfUnits = new int[]{784,16,16,10};
        noOfWeights = new int[]{1,784,16,16};

        //net composition + performance and adjustment
        w = new double[noOfLayers][][];
        z = new double[noOfLayers][];
        a = new double[noOfLayers][];
        b = new double[noOfLayers][];
        e = new double[noOfLayers][];
        n = new double[noOfLayers][][][];
        for(int i=0; i<noOfLayers; i++){
            w[i] = new double[noOfUnits[i]][noOfWeights[i]];
            z[i] = new double[noOfUnits[i]];
            a[i] = new double[noOfUnits[i]];
            b[i] = new double[noOfUnits[i]];
            e[i] = new double[noOfUnits[i]];
            n[i] = new double[noOfUnits[i]][noOfWeights[i]][batchDepth];
        }
        y = new int[noOfUnits[noOfLayers-1]];
        c = 0;
        initialiseWeights();
        //TODO: add bias
    }

    /**
     * initialises the weights for the net by giving each a random value between 1 and -1
     */
    private void initialiseWeights(){
        for(int l=0; l<noOfLayers; l++){
            for(int u=0; u<noOfUnits[l]; u++){
                for(int d=0; d<noOfWeights[l]; d++){
                    w[l][u][d] = ThreadLocalRandom.current().nextDouble(-1, 1);
                }
            }
        }
    }

    /**
     * handles the execution of the test
     * @param input the inputs to be fed into the net
     * @param currentAnswer the correct answer to the test
     */
    public void executeTest(double[] input, int currentAnswer){
        int guess = forProp(input);
        tracker.recordPerformance(guess, currentAnswer);
        batchProg++;
        tracker.outputPerformance();
    }

    /**
     * forward propagation; method responsible for getting a result from the net from given inputs
     * @param input inputs to be fed into the net
     * @return the guess the net has made
     */
    private int forProp(double[] input){
        a[0] = input;
        for(int l = 1; l< noOfLayers; l++){
            for(int u = 0; u< noOfUnits[l]; u++){
                z[l][u] = activate(w[l][u], a[l-1]);
                a[l][u] = sigmoid(z[l][u]);
            }
        }
        return findOutput();
    }

    /**
     * finds the activation of the current unit
     * @param weights the weights of the current unit
     * @param activations the activations of the previous layer
     * @return the activation of the current unit
     */
    private double activate(double[] weights, double[] activations){
        return sum(multiply(weights, activations));
    }

    /**
     * multiplies the weights and activations together
     * @param weights the weights for the current unit
     * @param activations the activations of the previous layer
     * @return the product
     */
    private double[] multiply(double[] weights, double[] activations){
        double[] result = new double[weights.length];
        for(int i=0; i<weights.length; i++){
            result[i] = weights[i] * activations[i];
        }
        return result;
    }

    /**
     * sums all the elements in an array
     * @param array the array to be summed
     * @return the product
     */
    private double sum(double[] array){
        double result=0;
        for(double x : array){
            result += x;
        }
        return result;
    }

    /**
     * the sigmoid activation function
     */
    private double sigmoid(double x){
        return (1/( 1 + Math.pow(Math.E,(-1*x))));
    }

    /**
     * finds the final output of the net, by finding the highest activation in the
     * output layer
     * @return the integer guess for this round of testing
     */
    private int findOutput(){
        double[] outputLayerActivations = a[3];
        int maxIndex=0;
        double max=0.0;
        for(int i=0; i<outputLayerActivations.length; i++){
            if(max < outputLayerActivations[i]){
                max = outputLayerActivations[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * looks over the net after the most recent round of testing
     */
    public void reviewTest(){
        System.out.println("BACK PROP");
        backProp();
        if(endOfBatchQuery()){
            System.out.println("---END OF BATCH---");
            endOfBatchReview();
        }
    }

    /**
     * back propagates through the net; assesses and records its performance
     */
    private void backProp(){
        findY();
        assessPerformance();
        for(int l=noOfLayers-2; l>0; l--){
            for(int u=0; u<noOfUnits[l]; u++){
                relayErrors(l, u);
                recordWeightNudges(l, u);
            }
        }
    }

    /**
     * finds the values for y for use when finding the error values for the output layer
     */
    private void findY(){
        for(int i=0; i<noOfUnits[noOfLayers-1]; i++) y[i]=0;
        int correctGuess = tracker.getCorrectJustGone();
        y[correctGuess] = 1;
    }

    /**
     * finds the cost of the net, and the error of the output layer,
     * which illustrates the performance of the net
     */
    private void assessPerformance(){
        int outputLayer = noOfLayers-1;
        for(int u=0; u<noOfUnits[outputLayer]; u++){
            double result = y[u] - a[outputLayer][u];
            e[outputLayer][u] = result;
            c+= Math.pow(-result, 2);
            recordWeightNudges(outputLayer, u);
        }
        //TODO: record cost
    }

    /**
     * calculates the error value for the unit by iterating through the units in the layer after the
     * current unit
     * @param currentLayer the layer of the unit whose error value is currently being calculated
     * @param u the unit whose error value is to be calculated
     */
    private void relayErrors(int currentLayer, int u){
        int relayLayer = currentLayer +1;
        double EC = 0.01; //error coefficient
        double[] relay = new double[noOfUnits[relayLayer]];

        for(int r=0; r<noOfUnits[relayLayer]; r++){
            double proportion; //finds if the weight connecting the units is negative
            if(w[relayLayer][r][currentLayer] < 0)  proportion = a[currentLayer][u];
            else                                    proportion = 1- a[currentLayer][u];

            int sign; //if the unit's activation needs to decrease flip the sign
            if(e[currentLayer][u] < 0)    sign = -1;
            else                          sign = 1;

            relay[r] = sign * proportion * w[relayLayer][r][u] * EC;
        }
        e[currentLayer][u] = average(relay);
    }

    /**
     * records all the changes/nudges that need to be averaged and then committed to each weight
     * at the end of the batch
     * @param currentLayer the layer of the unit which is having its nudges calculated
     * @param u the unit whose nudges are being calculated
     */
    private void recordWeightNudges(int currentLayer, int u){
        double WNC = 0.01; //weight nudge coefficient
        int prevLayer = currentLayer-1;
        for(int p=0; p<noOfUnits[prevLayer]; p++) {
            double proportion; //finds if the activation in the previous layer is bright
            if (a[prevLayer][p] > 0.5)  proportion = a[prevLayer][p] - 0.5;
            else                        proportion = 0.5 - a[prevLayer][p];

            int sign; //if the unit activation needs to decrease flip the sign
            if(e[currentLayer][u] < 0)  sign = -1;
            else                        sign = 1;

            n[currentLayer][u][p][batchProg-1] = sign * proportion * WNC;
        }
    }

    /**
     * finds the average value across an array
     * @param x the array
     * @return the average
     */
    private double average(double[] x){
        return sum(x) / x.length;
    }

    /**
     * checks if the batch has ended
     * @return true if the batch is at its end
     */
    private boolean endOfBatchQuery(){
        return batchProg == batchDepth;
    }

    /**
     * initiates actions to be done at the end of a batch of testing
     */
    private void endOfBatchReview(){
        commitNudges();
        batchProg = 0;
    }

    /**
     * takes all the nudges found, averages them and then changes the weights by the product
     */
    private void commitNudges(){
        for(int l=1; l<noOfLayers; l++){
            for(int u=0; u<noOfUnits[l]; u++){
                for(int d=0; d<noOfWeights[l]; d++){
                    double[] nudges = n[l][u][d];
                    double result = average(nudges);
                    w[l][u][d] += result;
                }
            }
        }
    }
}
