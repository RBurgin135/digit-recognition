package application;

import application.data_management.Data;
import application.net.Net;

import java.io.IOException;

/**
 * responsible for managing the whole application and organising flow
 */
public class Application {
    private Data data;
    private Net net;

    /**
     * main method
     */
    public static void main(String[] args) throws IOException {
        Application app = new Application();
        app.run();
    }

    /**
     * constructor
     */
    public Application () throws IOException {
        data = new Data();
        net = new Net();
    }

    /**
     * method which all other methods evaluate off of
     */
    private void run (){
        double[] inputs;
        int currentAnswer;
        for(int i=0; i<10000; i++){
            inputs = data.prepareTest();
            currentAnswer = data.getCurrentAnswer();

            net.executeTest(inputs, currentAnswer);
            net.reviewTest();
        }
    }
}
