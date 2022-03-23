package application.data_management;
import application.data_management.mnist.*;

import java.io.IOException;
import java.util.Random;

/**
 * responsible for holding all the dataset and allocating
 * the data to be tested
 */
public class Data {
    private final MnistMatrix[] dataSet = new MnistDataReader().readData("data/train-images.idx3-ubyte", "data/train-labels.idx1-ubyte");
    private final Random random = new Random();
    private MnistMatrix currentData;
    private int currentAnswer;

    /**
     * constructor
     */
    public Data() throws IOException {

    }

    /**
     * prepares the data for the next test to be taken
     */
    public double[] prepareTest(){
        find();
        double[] inputs = flatten(currentData);
        return inputs;
    }

    /**
     * randomly selects the next test input
     */
    private void find(){
        int i = random.nextInt(6000);
        currentData = dataSet[i];
        currentAnswer = dataSet[i].getLabel();
    }

    /**
     * flattens the matrix of data into a 1 dimensional array
     * to be used as an input for the net
     * @return list of inputs
     */
    private double[] flatten(MnistMatrix data) {
        int width = data.getNumberOfRows();
        int height = data.getNumberOfColumns();
        int flattenLength = width * height;
        double[] inputs = new double[flattenLength];
        int i = 0;
        for(int x=0; x<width; x++){
            for(int y=0; y<height; y++){
                inputs[i] = data.getValue(x,y);
                i++;
            }
        }
        return inputs;
    }

    /**
     * returns the current answer
     * @return value of currentAnswer
     */
    public int getCurrentAnswer(){
        return  currentAnswer;
    }
}
