package math;

import java.util.Arrays;

public class BezierCurve implements Curve {

	private static final int[][] BINOMIAL = { { 1 }, { 1, 1 }, { 1, 2, 1 }, { 1, 3, 3, 1 }, { 1, 4, 6, 4, 1 },
			{ 1, 5, 10, 10, 5, 1 }, { 1, 6, 15, 20, 15, 6, 1 } };

	public final Vector[] controlPoints;
	private BezierCurve derivative;
	public final double[] controlPointsX;
	public final double[] controlPointsY;
	// private final int n;

	/**
	 * Makes a new Bezier Curve
	 * 
	 * @param controlPoints
	 *            the control points to use. must be at least 2, and no more than 7.
	 */
	public BezierCurve(Vector... controlPoints) {
		this(controlPoints, true);
	}

	private BezierCurve(Vector[] controlPoints, boolean isTop) {
		this.controlPoints = controlPoints;
		// this.n = controlPoints.length - 1;
		// Timer t = new Timer();
		// t.printElapsed("Time calculating derivatives: ");
		controlPointsX = new double[controlPoints.length];
		controlPointsY = new double[controlPoints.length];
		for (int i = 0; i < controlPoints.length; i++) {
			controlPointsX[i] = controlPoints[i].x;
			controlPointsY[i] = controlPoints[i].y;
		}
		if (isTop) {
			// t.reset();
			this.createTToArcLengthTable();
			// t.printElapsed("Time calculating LUT: ");
		}
	}

	private BezierCurve calculateDerivative() {
		if (controlPoints.length == 1)
			return null;
		Vector[] newPoints = new Vector[controlPoints.length - 1];
		for (int i = 0; i < newPoints.length; i++) {
			newPoints[i] = controlPoints[i + 1].subtract(controlPoints[i]).scale(newPoints.length);
		}
		return new BezierCurve(newPoints, false); // warning, recursive
	}

	public Vector deCasteljau(double t) {
		// return bezier(t);
		return deCasteljau(Arrays.copyOf(controlPointsX, controlPointsX.length),
				Arrays.copyOf(controlPointsY, controlPointsY.length), controlPoints.length, t);
	}

	private Vector deCasteljau(double[] x, double[] y, int length, double t) {
		for (int k = length; k > 1; k--) {
			for (int i = 0; i < k - 1; i++) {
				double mt = 1 - t;
				x[i] = x[i] * mt + x[i + 1] * t;
				y[i] = y[i] * mt + y[i + 1] * t;
			}
		}
		return new Vector(x[0], y[0]);
	}

	public BezierCurve getDerivative() {
		if (derivative == null) {
			derivative = calculateDerivative();
			System.out.println("calculated derivative");
		}
		return derivative;
	}

	public double arcLengthDerivative(double t) {
		return getDerivative().deCasteljau(t).magnitude;
	}

	public double arcLengthIntegral(double lower, double upper) {
		return Util.gaussQuadIntegrate(this::arcLengthDerivative, lower, upper);
	}

	private LookupTable tToArcLengthTable = null;

	private void createTToArcLengthTable() {
		tToArcLengthTable = new LookupTable(this::arcLengthIntegral, 0, 1);
	}

	public LookupTable tToArcLengthTable() {
		if (tToArcLengthTable == null)
			createTToArcLengthTable();
		return tToArcLengthTable;
	}

	public double getArcLength(double t) {
		return tToArcLengthTable().getOutput(t);
	}

	public double tFromArcLength(double arcLength) {
		return tToArcLengthTable().getInput(arcLength);
	}

	public double curvature(double t) {
		Vector d1 = getDerivative().deCasteljau(t);
		Vector d2 = getDerivative().getDerivative().deCasteljau(t);
		return Util.curvature2d(d1.x, d2.x, d1.y, d2.y);
	}

	@Override
	public double getTotalArcLength() {
		return arcLengthIntegral(0, 1);
	}

	@Override
	public double getCurvatureAtArcLength(double arcLength) {
		return curvature(tFromArcLength(arcLength));
	}

	@Override
	public Vector getPointAtArcLength(double arcLength) {
		return deCasteljau(tFromArcLength(arcLength));
	}

	@Override
	public String toString() {
		String result = "";
		for (int i = 0; i < controlPoints.length; i++) {
			result += controlPoints[i].toString() + " | ";
		}
		return result;
	}

}