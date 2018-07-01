/*
 * Copyright (c) 2011 Eugenio Realini, Mirko Reguzzoni, Cryms sagl - Switzerland. All Rights Reserved.
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
 */
package com.galfins.gogpsextracts;

import org.ejml.simple.SimpleMatrix;

/**
 * <p>
 *
 * </p>
 *
 * @author Eugenio Realini, Lorenzo Patocchi (code architecture)
 *
 * adapted for Galileo constellation by: Sebastian Ciuban
 */

public abstract class EphemerisSystemGalileo {

	/**
     * Input parameters:
     *
	 * @param unixTime
	 *
	 * @param obsPseudorange
	 * @param satID
	 * @param satType
     * @param eph
     * @param receiverClockError
     *
	 */

	// double[] pos ;
	public SatellitePosition computePositionGalileo(long unixTime, double obsPseudorange, int satID, char satType,
			EphGalileo eph, double receiverClockError) {

		// long unixTime = obs.getRefTime().getMsec();
		// double obsPseudorange = obs.getSatByIDType(satID,
		// satType).getPseudorange(0);

		// char satType2 = eph.getSatType() ;


			// System.out.println("### other than GLONASS data");

			// Compute satellite clock error
			double satelliteClockError = computeSatelliteClockError(unixTime, eph, obsPseudorange);

			// Compute clock corrected transmission time
			double tGPS = computeClockCorrectedTransmissionTime(unixTime, satelliteClockError, obsPseudorange);

			// Compute eccentric anomaly
			double Ek = computeEccentricAnomaly(tGPS, eph);

			// Semi-major axis
			double A = eph.getRootA() * eph.getRootA();

			// Time from the ephemerides reference epoch
			double tk = checkGpsTime(tGPS - eph.getToe());

			// Position computation
			double fk = Math.atan2(Math.sqrt(1 - Math.pow(eph.getE(), 2)) * Math.sin(Ek), Math.cos(Ek) - eph.getE());
			double phi = fk + eph.getOmega();
			phi = Math.IEEEremainder(phi, 2 * Math.PI);
			double u = phi + eph.getCuc() * Math.cos(2 * phi) + eph.getCus() * Math.sin(2 * phi);
			double r = A * (1 - eph.getE() * Math.cos(Ek)) + eph.getCrc() * Math.cos(2 * phi)
					+ eph.getCrs() * Math.sin(2 * phi);
			double ik = eph.getI0() + eph.getiDot() * tk + eph.getCic() * Math.cos(2 * phi)
					+ eph.getCis() * Math.sin(2 * phi);
			double Omega = eph.getOmega0() + (eph.getOmegaDot() - Constants.EARTH_ANGULAR_VELOCITY) * tk
					- Constants.EARTH_ANGULAR_VELOCITY * eph.getToe();
			Omega = Math.IEEEremainder(Omega + 2 * Math.PI, 2 * Math.PI);
			double x1 = Math.cos(u) * r;
			double y1 = Math.sin(u) * r;

			// Coordinates
			// double[][] data = new double[3][1];
			// data[0][0] = x1 * Math.cos(Omega) - y1 * Math.cos(ik) *
			// Math.sin(Omega);
			// data[1][0] = x1 * Math.sin(Omega) + y1 * Math.cos(ik) *
			// Math.cos(Omega);
			// data[2][0] = y1 * Math.sin(ik);

			// Fill in the satellite position matrix
			// this.coord.ecef = new SimpleMatrix(data);
			// this.coord = Coordinates.globalXYZInstance(new
			// SimpleMatrix(data));
			SatellitePosition sp = new SatellitePosition(unixTime, satID, satType,
					x1 * Math.cos(Omega) - y1 * Math.cos(ik) * Math.sin(Omega),
					x1 * Math.sin(Omega) + y1 * Math.cos(ik) * Math.cos(Omega), y1 * Math.sin(ik));
			sp.setSatelliteClockError(satelliteClockError);

			// Apply the correction due to the Earth rotation during signal
			// travel time
			SimpleMatrix R = computeEarthRotationCorrection(unixTime, receiverClockError, tGPS);
			sp.setSMMultXYZ(R);

			///////////////////////////
			// compute satellite speeds
			// The technical paper which describes the bc_velo.c program is
			/////////////////////////// published in
			// GPS Solutions, Volume 8, Number 2, 2004 (in press). "Computing
			/////////////////////////// Satellite Velocity using the Broadcast
			/////////////////////////// Ephemeris", by Benjamin W. Remondi
			double cus = eph.getCus();
			double cuc = eph.getCuc();
			double cis = eph.getCis();
			double crs = eph.getCrs();
			double crc = eph.getCrc();
			double cic = eph.getCic();
			double idot = eph.getiDot(); // 0.342514267094e-09;
			double e = eph.getE();

			double ek = Ek;
			double tak = fk;

			// Computed mean motion [rad/sec]
			double n0 = Math.sqrt(Constants.EARTH_GRAVITATIONAL_CONSTANT / Math.pow(A, 3));

			// Corrected mean motion [rad/sec]
			double n = n0 + eph.getDeltaN();

			// Mean anomaly
			double Mk = eph.getM0() + n * tk;

			double mkdot = n;
			double ekdot = mkdot / (1.0 - e * Math.cos(ek));
			double takdot = Math.sin(ek) * ekdot * (1.0 + e * Math.cos(tak))
					/ (Math.sin(tak) * (1.0 - e * Math.cos(ek)));
			double omegakdot = (eph.getOmegaDot() - Constants.EARTH_ANGULAR_VELOCITY);

			double phik = phi;
			double corr_u = cus * Math.sin(2.0 * phik) + cuc * Math.cos(2.0 * phik);
			double corr_r = crs * Math.sin(2.0 * phik) + crc * Math.cos(2.0 * phik);

			double uk = phik + corr_u;
			double rk = A * (1.0 - e * Math.cos(ek)) + corr_r;

			double ukdot = takdot + 2.0 * (cus * Math.cos(2.0 * uk) - cuc * Math.sin(2.0 * uk)) * takdot;
			double rkdot = A * e * Math.sin(ek) * n / (1.0 - e * Math.cos(ek))
					+ 2.0 * (crs * Math.cos(2.0 * uk) - crc * Math.sin(2.0 * uk)) * takdot;
			double ikdot = idot + (cis * Math.cos(2.0 * uk) - cic * Math.sin(2.0 * uk)) * 2.0 * takdot;

			double xpk = rk * Math.cos(uk);
			double ypk = rk * Math.sin(uk);

			double xpkdot = rkdot * Math.cos(uk) - ypk * ukdot;
			double ypkdot = rkdot * Math.sin(uk) + xpk * ukdot;

			double xkdot = (xpkdot - ypk * Math.cos(ik) * omegakdot) * Math.cos(Omega)
					- (xpk * omegakdot + ypkdot * Math.cos(ik) - ypk * Math.sin(ik) * ikdot) * Math.sin(Omega);
			double ykdot = (xpkdot - ypk * Math.cos(ik) * omegakdot) * Math.sin(Omega)
					+ (xpk * omegakdot + ypkdot * Math.cos(ik) - ypk * Math.sin(ik) * ikdot) * Math.cos(Omega);
			double zkdot = ypkdot * Math.sin(ik) + ypk * Math.cos(ik) * ikdot;

			sp.setSpeed(xkdot, ykdot, zkdot);

			return sp;
	}


