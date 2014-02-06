package remote;

import java.util.Random;

public class Perf2 {

	int repeats = 100;

	public static void main(String[] args) {
		time();
	}
	
	public static void time() {
		long stime = System.currentTimeMillis();
		System.out.println(computepi(1000000));
		long etime = System.currentTimeMillis();
		System.out.println("took "+(etime-stime)+"ms");
	}

	public static double computepi(int iterations) {
		Random randomGen = new Random(System.currentTimeMillis());
		int insideCount = 0;
		for (int i = 1; i <= iterations; i++) {
			insideCount += doOneIteration(randomGen);
		}
		return calc(iterations, insideCount);
	}

	private static int doOneIteration(Random randomGen) {
		double xPos = getRandom(randomGen);
		double yPos = getRandom(randomGen);
		return isInside( xPos, yPos);
	}

	private static int isInside( double xPos, double yPos) {
		double distance = Math.sqrt((xPos * xPos) + (yPos * yPos));
		if (distance<1.0) return 1;
		else return 0;
	}

	private static double getRandom(Random randomGen) {
		return (randomGen.nextDouble()) * 2 - 1.0;
	}

	private static double calc(int iterations, int insideCount) {
		return 4.0 * (insideCount / (double)iterations);
	}

}
