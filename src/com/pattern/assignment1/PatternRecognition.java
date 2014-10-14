package com.pattern.assignment1;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.tc33.jheatchart.HeatChart;

public class PatternRecognition {

	static int cols;
	static int rows;	
	static int array[][];
	static int arrayWithSmoothing[][];
	int rowsCount, colsCount;	
	int scaledArray[][];
	
	int finalSkeletonArray[][];
	static boolean iteration = true;
	
	// Enter the file name here
	static String inputFileName = "res/File1.txt";

	public static void main(String args[]) throws FileNotFoundException {
		
		
		FileInputStream file = new FileInputStream(inputFileName);
		
		InputStreamReader reader = new InputStreamReader(file);
		
		Scanner scanner = new Scanner(reader);		
		
		PatternRecognition obj = new PatternRecognition();
		
		// find rows and columns from input file
		int[] rows_cols = obj.findRowColumn(inputFileName);
		
		cols = rows_cols[1];
		rows = rows_cols[0];		
		array = new int[rows][cols];
		arrayWithSmoothing = new int[rows+2][cols+2];
	
		//obj.determineRowsColumns(scanner);
		obj.readFile(scanner);	
		
		obj.addPadding();
		
		// Smooting - removing noise
		obj.smoothing();
		
		// Filling
//		obj.filling();
		
		// Normalization - scaling
		int scaledImageArray[][] = obj.normalization(cols+2, rows+2);
		
		// Center of gravity
		obj.findCenterOfGravity(scaledImageArray);
		
		// Skeletonization using Zhang-Suen Algorithm
		int tempArrayforStepOne[][] = new int[scaledImageArray.length][scaledImageArray[0].length];;
		int skeletonArray[][];
		int count = 0;
		obj.finalSkeletonArray = new int[scaledImageArray.length][scaledImageArray[0].length];
		
		for(int i=0; i < scaledImageArray.length; i++) {
			for(int j=0; j <scaledImageArray[0].length; j++) {
				tempArrayforStepOne[i][j] = scaledImageArray[i][j];
			}
		}
		
		// this loop will continue until the change in the image stops
		while(iteration) {
			count++;
			tempArrayforStepOne = obj.skeletonization(tempArrayforStepOne, "Step1");
			skeletonArray = obj.skeletonization(tempArrayforStepOne, "Step2");				
			
			for(int i=0; i < skeletonArray.length; i++) {
				for(int j=0; j <skeletonArray[0].length; j++) {
					if(obj.finalSkeletonArray[i][j] != skeletonArray[i][j]) {
						iteration = true;
						break;
					}
					else
						iteration = false;
				}
				if(iteration == true)
					break;					
			}
			
			for(int i=0; i < skeletonArray.length; i++) {
				for(int j=0; j <skeletonArray[0].length; j++) {
					obj.finalSkeletonArray[i][j] = skeletonArray[i][j];
				}
			}
		}
				
		
		// make jpg image
		try {
			obj.getImageFromArray(array[0].length, array.length, array, "original");
			obj.getImageFromArray(arrayWithSmoothing[0].length, arrayWithSmoothing.length, arrayWithSmoothing, "smoothing");
			obj.getImageFromArray(scaledImageArray[0].length, scaledImageArray.length, scaledImageArray, "normalized");
			obj.getImageFromArray(obj.finalSkeletonArray[0].length, obj.finalSkeletonArray.length,obj.finalSkeletonArray, "skeleton");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		for(int i=0; i < arrayWithSmoothing.length; i++) {
			for(int j=0; j <arrayWithSmoothing[0].length; j++) {
				System.out.print(arrayWithSmoothing[i][j]);
			}
			System.out.println();
		}
		
		for(int i=0; i < scaledImageArray.length; i++) {
			for(int j=0; j <scaledImageArray[0].length; j++) {
				System.out.print(scaledImageArray[i][j]);
			}
			System.out.println();
		}
		
		for(int i=0; i < obj.finalSkeletonArray.length; i++) {
			for(int j=0; j <obj.finalSkeletonArray[0].length; j++) {
				System.out.print(obj.finalSkeletonArray[i][j]);
			}
			System.out.println();
		}
		
		
		System.out.println("count "+count);
	}
	
	public int[] findRowColumn(String fileName)
	{
		int result[]=new int[2];
		int row=0;
		int col=0;
		try 
	    {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
	        String line = br.readLine();
	        col=line.length();
	        while (line != null) 
	        {
	            row++;
	            if(col<line.length())
	            	col=line.length();
	            line = br.readLine();
	            
	        }
	        br.close();
	     } 
		catch(Exception e)
		{
			System.out.println(e);
		}
		result[0]=row;
		result[1]=(col+1) / 2;
		
		return result; 
	}
	

	private void filling() {
		
		for(int i=1; i < arrayWithSmoothing.length - 1; i++) {
			for(int j=1; j <arrayWithSmoothing[0].length - 1; j++) {
				if(arrayWithSmoothing[i][j] == 0) {
					int b = arrayWithSmoothing[i-1][j];
					int c = arrayWithSmoothing[i-1][j+1];
					int e = arrayWithSmoothing[i][j+1];
					int h = arrayWithSmoothing[i+1][j+1];
					int g = arrayWithSmoothing[i+1][j];
					int f = arrayWithSmoothing[i+1][j-1];
					int d = arrayWithSmoothing[i][j-1];
					int a = arrayWithSmoothing[i-1][j-1];
					
//					int x_one = arrayWithPadding[i][j] + (b*g) * (d+e) + (d*e) * (b+g);
					int x_one = arrayWithSmoothing[i][j] + a*h + c*f + b*g + d*e;
					
					if(x_one > 0)
						arrayWithSmoothing[i][j] = 1;
				}
			}
		}
	}

	private int[][] skeletonization(int[][] tempScaledArray, String step) {
		
		ArrayList<String> flaggedPixels = new ArrayList<String>();
		
		for(int i=1; i < tempScaledArray.length - 1; i++) {
			for(int j=1; j <tempScaledArray[0].length - 1; j++) {
				
				int A = 0;
				int B = 0;
				
				int p2 = tempScaledArray[i-1][j];
				int p3 = tempScaledArray[i-1][j+1];
				int p4 = tempScaledArray[i][j+1];
				int p5 = tempScaledArray[i+1][j+1];
				int p6 = tempScaledArray[i+1][j];
				int p7 = tempScaledArray[i+1][j-1];
				int p8 = tempScaledArray[i][j-1];
				int p9 = tempScaledArray[i-1][j-1];
				
				if(tempScaledArray[i][j] == 1) {
//										
					if(p2 == 1 && p9 == 0)
						A++;
					if(p3 == 1 && p2 == 0)
						A++;
					if(p4 == 1 && p3 == 0)
						A++;
					if(p5 == 1 && p4 == 0)
						A++;
					if(p6 == 1 && p5 == 0)
						A++;
					if(p7 == 1 && p6 == 0)
						A++;
					if(p8 == 1 && p7 == 0)
						A++;
					if(p9 == 1 && p8 == 0)
						A++;
					
//				}		
				
				B = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
				
				if(B >= 2 && B <= 6) {
					if(A == 1) {
						if(step.equalsIgnoreCase("Step1")) {
							if((p2 * p4 * p6) == 0){
								if((p4 * p6 * p8) == 0) {
									flaggedPixels.add(i +" "+ j);
								}
							}
						}
						else if(step.equalsIgnoreCase("Step2")) {
							if((p2 * p4 * p8) == 0){
								if((p2 * p6 * p8) == 0) {
									flaggedPixels.add(i +" "+ j);
								}
							}
						}
						
					}
				}
			}
			}			
		}		
		
		for(String pixel : flaggedPixels) {
			int x = Integer.parseInt(pixel.split(" ")[0]);
			int y = Integer.parseInt(pixel.split(" ")[1]);
			
			tempScaledArray[x][y] = 0;
		}
		
		return tempScaledArray;
	}

	private void findCenterOfGravity(int scaledImageArray[][]) {
		
		int m_zero_zero=0;
		int m_one_zero=0;
		int m_zero_one=0;
		int centroidX=0;
		int centroidY=0;
		
		for(int i=0; i < scaledImageArray.length; i++) {
			for(int j=0; j <scaledImageArray[0].length; j++) {
				m_zero_zero += scaledImageArray[i][j];
				m_one_zero += ((int) Math.pow(i, 1)) * ((int) Math.pow(j, 0)) * scaledImageArray[i][j];
				m_zero_one += ((int) Math.pow(i, 0)) * ((int) Math.pow(j, 1)) * scaledImageArray[i][j];
			}			
		}
				
		centroidX = m_one_zero / m_zero_zero;
		centroidY = m_zero_one / m_zero_zero;
		
		System.out.println("M00 " +m_zero_zero);
		System.out.println("M10 " +m_one_zero);
		System.out.println("M01 " +m_zero_one);
		System.out.println("CentroidX " +centroidX);
		System.out.println("CentroidY " +centroidY);
	}

	private int[][] normalization(int width, int height) {
		
		int NEW_WIDTH = (cols) * 2;
		int NEW_HEIGHT = (rows) * 2;
		
		int scaledArray[][] = new int[NEW_HEIGHT][NEW_WIDTH];
		
		double alpha = (double)NEW_WIDTH / width;
		double beta = (double)NEW_HEIGHT / height;
		
		System.out.println("Beta\t" + beta +"\nAlpha\t" + alpha);
		
		for(int i=0; i < NEW_HEIGHT; i++) {
			for(int j=0; j <NEW_WIDTH; j++) {				
				scaledArray[i][j] = arrayWithSmoothing[(int)(i /beta)][(int)(j /alpha)];
			}
		}
		
		return scaledArray;
	}

	public void getImageFromArray(int width, int height, int inputArray[][], String typeOfImage) throws IOException {
				
//		double inputarray[][] = new double[height][width];
//		
//		  for(int x = 0; x<height; x++){
//            for(int y = 0; y<width; y++){               
//            	inputarray[x][y] = inputArray[x][y];
//            }
//        }
//		
//		HeatChart map = new HeatChart(inputarray);
//		
//		map.saveToFile(new File("res/" + typeOfImage + ".jpg"));
		
		
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        WritableRaster raster = image.getRaster();
        
        for(int x = 0; x<width; x++){
            for(int y = 0; y<height; y++){               
            	raster.setSample(x,y,0,inputArray[y][x]);
            }
        }

        File outputfile = new File("res/" + typeOfImage + ".jpg");
        ImageIO.write(image, "jpg", outputfile);
    }
	
	public void smoothing() {
		
		ignoreLeftColumn();
		ignoreRightColumn();
		ignoreTopRow();		
		ignoreBottomRow();	
	
	}

	private void ignoreTopRow() {
		
		for(int i=1; i < arrayWithSmoothing.length - 1; i++) {
			for(int j=1; j <arrayWithSmoothing[0].length - 1; j++) {
				// 3 by 3 matrix - ignore top row, check if the surrounding pixels are all the same
				if((arrayWithSmoothing[i+1][j] == arrayWithSmoothing[i+1][j-1]) && 
						(arrayWithSmoothing[i+1][j] == arrayWithSmoothing[i+1][j+1]) && 
							(arrayWithSmoothing[i+1][j] == arrayWithSmoothing[i][j+1]) && 
								(arrayWithSmoothing[i+1][j] == arrayWithSmoothing[i][j-1])) {
					
						if(arrayWithSmoothing[i+1][j] == 1) {
							arrayWithSmoothing[i][j] = 1;
						}
						else if(arrayWithSmoothing[i+1][j] == 0) {
							arrayWithSmoothing[i][j] = 0;
						}
				}
			}
		}
		
	}

	private void ignoreBottomRow() {
		
		for(int i=1; i < arrayWithSmoothing.length - 1; i++) {
			for(int j=1; j <arrayWithSmoothing[0].length - 1; j++) {
				// 3 by 3 matrix - ignore bottom row, check if the surrounding pixels are all the same
				if((arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i-1][j-1]) && 
						(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i-1][j+1]) && 
							(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i][j+1]) && 
								(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i][j-1])) {
					
						if(arrayWithSmoothing[i-1][j] == 1) {
							arrayWithSmoothing[i][j] = 1;
						}
						else if(arrayWithSmoothing[i-1][j] == 0) {
							arrayWithSmoothing[i][j] = 0;
						}
				}
			}
		}
		
	}

	private void ignoreRightColumn() {
		
		for(int i=1; i < arrayWithSmoothing.length - 1; i++) {
			for(int j=1; j <arrayWithSmoothing[0].length - 1; j++) {
				// 3 by 3 matrix - ignore right column, check if the surrounding pixels are all the same
				if((arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i+1][j]) && 
						(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i-1][j-1]) && 
							(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i][j-1]) && 
								(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i+1][j-1])) {
					
						if(arrayWithSmoothing[i-1][j] == 1) {
							arrayWithSmoothing[i][j] = 1;
						}
						else if(arrayWithSmoothing[i-1][j] == 0) {
							arrayWithSmoothing[i][j] = 0;
						}
				}
			}
		}
		
	}

	private void ignoreLeftColumn() {	
	
		for(int i=1; i < arrayWithSmoothing.length - 1; i++) {
			for(int j=1; j <arrayWithSmoothing[0].length - 1; j++) {
				
				// 3 by 3 matrix - ignore left column, check if the surrounding pixels are all the same
				if((arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i+1][j]) && 
						(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i-1][j+1]) && 
							(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i][j+1]) && 
								(arrayWithSmoothing[i-1][j] == arrayWithSmoothing[i+1][j+1])) {
					
						if(arrayWithSmoothing[i-1][j] == 1) {
							arrayWithSmoothing[i][j] = 1;
						}
						else if(arrayWithSmoothing[i-1][j] == 0) {
							arrayWithSmoothing[i][j] = 0;
						}
				}
			}
		}
		
	}

	private void addPadding() {
				
		for(int i=0;i < array.length; i++) {
			for(int j=0; j < array[0].length; j++) {
				arrayWithSmoothing[i+1][j+1] = array[i][j];
			}
		}
	}

	public void readFile(Scanner scanner) {
		
		int x=0, y=0;
		
		while(scanner.hasNext()) {
			array[x][y] = scanner.nextInt();
			
			// keep on reading the same line until it reaches the last element
			if(y < array[0].length - 1) {
				y++;
			} 
			else {
				// come to next row
				x++;
				
				// and set the column number equal to 0
				y=0;
			}
		}
	}
}