    /**
     * Input parameters:
     *
     * @param unixTime
     * @param obsPseudorange
     * @param satID
     * @param satType
     * @param eph
     * @param receiverClockError
     *
     **/
	public SatellitePosition computePositionSpeedGalileo(long unixTime, double obsPseudorange, int satID, char satType,
			EphGalileo eph, double receiverClockError) {

		// long unixTime = obs.getRefTime().getMsec();
		// double obsPseudorange = obs.getSatByIDType(satID,
		// satType).getPseudorange(0);

		// Compute satellite clock error
		double satelliteClockError = computeSatelliteClockError(unixTime, eph, obsPseudorange);

		// Compute clock corrected transmission time
		double tGPS = computeClockCorrectedTransmissionTime(unixTime, satelliteClockError, obsPseudorange);

		// Compute eccentric anomaly
		double Ek = computeEccentricAnomaly(tGPS, eph);

		// Semi-major axis
		double A = eph.getRootA() * eph.getRootA();

		// Time from the ephemerides reference epoch
		double tk = checkGpsTime(tGPS - eph.getToe());

		// Position computation
		double fk = Math.atan2(Math.sqrt(1 - Math.pow(eph.getE(), 2)) * Math.sin(Ek), Math.cos(Ek) - eph.getE());
		double phi = fk + eph.getOmega();
		phi = Math.IEEEremainder(phi, 2 * Math.PI);
		double u = phi + eph.getCuc() * Math.cos(2 * phi) + eph.getCus() * Math.sin(2 * phi);
		double r = A * (1 - eph.getE() * Math.cos(Ek)) + eph.getCrc() * Math.cos(2 * phi)
				+ eph.getCrs() * Math.sin(2 * phi);
		double ik = eph.getI0() + eph.getiDot() * tk + eph.getCic() * Math.cos(2 * phi)
				+ eph.getCis() * Math.sin(2 * phi);
		double Omega = eph.getOmega0() + (eph.getOmegaDot() - Constants.EARTH_ANGULAR_VELOCITY) * tk
				- Constants.EARTH_ANGULAR_VELOCITY * eph.getToe();
		Omega = Math.IEEEremainder(Omega + 2 * Math.PI, 2 * Math.PI);
		double x1 = Math.cos(u) * r;
		double y1 = Math.sin(u) * r;

		// Coordinates
		// double[][] data = new double[3][1];
		// data[0][0] = x1 * Math.cos(Omega) - y1 * Math.cos(ik) *
		// Math.sin(Omega);
		// data[1][0] = x1 * Math.sin(Omega) + y1 * Math.cos(ik) *
		// Math.cos(Omega);
		// data[2][0] = y1 * Math.sin(ik);

		// Fill in the satellite position matrix
		// this.coord.ecef = new SimpleMatrix(data);
		// this.coord = Coordinates.globalXYZInstance(new SimpleMatrix(data));
		SatellitePosition sp = new SatellitePosition(unixTime, satID, satType,
				x1 * Math.cos(Omega) - y1 * Math.cos(ik) * Math.sin(Omega),
				x1 * Math.sin(Omega) + y1 * Math.cos(ik) * Math.cos(Omega), y1 * Math.sin(ik));
		sp.setSatelliteClockError(satelliteClockError);

		// Apply the correction due to the Earth rotation during signal travel
		// time
		SimpleMatrix R = computeEarthRotationCorrection(unixTime, receiverClockError, tGPS);
		sp.setSMMultXYZ(R);

		///////////////////////////
		// compute satellite speeds
		// The technical paper which describes the bc_velo.c program is
		/////////////////////////// published in
		// GPS Solutions, Volume 8, Number 2, 2004 (in press). "Computing
		/////////////////////////// Satellite Velocity using the Broadcast
		/////////////////////////// Ephemeris", by Benjamin W. Remondi
		double cus = eph.getCus();
		double cuc = eph.getCuc();
		double cis = eph.getCis();
		double crs = eph.getCrs();
		double crc = eph.getCrc();
		double cic = eph.getCic();
		double idot = eph.getiDot(); // 0.342514267094e-09;
		double e = eph.getE();

		double ek = Ek;
		double tak = fk;

		// Computed mean motion [rad/sec]
		double n0 = Math.sqrt(Constants.EARTH_GRAVITATIONAL_CONSTANT / Math.pow(A, 3));

		// Corrected mean motion [rad/sec]
		double n = n0 + eph.getDeltaN();

		// Mean anomaly
		double Mk = eph.getM0() + n * tk;

		double mkdot = n;
		double ekdot = mkdot / (1.0 - e * Math.cos(ek));
		double takdot = Math.sin(ek) * ekdot * (1.0 + e * Math.cos(tak)) / (Math.sin(tak) * (1.0 - e * Math.cos(ek)));
		double omegakdot = (eph.getOmegaDot() - Constants.EARTH_ANGULAR_VELOCITY);

		double phik = phi;
		double corr_u = cus * Math.sin(2.0 * phik) + cuc * Math.cos(2.0 * phik);
		double corr_r = crs * Math.sin(2.0 * phik) + crc * Math.cos(2.0 * phik);

		double uk = phik + corr_u;
		double rk = A * (1.0 - e * Math.cos(ek)) + corr_r;

		double ukdot = takdot + 2.0 * (cus * Math.cos(2.0 * uk) - cuc * Math.sin(2.0 * uk)) * takdot;
		double rkdot = A * e * Math.sin(ek) * n / (1.0 - e * Math.cos(ek))
				+ 2.0 * (crs * Math.cos(2.0 * uk) - crc * Math.sin(2.0 * uk)) * takdot;
		double ikdot = idot + (cis * Math.cos(2.0 * uk) - cic * Math.sin(2.0 * uk)) * 2.0 * takdot;

		double xpk = rk * Math.cos(uk);
		double ypk = rk * Math.sin(uk);

		double xpkdot = rkdot * Math.cos(uk) - ypk * ukdot;
		double ypkdot = rkdot * Math.sin(uk) + xpk * ukdot;

		double xkdot = (xpkdot - ypk * Math.cos(ik) * omegakdot) * Math.cos(Omega)
				- (xpk * omegakdot + ypkdot * Math.cos(ik) - ypk * Math.sin(ik) * ikdot) * Math.sin(Omega);
		double ykdot = (xpkdot - ypk * Math.cos(ik) * omegakdot) * Math.sin(Omega)
				+ (xpk * omegakdot + ypkdot * Math.cos(ik) - ypk * Math.sin(ik) * ikdot) * Math.cos(Omega);
		double zkdot = ypkdot * Math.sin(ik) + ypk * Math.cos(ik) * ikdot;

		sp.setSpeed(xkdot, ykdot, zkdot);

		return sp;
	}

