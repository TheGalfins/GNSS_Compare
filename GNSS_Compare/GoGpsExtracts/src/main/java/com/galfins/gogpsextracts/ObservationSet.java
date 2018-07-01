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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>
 * Set of observations for one epoch and one satellite
 * </p>
 *
 * @author Eugenio Realini, Cryms.com
 */
public class ObservationSet implements Streamable {

	private final static int STREAM_V = 1;


	public final static int L1 = 0;
	public final static int L2 = 1;


	private int satID;	/* Satellite number */
	private char satType;	/* Satellite Type */

	/* Array of [L1,L2] */
	private double[] codeC = {Double.NaN,Double.NaN};			/* C Coarse/Acquisition (C/A) code [m] */
	private double[] codeP = {Double.NaN,Double.NaN};			/* P Code Pseudorange [m] */
	private double[] phase = {Double.NaN,Double.NaN};			/* L Carrier Phase [cycle] */
	private float[] signalStrength = {Float.NaN,Float.NaN};		/* C/N0 (signal strength) [dBHz] */
	private float[] doppler = {Float.NaN,Float.NaN};			/* Doppler value [Hz] */

	private int[] qualityInd = {-1,-1};	/* Nav Measurements Quality Ind. ublox proprietary? */

	/*
	 * Loss of lock indicator (LLI). Range: 0-7
	 *  0 or blank: OK or not known
	 *  Bit 0 set : Lost lock between previous and current observation: cycle slip possible
	 *  Bit 1 set : Opposite wavelength factor to the one defined for the satellite by a previous WAVELENGTH FACT L1/2 line. Valid for the current epoch only.
	 *  Bit 2 set : Observation under Antispoofing (may suffer from increased noise)
	 * Bits 0 and 1 for phase only.
	 */
	private int[] lossLockInd = {-1,-1};

	/*
	 * Signal strength indicator projected into interval 1-9:
	 *  1: minimum possible signal strength
 	 *  5: threshold for good S/N ratio
 	 *  9: maximum possible signal strength
 	 * 0 or blank: not known, don't care
	 */
	private int[] signalStrengthInd = {-1,-1};

	private int freqNum;

	/* Sets whether this obs is in use or not:
		 could be below the elevation threshold for example
     or unhealthy
  */
	private boolean inUse = false;

  /* residual error */
  public double eRes;

  /**
   * topocentric elevation
   */
  public double el;

	public ObservationSet(){
	}

	public ObservationSet(DataInputStream dai, boolean oldVersion) throws IOException{
		read(dai,oldVersion);
	}

	/**
	 * @return the satID
	 */
	public int getSatID() {
		return satID;
	}

	/**
	 * @param satID the satID to set
	 */
	public void setSatID(int satID) {
		this.satID = satID;
	}

	/**
	 * @return the satType
	 */
	public char getSatType() {
		return satType;
	}

	/**
	 * @param satType the satType to set
	 */
	public void setSatType(char satType) {
		this.satType = satType;
	}
	
	/**
	 * @return the phase range (in meters)
	 */
	public double getPhaserange(int i) {
		return phase[i] * getWavelength(i);
	}

	public double getWavelength(int i) {
		double frequency = 0;
		switch (this.satType) {
		case 'G': frequency = (i==0)?Constants.FL1:Constants.FL2;
		case 'R': frequency = (i==0)?freqNum*Constants.FR1_delta+Constants.FR1_base:freqNum*Constants.FR2_delta+Constants.FR2_base;
		case 'E': frequency = (i==0)?Constants.FE1:Constants.FE5a;
		case 'C': frequency = (i==0)?Constants.FC2:Constants.FC5b;
		case 'J': frequency = (i==0)?Constants.FJ1:Constants.FJ2;
		}
		return Constants.SPEED_OF_LIGHT/frequency;
	}
	
	/**
	 * @return the pseudorange (in meters)
	 */
	public double getPseudorange(int i) {
		return Double.isNaN(codeP[i])?codeC[i]:codeP[i];
	}

	public boolean isPseudorangeP(int i){
		return !Double.isNaN(codeP[i]);
	}

	/**
	 * @return the c
	 */
	public double getCodeC(int i) {
		return codeC[i];
	}

	/**
	 * @param c the c to set
	 */
	public void setCodeC(int i,double c) {
		codeC[i] = c;
	}

	/**
	 * @return the p
	 */
	public double getCodeP(int i) {
		return codeP[i];
	}

	/**
	 * @param p the p to set
	 */
	public void setCodeP(int i, double p) {
		codeP[i] = p;
	}

	/**
	 * @return the l
	 */
	public double getPhaseCycles(int i) {
		return phase[i];
	}

	/**
	 * @param l the l to set
	 */
	public void setPhaseCycles(int i, double l) {
		phase[i] = l;
	}

	/**
	 * @return the s
	 */
	public float getSignalStrength(int i) {
		return signalStrength[i];
	}

	/**
	 * @param s the s to set
	 */
	public void setSignalStrength(int i, float s) {
		signalStrength[i] = s;
	}

	/**
	 * @return the d
	 */
	public float getDoppler(int i) {
		return doppler[i];
	}

