package de.schiller.math;

import de.schiller.math.Vector;

import java.awt.*;

public class Smooth {

    public static float[][] smooth(float[][] matrix){
        for(int x = 0; x < matrix.length; x++){
            for(int y = 0; y < matrix[0].length; y++){
                matrix[x][y] = calculateSum(matrix, new Point(x, y));

                matrix[x][y] = (matrix[x][y] > 1) ? 1 : matrix[x][y];
            }
        }

        for(int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                matrix[x][y] = (float) (Math.round(matrix[x][y] * 100) / 100.0);

            }
        }


        return matrix;
    }

    private static float calculateSum(float[][] matrix, Point pos){
        int area = 3;
        int fail = 0;
        float sum = 0;

        for(int x = pos.x-area; x <= pos.x+area; x++){
            for(int y = pos.y-area; y <= pos.y+area; y++){
                if(x < 0 || y < 0 || x >= matrix.length || y >= matrix[0].length){
                    fail++;
                    System.out.println(fail);
                    continue;
                }
                sum += matrix[x][y];
            }
        }
        return sum/((area*2+1)*(area*2+1)-fail);
    }
}
