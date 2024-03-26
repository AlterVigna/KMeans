package it.unipi.ca.KMeans;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sequential version of KMeans.
 * 
 */
public class SequentialKMeans {
	public static void main(String[] args) {

		// External dataset file info
		String csvFile;
		String csvSplitBy;

		// Algorithm parameters
		int K; // Number of clusters to discover
		int MAX_ITERATIONS; // Stopping condition

		Properties properties = new Properties();
		String fileProperties = "config.properties";

		FileInputStream inputConfig = null;
		try {

			// Load properties from config.properties file.
			inputConfig = new FileInputStream(fileProperties);
			properties.load(inputConfig);

			csvFile = properties.getProperty("datasetPath");
			csvSplitBy = properties.getProperty("csvCharSplit");
			K = Integer.parseInt(properties.getProperty("numberOfClusters_K"));
			MAX_ITERATIONS = Integer.parseInt(properties.getProperty("maxIterations"));

		} catch (Exception e) {

			e.printStackTrace();
			System.err.println("Some of the properties are not configured correctly. The program will be quit..");
			return;
		}

		int DIM = getNumberOfDimensions(csvFile, csvSplitBy); // Dimension of the points in the dataset
		@SuppressWarnings("unused")
		int DATASET_SIZE = Integer.parseInt(Pattern.compile("[^0-9]").matcher(csvFile).replaceAll("").toString());

		// Dataset points a list of points in n-dimensions.
		List<List<Float>> points = new ArrayList<List<Float>>();

		// Result to be achieved: about Cluster informations and composition

		// Centroid points
		List<List<Float>> centroids = new ArrayList<List<Float>>();
		// Membership of each point in the cluster
		List<Integer> membership = new ArrayList<Integer>();

		
		
		
		
//----- START MEASUREMENT

		long startMain = System.currentTimeMillis();

		System.out.println("Loading the dataset");

		long startLoadDataset = System.currentTimeMillis();

		loadData(csvFile, csvSplitBy, DIM, points, membership);

		long endLoadDataset = System.currentTimeMillis();
		System.out.println("Dataset loaded");

		long startVariableInit = System.currentTimeMillis();
		initializeCentroids(K, points, centroids);

		System.out.println("\nStarting centroids: ");
		printCentroids(centroids);

		// Temp. variables
		List<List<Float>> sums = new ArrayList<List<Float>>();
		List<Integer> counts = new ArrayList<Integer>();

		// Initialization temp. variables.
		for (int j = 0; j < K; j++) {
			List<Float> partialSum = new ArrayList<Float>();
			for (int k = 0; k < DIM; k++) {
				partialSum.add(0.0f);
			}
			sums.add(partialSum);
			counts.add(0);
		}

		long endVariableInit = System.currentTimeMillis();
		
		System.out.println("\nStart of the algorithm");
		long startAlgorithmExecution = System.currentTimeMillis();
		
		for (int nrIteration = 0; nrIteration < MAX_ITERATIONS; nrIteration++) {

			// For each point in the cluster, assign its nearest centroid and compute (sums and count) information of each cluster
			assignPointsToCluster(K, points, centroids, sums, counts, membership);

			// Update new Centroids
			updateCentroids(K, DIM, centroids, sums, counts);
		}
		
		System.out.println("\nEnd of the algorithm");
		long endAlgorithmExecution = System.currentTimeMillis();

		System.out.println("\nLast centroids: ");
		printCentroids(centroids);

		long endMain = System.currentTimeMillis();

//----- END MEASUREMENT

		printTimeElapsed(endMain - startMain, endLoadDataset - startLoadDataset, endVariableInit - startVariableInit,
				endAlgorithmExecution - startAlgorithmExecution);
	}