	/**
	 * @param d the d to set
	 */
	public void setDoppler(int i, float d) {
		doppler[i] = d;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ObservationSet){
			return ((ObservationSet)obj).getSatID() == satID;
		}else{
			return super.equals(obj);
		}
	}

	/**
	 * @return the qualityInd
	 */
	public int getQualityInd(int i) {
		return qualityInd[i];
	}

	/**
	 * @param qualityInd the qualityInd to set
	 */
	public void setQualityInd(int i,int qualityInd) {
		this.qualityInd[i] = qualityInd;
	}

	/**
	 * @return the lossLockInd
	 */
	public int getLossLockInd(int i) {
		return lossLockInd[i];
	}

	/**
	 * @param lossLockInd the lossLockInd to set
	 */
	public void setLossLockInd(int i,int lossLockInd) {
		this.lossLockInd[i] = lossLockInd;
	}

	public boolean isLocked(int i){
		return lossLockInd[i] == 0;
	}
	public boolean isPossibleCycleSlip(int i){
		return lossLockInd[i]>0 && ((lossLockInd[i]&0x1) == 0x1);
	}
	public boolean isHalfWavelength(int i){
		return lossLockInd[i]>0 && ((lossLockInd[i]&0x2) == 0x2);
	}
	public boolean isUnderAntispoof(int i){
		return lossLockInd[i]>0 && ((lossLockInd[i]&0x4) == 0x4);
	}

	public int write(DataOutputStream dos) throws IOException{
		int size = 0;
		dos.writeUTF(MESSAGE_OBSERVATIONS_SET); // 5

		dos.writeInt(STREAM_V); size +=4;
		dos.write(satID);size +=1;		// 1
		dos.write(satType);size +=1;		// 1
		// L1 data
		dos.write((byte)qualityInd[L1]);	size+=1;
		dos.write((byte)lossLockInd[L1]);	size+=1;
		dos.writeDouble(codeC[L1]); size+=8;
		dos.writeDouble(codeP[L1]); size+=8;
		dos.writeDouble(phase[L1]); size+=8;
		dos.writeFloat(signalStrength[L1]); size+=4;
		dos.writeFloat(doppler[L1]); size+=4;
		// write L2 data ?
		boolean hasL2 = false;
		if(!Double.isNaN(codeC[L2])) hasL2 = true;
		if(!Double.isNaN(codeP[L2])) hasL2 = true;
		if(!Double.isNaN(phase[L2])) hasL2 = true;
		if(!Float.isNaN(signalStrength[L2])) hasL2 = true;
		if(!Float.isNaN(doppler[L2])) hasL2 = true;
		dos.writeBoolean(hasL2); size+=1;
		if(hasL2){
			dos.write((byte)qualityInd[L2]);	size+=1;
			dos.write((byte)lossLockInd[L2]);	size+=1;
			dos.writeDouble(codeC[L2]); size+=8;
			dos.writeDouble(codeP[L2]); size+=8;
			dos.writeDouble(phase[L2]); size+=8;
			dos.writeFloat(signalStrength[L2]); size+=4;
			dos.writeFloat(doppler[L2]); size+=4;
		}
		return size;
	}

	/* (non-Javadoc)
	 * @see org.gogpsproject.Streamable#read(java.io.DataInputStream)
	 */
	@Override
	public void read(DataInputStream dai, boolean oldVersion) throws IOException {
		int v = 1;
		if(!oldVersion) v = dai.readInt();

		if(v==1){
			satID = dai.read();
			satType = (char) dai.read();

			// L1 data
			qualityInd[L1] = (int)dai.read();
			if(qualityInd[L1]==255) qualityInd[L1] = -1;
			lossLockInd[L1] = (int)dai.read();
			if(lossLockInd[L1]==255) lossLockInd[L1] = -1;
			codeC[L1] = dai.readDouble();
			codeP[L1] = dai.readDouble();
			phase[L1] = dai.readDouble();
			signalStrength[L1] = dai.readFloat();
			doppler[L1] = dai.readFloat();
			if(dai.readBoolean()){
				// L2 data
				qualityInd[L2] = (int)dai.read();
				if(qualityInd[L2]==255) qualityInd[L2] = -1;
				lossLockInd[L2] = (int)dai.read();
				if(lossLockInd[L2]==255) lossLockInd[L2] = -1;
				codeC[L2] = dai.readDouble();
				codeP[L2] = dai.readDouble();
				phase[L2] = dai.readDouble();
				signalStrength[L2] = dai.readFloat();
				doppler[L2] = dai.readFloat();
			}
		}else{
			throw new IOException("Unknown format version:"+v);
		}
	}

	/**
	 * @param signalStrengthInd the signalStrengthInd to set
	 */
	public void setSignalStrengthInd(int i,int signalStrengthInd) {
		this.signalStrengthInd[i] = signalStrengthInd;
	}

	/**
	 * @return the signalStrengthInd
	 */
	public int getSignalStrengthInd(int i) {
		return signalStrengthInd[i];
	}
	
	/**
	 * @param signalStrengthInd the signalStrengthInd to set
	 */
	public void setFreqNum(int freqNum) {
		this.freqNum = freqNum;
	}

	/**
	 * @return the signalStrengthInd
	 */
	public int getFreqNum(int i) {
		return freqNum;
	}

  public boolean inUse() {
    return isInUse();
  }
  
  public void inUse(boolean inUse) {
    this.setInUse(inUse);
  }

  public boolean isInUse() {
    return inUse;
  }

  public void setInUse(boolean inUse) {
    this.inUse = inUse;
  }
}