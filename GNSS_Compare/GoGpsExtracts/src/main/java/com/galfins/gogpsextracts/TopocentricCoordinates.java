/*
 * Copyright (c) 2010, Eugenio Realini, Mirko Reguzzoni, Cryms sagl - Switzerland. All Rights Reserved.
 *
 * This file is part of goGPS Project (goGPS).
 *
 * goGPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * goGPS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with goGPS.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.galfins.gogpsextracts;

import org.ejml.simple.SimpleMatrix;
/**
 * <p>
 * Class for
 * </p>
 *
 * @author Eugenio Realini, Cryms.com
 */
public class TopocentricCoordinates {

	private SimpleMatrix topocentric = new SimpleMatrix(3, 1); /* Azimuth (az), elevation (el), distance (d) */

    public TopocentricCoordinates(){

    }

	public TopocentricCoordinates(Coordinates origin, Coordinates target){
	    computeTopocentric(origin, target);
    }

	/**
	 * @param origin
	 */
	public TopocentricCoordinates computeTopocentric(Coordinates origin, Coordinates target) {

//		// Build rotation matrix from global to local reference systems
//		SimpleMatrix R = globalToLocalMatrix(origin);
//
//		// Compute local vector from origin to this object coordinates
//		//SimpleMatrix enu = R.mult(target.ecef.minus(origin.ecef));
//		SimpleMatrix enu = R.mult(target.minusXYZ(origin));

		origin.computeLocalV2(target);

		double E = origin.getE();//enu.get(0);
		double N = origin.getN();//enu.get(1);
		double U = origin.getU();//enu.get(2);

		// Compute horizontal distance from origin to this object
		double hDist = Math.sqrt(Math.pow(E, 2) + Math.pow(N, 2));

		// If this object is at zenith ...
		if (hDist < 1e-20) {
			// ... set azimuth = 0 and elevation = 90, ...
			topocentric.set(0, 0, 0);
			topocentric.set(1, 0, 90);

		} else {

			// ... otherwise compute azimuth ...
			topocentric.set(0, 0, Math.toDegrees(Math.atan2(E, N)));

			// ... and elevation
			topocentric.set(1, 0, Math.toDegrees(Math.atan2(U, hDist)));

			if (topocentric.get(0) < 0)
				topocentric.set(0, 0, topocentric.get(0) + 360);
		}

		// Compute distance
		topocentric.set( 2, 0, Math.sqrt(Math.pow(E, 2) + Math.pow(N, 2) + Math.pow(U, 2)));
		
		return this;
	}

	public double getAzimuth(){
		return topocentric.get(0);
	}
	
	public double getElevation(){
		return topocentric.get(1);
	}
	
	public double getDistance(){
		return topocentric.get(2);
	}

//	/**
//	 * @param origin
//	 * @return Rotation matrix from global to local reference systems
//	 */
//	private SimpleMatrix globalToLocalMatrix(Coordinates origin) {
//
//		double lam = Math.toRadians(origin.getGeodeticLongitude());
//		double phi = Math.toRadians(origin.getGeodeticLatitude());
//
//		double cosLam = Math.cos(lam);
//		double cosPhi = Math.cos(phi);
//		double sinLam = Math.sin(lam);
//		double sinPhi = Math.sin(phi);
//
//		double[][] data = new double[3][3];
//		data[0][0] = -sinLam;
//		data[0][1] = cosLam;
//		data[0][2] = 0;
//		data[1][0] = -sinPhi * cosLam;
//		data[1][1] = -sinPhi * sinLam;
//		data[1][2] = cosPhi;
//		data[2][0] = cosPhi * cosLam;
//		data[2][1] = cosPhi * sinLam;
//		data[2][2] = sinPhi;
//
//		SimpleMatrix R = new SimpleMatrix(data);
//
//		return R;
//	}

}