	/**
	 * Method for loading the dataset into a proper data structure and
	 * initialization of default membership.
	 */
	public static void loadData(String csvFile, String csvSplitBy, int N_DIM, List<List<Float>> points,
			List<Integer> membership) {
		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			String line = br.readLine(); // Skip the first header line
			while ((line = br.readLine()) != null) {
				String[] data = line.split(csvSplitBy);
				List<Float> point = new ArrayList<Float>();
				for (int dim = 0; dim < N_DIM; dim++) {
					point.add(Float.parseFloat(data[dim]));
				}
				points.add(point);
				membership.add(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function to use to assign to each point in the dataset a cluster membership
	 * and compute the sums and the counts of the new obtained cluster.
	 * 
	 * @param K          - the number of clusters
	 * @param points     - the dataset
	 * @param centroids  - the list of central points representing each cluster
	 * @param sums       - the list of all the sums for each cluster in all dimensions
	 * @param counts     - the list of all the counts for each cluster
	 * @param membership - the list of all membership relation between each point and the nearest cluster
	 */
	public static void assignPointsToCluster(int K, List<List<Float>> points, List<List<Float>> centroids,
			List<List<Float>> sums, List<Integer> counts, List<Integer> membership) {

		List<Float> dists = new ArrayList<Float>();
		for (int j = 0; j < K; j++) {
			dists.add(0.0f);
		}

		for (int indexOfPoint = 0; indexOfPoint < points.size(); indexOfPoint++) {

			for (int clusterIndex = 0; clusterIndex < K; clusterIndex++) {

				float sumPartial = 0.0f;
				for (int dim = 0; dim < centroids.get(clusterIndex).size(); dim++) {
					sumPartial += Math.pow(centroids.get(clusterIndex).get(dim) - points.get(indexOfPoint).get(dim), 2);
				}

				// Compute distance
				dists.set(clusterIndex, (float) Math.sqrt(sumPartial));
			}

			float min = dists.get(0);
			int minIndex = 0;

			for (int z = 1; z < dists.size(); z++) {

				float currentValue = dists.get(z);
				if (currentValue < min) {
					min = currentValue;
					minIndex = z;
				}
			}
			// Assign to the point, the nearest cluster.
			membership.set(indexOfPoint, minIndex);

			// Save information of the points that belongs to the new cluster in order to
			// update it leater.
			for (int dim = 0; dim < centroids.get(minIndex).size(); dim++) {
				sums.get(minIndex).set(dim, sums.get(minIndex).get(dim) + points.get(indexOfPoint).get(dim));
			}
			counts.set(minIndex, counts.get(minIndex) + 1);
		}

	}

	/**
	 * Function to choose and initialize random centroid from a dataset of points.
	 * 
	 * @param K         - the number of clusters
	 * @param points    - the dataset
	 * @param centroids - the list of central points representing each cluster
	 */
	public static void initializeCentroids(int K, List<List<Float>> points, List<List<Float>> centroids) {
		Random random = new Random(0);
		Set<Integer> randomChosen = new HashSet<Integer>();
		for (int i = 0; i < K; i++) {
			int randomNumber = -1;
			do {
				randomNumber = random.nextInt(points.size());
			} while (randomChosen.contains(randomNumber));
			randomChosen.add(randomNumber);
			centroids.add(new ArrayList<Float>(points.get(randomNumber)));
		}
	}

	/**
	 * Function to choose new representative centroids for new clusters.
	 * 
	 * @param K         - the number of clusters
	 * @param DIM       - the number of dimension of each point
	 * @param centroids - the list of central points representing each cluster
	 * @param sums      - the list of all the sums for each cluster in all
	 *                  dimensions
	 * @param counts    - the list of all the counts for each cluster
	 */
	public static void updateCentroids(int K, int DIM, List<List<Float>> centroids, List<List<Float>> sums,
			List<Integer> counts) {

		// Compute the mean for each cluster to discover new centroid point.
		for (int j = 0; j < K; j++) {
			for (int dim = 0; dim < DIM; dim++) {
				centroids.get(j).set(dim, sums.get(j).get(dim) / counts.get(j));
			}
		}

		// reset distance and sum
		for (int j = 0; j < K; j++) {
			for (int k = 0; k < DIM; k++) {
				sums.get(j).set(k, 0.0f);
			}
			counts.set(j, 0);
		}
	}

	/**
	 * Utility method to print the centroids coordinates.
	 * 
	 * @param centroids - the list of central points representing each cluster
	 */
	public static void printCentroids(List<List<Float>> centroids) {
		for (int i = 0; i < centroids.size(); i++) {
			System.out.print("Centroid " + i + ": ");
			for (Float val : centroids.get(i)) {
				System.out.print(val + " ");
			}
			System.out.println();
		}
	}

	/**
	 * Utility method to discover the number of space dimension of the cluster
	 * coordinates points.
	 * 
	 * @param csvFile    - the file location of the dataset
	 * @param csvSplitBy - the splitting character to analize a row in the csv file
	 * @return - the number of dimensions of each point
	 */
	public static int getNumberOfDimensions(String csvFile, String csvSplitBy) {

		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			String line;
			line = br.readLine();
			String[] ris = line.split(csvSplitBy);
			return ris.length;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Utility method to track execution time among various part of the program.
	 * 
	 * @param totalElapsedTime            - total time between start and end of the
	 *                                    program
	 * @param totalLoadingDatasetTime     - total time needed to load the dataset in
	 *                                    memory
	 * @param totalInitCentroidTime       - total time needed to initialize
	 *                                    centroids
	 * @param totalAlgorithmExecutionTime - total time needed execute the main
	 *                                    algorithm
	 */
	public static void printTimeElapsed(long totalElapsedTime, long totalLoadingDatasetTime, long totalInitCentroidTime,
			long totalAlgorithmExecutionTime) {

		System.out.println("\nTotal execution time (ms): " + totalElapsedTime);

		System.out.println("DETAILS:");
		System.out.println("Loading dataset (ms): " + totalLoadingDatasetTime);
		System.out.println("Init. Variables time (ms): " + totalInitCentroidTime);
		System.out.println("Alg. execution time (ms): " + totalAlgorithmExecutionTime);

	}

}