	private SimpleMatrix satellite_motion_diff_eq(SimpleMatrix pos1Array, SimpleMatrix vel1Array, SimpleMatrix accArray,
			long ellAGlo, double gmGlo, double j2Glo, double omegaeDotGlo) {
		// TODO Auto-generated method stub

		/* renaming variables for better readability position */
		double X = pos1Array.get(0);
		double Y = pos1Array.get(1);
		double Z = pos1Array.get(2);

		// System.out.println("X: " + X);
		// System.out.println("Y: " + Y);
		// System.out.println("Z: " + Z);

		/* velocity */
		double Xv = vel1Array.get(0);
		double Yv = vel1Array.get(1);

		// System.out.println("Xv: " + Xv);
		// System.out.println("Yv: " + Yv);

		/* acceleration (i.e. perturbation) */
		double Xa = accArray.get(0);
		double Ya = accArray.get(1);
		double Za = accArray.get(2);

		// System.out.println("Xa: " + Xa);
		// System.out.println("Ya: " + Ya);
		// System.out.println("Za: " + Za);

		/* parameters */
		double r = Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2) + Math.pow(Z, 2));
		double g = -gmGlo / Math.pow(r, 3);
		double h = j2Glo * 1.5 * Math.pow((ellAGlo / r), 2);
		double k = 5 * Math.pow(Z, 2) / Math.pow(r, 2);

		// System.out.println("r: " + r);
		// System.out.println("g: " + g);
		// System.out.println("h: " + h);
		// System.out.println("k: " + k);

		/* differential velocity */
		double[] vel_dot = new double[3];
		vel_dot[0] = g * X * (1 - h * (k - 1)) + Xa + Math.pow(omegaeDotGlo, 2) * X + 2 * omegaeDotGlo * Yv;
		// System.out.println("vel1: " + vel_dot[0]);

		vel_dot[1] = g * Y * (1 - h * (k - 1)) + Ya + Math.pow(omegaeDotGlo, 2) * Y - 2 * omegaeDotGlo * Xv;
		// System.out.println("vel2: " + vel_dot[1]);

		vel_dot[2] = g * Z * (1 - h * (k - 3)) + Za;
		// System.out.println("vel3: " + vel_dot[2]);

		SimpleMatrix velDotArray = new SimpleMatrix(1, 3, true, vel_dot);
		// velDotArray.print();

		return velDotArray;
	}

	/**
	 * @param time
	 *            (Uncorrected Galileo time)
	 * @return Galileo time accounting for beginning or end of week crossover
	 */
	protected double checkGpsTime(double time) {

		// Account for beginning or end of week crossover
		if (time > Constants.SEC_IN_HALF_WEEK) {
			time = time - 2 * Constants.SEC_IN_HALF_WEEK;
		} else if (time < -Constants.SEC_IN_HALF_WEEK) {
			time = time + 2 * Constants.SEC_IN_HALF_WEEK;
		}
		return time;
	}

	/**
	 * @param unixTime
     * @param receiverClockError
     * @param transmissionTime
     *
	 */
	protected SimpleMatrix computeEarthRotationCorrection(long unixTime, double receiverClockError,
			double transmissionTime) {

		// Computation of signal travel time
		// SimpleMatrix diff =
		// satellitePosition.minusXYZ(approxPos);//this.coord.minusXYZ(approxPos);
		// double rho2 = Math.pow(diff.get(0), 2) + Math.pow(diff.get(1), 2)
		// + Math.pow(diff.get(2), 2);
		// double traveltime = Math.sqrt(rho2) / Constants.SPEED_OF_LIGHT;
		double receptionTime = (new Time(unixTime)).getGpsTime();
		double traveltime = receptionTime + receiverClockError - transmissionTime;

		// Compute rotation angle
		double omegatau = Constants.EARTH_ANGULAR_VELOCITY * traveltime;

		// Rotation matrix
		double[][] data = new double[3][3];
		data[0][0] = Math.cos(omegatau);
		data[0][1] = Math.sin(omegatau);
		data[0][2] = 0;
		data[1][0] = -Math.sin(omegatau);
		data[1][1] = Math.cos(omegatau);
		data[1][2] = 0;
		data[2][0] = 0;
		data[2][1] = 0;
		data[2][2] = 1;
		SimpleMatrix R = new SimpleMatrix(data);

		return R;
		// Apply rotation
		// this.coord.ecef = R.mult(this.coord.ecef);
		// this.coord.setSMMultXYZ(R);// = R.mult(this.coord.ecef);
		// satellitePosition.setSMMultXYZ(R);// = R.mult(this.coord.ecef);

	}

	/**
	 * @param unixTime
     * @param satelliteClockError
     * @param obsPseudorange
     *
	 * @return Clock-corrected Galileo transmission time
	 */
	protected double computeClockCorrectedTransmissionTime(long unixTime, double satelliteClockError,
			double obsPseudorange) {

		double gpsTime = (new Time(unixTime)).getGpsTime();

		// Remove signal travel time from observation time
		double tRaw = (gpsTime - obsPseudorange /* this.range */ / Constants.SPEED_OF_LIGHT);

		return tRaw - satelliteClockError;
	}

	/**
	 * @param eph
     * @param unixTime
     * @param obsPseudorange
	 * @return Satellite clock error
	 */
	protected double computeSatelliteClockError(long unixTime, EphGalileo eph, double obsPseudorange) {

			double gpsTime = (new Time(unixTime)).getGpsTime();
			// Remove signal travel time from observation time
			double tRaw = (gpsTime - obsPseudorange /* this.range */ / Constants.SPEED_OF_LIGHT);

			// Compute eccentric anomaly
			double Ek = computeEccentricAnomaly(tRaw, eph);

			// Relativistic correction term computation
			// double dtr = Constants.RELATIVISTIC_ERROR_CONSTANT * eph.getE() *
			// eph.getRootA() * Math.sin(Ek);

			// Added by Sebastian (20.01.2018)
			double dtr = -2.0 * ((Math.sqrt(Constants.EARTH_GRAVITATIONAL_CONSTANT) * eph.getRootA())
					/ (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT)) * eph.getE() * Math.sin(Ek);

			// Clock error computation
			double dt = checkGpsTime(tRaw - eph.getToc());
			double timeCorrection = (eph.getAf2() * dt + eph.getAf1()) * dt + eph.getAf0() + dtr - eph.getTgd();
			double tGPS = tRaw - timeCorrection;
			dt = checkGpsTime(tGPS - eph.getToc());
			timeCorrection = (eph.getAf2() * dt + eph.getAf1()) * dt + eph.getAf0() + dtr - eph.getTgd();

			return timeCorrection;

	}

	/**
	 * @param time
	 *            (Galileo time in seconds)
	 * @param eph
	 * @return Eccentric anomaly
	 */
	protected double computeEccentricAnomaly(double time, EphGalileo eph) {

		// Semi-major axis
		double A = eph.getRootA() * eph.getRootA();

		// Time from the ephemerides reference epoch
		double tk = checkGpsTime(time - eph.getToe());

		// Computed mean motion [rad/sec]
		double n0 = Math.sqrt(Constants.EARTH_GRAVITATIONAL_CONSTANT / Math.pow(A, 3));

		// Corrected mean motion [rad/sec]
		double n = n0 + eph.getDeltaN();

		// Mean anomaly
		double Mk = eph.getM0() + n * tk;

		// Eccentric anomaly starting value
		Mk = Math.IEEEremainder(Mk + 2 * Math.PI, 2 * Math.PI);
		double Ek = Mk;

		int i;
		double EkOld, dEk;

		// Eccentric anomaly iterative computation
		int maxNumIter = 12;
		for (i = 0; i < maxNumIter; i++) {
			EkOld = Ek;
			Ek = Mk + eph.getE() * Math.sin(Ek);
			dEk = Math.IEEEremainder(Ek - EkOld, 2 * Math.PI);
			if (Math.abs(dEk) < 1e-12)
				break;
		}

		// TODO Display/log warning message
		if (i == maxNumIter)
			System.out.println("Warning: Eccentric anomaly does not converge.");

		return Ek;

	}

}
