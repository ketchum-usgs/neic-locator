package gov.usgs.locator;

import java.util.Arrays;

import gov.usgs.traveltime.TauUtil;

/**
 * This weighted residuals storage combines the residuals and weights 
 * for picks and the Bayesian depth, while providing storage for the 
 * dispersion calculation and the spatial derivatives.  Note that the 
 * residuals and weights are kept separate as some calculations depend 
 * only on the residuals.
 * 
 * @author Ray Buland
 *
 */
public class Wresidual implements Comparable<Wresidual>{
	boolean isDepth;		// True if this is the Bayesian depth residual
	double residual;		// Residual in seconds for picks or kilometers for depth
	double estResidual;	// Linearly estimated residual
	double weight;			// Weight
	double[] deriv;			// Spatial derivatives in kilometers
	double[] deDeriv;		// Demedianed spatial derivatives in kilometers
	double dNorm;				// 2-norm of the horizontal derivatives in kilometers
	double sortValue;		// The value to sort on
	Pick pick;					// Pointer to the pick the residuals were derived from
	
	/**
	 * Create the object with no initial information.
	 */
	public Wresidual() {
		estResidual = 0d;
		dNorm = Double.NaN;
		sortValue = Double.NaN;
	}
	
	/**
	 * Initialize the weighted residual.
	 * 
	 * @param pick The pick associated with this data, if any
	 * @param residual Residual in seconds for picks and in kilometers 
	 * for depth
	 * @param weight Weight
	 * @param isDepth True if this is the Bayesian depth residual
	 * @param dTdLat Derivative of travel time with respect to latitude 
	 * in seconds/kilometers
	 * @param dTdLon Derivative of travel time with respect to longitude 
	 * in seconds/kilometers
	 * @param dTdDepth Derivative of travel time with respect to depth 
	 * in seconds/kilometers
	 */
	public Wresidual(Pick pick, double residual, double weight, boolean isDepth, 
			double dTdLat, double dTdLon, double dTdDepth) {
		this.pick = pick;
		this.residual = residual;
		this.weight = weight;
		this.isDepth = isDepth;
		deriv = new double[3];
		deriv[0] = dTdLat;
		deriv[1] = dTdLon;
		deriv[2] = dTdDepth;
		// Initialize the demedianed derivatives.
		deDeriv = Arrays.copyOf(deriv, deriv.length);
		estResidual = 0d;
		dNorm = Double.NaN;
		sortValue = Double.NaN;
	}
	
	/**
	 * Re-initialize the weighted residual.
	 * 
	 * @param pick The pick associated with this data, if any
	 * @param residual Residual in seconds for picks and in kilometers 
	 * for depth
	 * @param weight Weight
	 * @param isDepth True if this is the Bayesian depth residual
	 * @param dTdLat Derivative of travel time with respect to latitude 
	 * in seconds/kilometers
	 * @param dTdLon Derivative of travel time with respect to longitude 
	 * in seconds/kilometers
	 * @param dTdDepth Derivative of travel time with respect to depth 
	 * in seconds/kilometers
	 */
	public void reInit(Pick pick, double residual, double weight, boolean isDepth, 
			double dTdLat, double dTdLon, double dTdDepth) {
		this.pick = pick;
		this.residual = residual;
		this.weight = weight;
		this.isDepth = isDepth;
		deriv = new double[3];
		deriv[0] = dTdLat;
		deriv[1] = dTdLon;
		deriv[2] = dTdDepth;
		// Initialize the demedianed derivatives.
		deDeriv = Arrays.copyOf(deriv, deriv.length);
		estResidual = 0d;
		dNorm = Double.NaN;
		sortValue = Double.NaN;
	}
	
	/**
	 * Remove the median from the travel-time residuals.
	 * 
	 * @param median Median travel-time residual in seconds
	 */
	public void deMedianRes(double median) {
		if(!isDepth) residual -= median;
	}
	
	/**
	 * Remove the median from the derivatives.  Note that this isn't the 
	 * median of the derivatives, but the derivative corresponding to the 
	 * median of the residuals.
	 * 
	 * @param medians Array of derivative medians
	 */
	public void deMedianDeriv(double[] medians) {
		if(!isDepth) {
			for(int j=0; j<medians.length; j++) {
				deDeriv[j] = deriv[j]-medians[j];
			}
		}
	}
	
	/**
	 * Update the estimated residual given a trial step vector.
	 * 
	 * @param trialVector Distance and direction from the current 
	 * hypocenter in kilometers
	 */
	public void updateEst(double[] trialVector) {
		estResidual = residual;
		for(int j=0; j<trialVector.length; j++) {
			estResidual -= trialVector[j]*deriv[j];
		}
	}
	
	/**
	 * Remove the median from the estimated travel-time residuals.
	 * 
	 * @param median Median estimated travel-time residuals in seconds
	 */
	public void deMedianEst(double median) {
		if(!isDepth) estResidual -= median;
	}
	
	/**
	 * Set the sort value to sort by travel-time residual.  Note that 
	 * the depth residual will be sorted to the end to keep it out of 
	 * the way.
	 */
	public void sortRes() {
		if(isDepth) sortValue = TauUtil.DMAX;
		else sortValue = residual;
	}
	
	/**
	 * Set the sort value to sort by absolute demedianed travel-time 
	 * residuals in order to compute the spread, a 1-norm measure of 
	 * scatter.  Note that the depth residual will be sorted to the end 
	 * to keep it out of the way.
	 * 
	 * @param median Median travel-time residual in seconds
	 */
	public void sortSpread(double median) {
		if(isDepth) sortValue = TauUtil.DMAX;
		else sortValue = Math.abs(residual-median);
	}
	
	/**
	 * Set the sort value to sort by the demedianed, weighted residuals 
	 * in order to compute the R-estimator dispersion or penalty function.
	 * 
	 * @param median Median travel-time residual in seconds
	 */
	public void sortDisp(double median) {
		if(isDepth) sortValue = residual*weight;
		else sortValue = (residual-median)*weight;
	}
	
	/**
	 * Set the sort value to sort by the estimated travel-time residual.  
	 * Note that the depth residual will be sorted to the end to keep it 
	 * out of the way.
	 */
	public void sortEst() {
		if(isDepth) sortValue = TauUtil.DMAX;
		else sortValue = estResidual;
	}
	
	/**
	 * Set the sort value to sort by the demedianed, weighted, estimated 
	 * residuals in order to compute the R-estimator dispersion or penalty 
	 * function.
	 * 
	 * @param median Median estimated travel-time residual in seconds
	 */
	public void sortEstDisp(double median) {
		if(isDepth) sortValue = estResidual*weight;
		else sortValue = (estResidual-median)*weight;
	}
	
	/**
	 * Contribute to projecting the original weighted residuals.
	 * 
	 * @param wRes Projected weighted residual
	 * @param v Eigenvector element
	 */
	public void proj(Wresidual wRes, double v) {
		residual += v*wRes.residual;
		for(int j=0; j<deriv.length; j++) {
			deriv[j] += v*wRes.deriv[j];
		}
	}
	
	/**
	 * Contribute to projecting the estimated weighted residuals.
	 * 
	 * @param wRes Projected weighted residual
	 * @param v Eigenvector element
	 */
	public void estProj(Wresidual wRes, double v) {
		estResidual += v*wRes.estResidual;
	}
	
	/**
	 * If the eigenvector is backwards change the sign of the 
	 * residual and it's derivatives.
	 */
	public void changeSign() {
		residual = -residual;
		for(int j=0; j<deriv.length; j++) {
			deriv[j] = -deriv[j];
		}
	}
	
	/**
	 * Get the 2-norm of the horizontal derivatives.
	 * 
	 * @return The 2-norm of the horizontal derivatives in kilometers
	 */
	public double derivNorm() {
		if(Double.isNaN(dNorm)) dNorm = Math.sqrt(Math.pow(deriv[0],2d)+
				Math.pow(deriv[1],2d));
		return dNorm;
	}
	
	/**
	 * Get the correlation between these horizontal derivatives and 
	 * another set of horizontal derivatives.
	 * 
	 * @param wRes Weighted residual information to correlate against
	 * @return Correlation between the horizontal derivatives of two picks
	 */
	public double derivCorr(Wresidual wRes) {
		return (deriv[0]*wRes.deriv[0]+deriv[1]*wRes.deriv[1])/
				(derivNorm()*wRes.derivNorm());
	}
	
	/**
	 * Get the weighted derivatives for computing the "normal" matrix.
	 * 
	 * @param n Number of degrees of freedom
	 * @return Weighted derivative vector
	 */
	public double[] getWderiv(int n) {
		double c[] = new double[n];
		for(int j=0; j<n; j++) {
			c[j] = weight*deriv[j];
		}
		return c;
	}
	
	/**
	 * Get the weighted, demedianed derivatives for computing the 
	 * projected "normal" matrix.
	 * 
	 * @param n Number of degrees of freedom
	 * @return Weighted derivative vector
	 */
	public double[] getWdeDeriv(int n) {
		double c[] = new double[n];
		for(int j=0; j<n; j++) {
			c[j] = weight*deDeriv[j];
		}
		return c;
	}
	
	/**
	 * Update the pick data importance.
	 * 
	 * @param importance Pick data importance
	 */
	public void updateImport(double importance) {
		if(pick != null) pick.importance = importance;
	}
	
	/**
	 * Print the contents of the weighted residuals.
	 * 
	 * @param full If true, print the derivatives as well
	 */
	public void printWres(boolean full) {
		if(!full || deriv == null) {
			System.out.format("res: %7.2f %7.2f wt: %7.4f %b\n", residual, 
					estResidual, weight, isDepth);
		} else {
			System.out.format("res: %7.2f %7.2f wt: %7.4f deriv: %10.3e "+
					"%10.3e %10.3e %b\n", residual, estResidual, weight, deriv[0], 
					deriv[1], deriv[2], isDepth);
		}
	}

	/**
	 * Sort so that the current sort values are in ascending order.
	 */
	@Override
	public int compareTo(Wresidual wRes) {
		// Sort into value order.
		if(this.sortValue < wRes.sortValue) return -1;
		else if(this.sortValue > wRes.sortValue) return +1;
		else return 0;
	}
}
