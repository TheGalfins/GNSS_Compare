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

import android.location.cts.nano.Ephemeris;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>
 * GPS broadcast ephemerides
 * </p>
 *
 * @author Eugenio Realini, Cryms.com, Daisuke Yoshida 
 */

public class EphGps implements Streamable {
	private final static int STREAM_V = 1;

	private Time refTime; /* Reference time of the dataset */
	private char satType; /* Satellite Type */
	private int satID; /* Satellite ID number */
	private int week; /* GPS week number */

	private int L2Code; /* Code on L2 */
	private int L2Flag; /* L2 P data flag */

	private int svAccur; /* SV accuracy (URA index) */
	private int svHealth; /* SV health */

	private int iode; /* Issue of data (ephemeris) */
	private int iodc; /* Issue of data (clock) */

	private double toc; /* clock data reference time */
	private double toe; /* ephemeris reference time */
	private double tom; /* transmission time of message */

	/* satellite clock parameters */
	private double af0;
	private double af1;
	private double af2;
	private double tgd;

	/* satellite orbital parameters */
	private double rootA; /* Square root of the semimajor axis */
	private double e; /* Eccentricity */
	private double i0; /* Inclination angle at reference time */
	private double iDot; /* Rate of inclination angle */
	private double omega; /* Argument of perigee */
	private double omega0; /*
	 * Longitude of ascending node of orbit plane at beginning
	 * of week
	 */
	private double omegaDot; /* Rate of right ascension */
	private double M0; /* Mean anomaly at reference time */
	private double deltaN; /* Mean motion difference from computed value */
	private double crc, crs, cuc, cus, cic, cis; /*
	 * Amplitude of second-order harmonic
	 * perturbations
	 */
	private long fitInt; /* Fit interval */

	/* for GLONASS data */
	private float tow;

	private float tauN;
	private float gammaN;
	private double tk;

	private double X;
	private double Xv;
	private double Xa;
	private double Bn;

	private double Y;
	private double Yv;
	private double Ya;
	private int freq_num;
	private double tb;

	private double Z;
	private double Zv;
	private double Za;
	private double En;

  public static final EphGps UnhealthyEph = new EphGps();


	public EphGps(){

	}
	public EphGps(DataInputStream dai, boolean oldVersion) throws IOException{
		read(dai,oldVersion);
	}

	public EphGps(Ephemeris.GpsEphemerisProto ephemerids) {
		this.satID = ephemerids.prn;
		this.satType = 'G';
        this.week = ephemerids.week;
        this.L2Code = ephemerids.l2Code;
        this.L2Flag = ephemerids.l2Flag;
		this.svAccur = (int) ephemerids.svAccuracyM;
		this.svHealth = ephemerids.svHealth;
        this.iodc = ephemerids.iodc;
        this.iode = ephemerids.iode;
        this.toc = ephemerids.toc;
        this.toe = ephemerids.toe;
        this.af0 = ephemerids.af0;
        this.af1 = ephemerids.af1;
        this.af2 = ephemerids.af2;
        this.tgd  =ephemerids.tgd;
        this.rootA = ephemerids.rootOfA;
        this.e = ephemerids.e;
        this.i0 = ephemerids.i0;
        this.iDot = ephemerids.iDot;
        this.omega = ephemerids.omega;
        this.omega0= ephemerids.omega0;
        this.omegaDot = ephemerids.omegaDot;
        this.M0 = ephemerids.m0;
        this.deltaN = ephemerids.deltaN;
        this.cic = ephemerids.cic;
        this.cis = ephemerids.cis;
        this.crc = ephemerids.crc;
        this.crs = ephemerids.crs;
        this.cuc = ephemerids.cuc;
        this.cus = ephemerids.cus;
        this.fitInt = (long) ephemerids.fitInterval;
        this.refTime = new Time(week, toc);
	}

    /**
	 * @return the refTime
	 */
	public Time getRefTime() {
		return refTime;
	}
	/**
	 * @param refTime the refTime to set
	 */
	public void setRefTime(Time refTime) {
		this.refTime = refTime;
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
	 * @return the week
	 */
	public int getWeek() {
		return week;
	}
	/**
	 * @param week the week to set
	 */
	public void setWeek(int week) {
		this.week = week;
	}
	/**
	 * @return the l2Code
	 */
	public int getL2Code() {
		return L2Code;
	}
	/**
	 * @param l2Code the l2Code to set
	 */
	public void setL2Code(int l2Code) {
		L2Code = l2Code;
	}
	/**
	 * @return the l2Flag
	 */
	public int getL2Flag() {
		return L2Flag;
	}
	/**
	 * @param l2Flag the l2Flag to set
	 */
	public void setL2Flag(int l2Flag) {
		L2Flag = l2Flag;
	}
	/**
	 * @return the svAccur
	 */
	public int getSvAccur() {
		return svAccur;
	}
	/**
	 * @param svAccur the svAccur to set
	 */
	public void setSvAccur(int svAccur) {
		this.svAccur = svAccur;
	}
	/**
	 * @return the svHealth
	 */
	public int getSvHealth() {
		return svHealth;
	}
	/**
	 * @param svHealth the svHealth to set
	 */
	public void setSvHealth(int svHealth) {
		this.svHealth = svHealth;
	}
	/**
	 * @return the iode
	 */
	public int getIode() {
		return iode;
	}
	/**
	 * @param iode the iode to set
	 */
	public void setIode(int iode) {
		this.iode = iode;
	}
	/**
	 * @return the iodc
	 */
	public int getIodc() {
		return iodc;
	}
	/**
	 * @param iodc the iodc to set
	 */
	public void setIodc(int iodc) {
		this.iodc = iodc;
	}
	/**
	 * @return the toc
	 */
	public double getToc() {
		return toc;
	}
	/**
	 * @param toc the toc to set
	 */
	public void setToc(double toc) {
		this.toc = toc;
	}
	/**
	 * @return the toe
	 */
	public double getToe() {
		return toe;
	}
	/**
	 * @param toe the toe to set
	 */
	public void setToe(double toe) {
		this.toe = toe;
	}
	/**
	 * @return the tom
	 */
	public double getTom() {
		return tom;
	}
	/**
	 * @param tom the tom to set
	 */
	public void setTom(double tom) {
		this.tom = tom;
	}
	/**
	 * @return the af0
	 */
	public double getAf0() {
		return af0;
	}
	/**
	 * @param af0 the af0 to set
	 */
	public void setAf0(double af0) {
		this.af0 = af0;
	}
	/**
	 * @return the af1
	 */
	public double getAf1() {
		return af1;
	}
	/**
	 * @param af1 the af1 to set
	 */
	public void setAf1(double af1) {
		this.af1 = af1;
	}
	/**
	 * @return the af2
	 */
	public double getAf2() {
		return af2;
	}
	/**
	 * @param af2 the af2 to set
	 */
	public void setAf2(double af2) {
		this.af2 = af2;
	}
	/**
	 * @return the tgd
	 */
	public double getTgd() {
		return tgd;
	}
	/**
	 * @param tgd the tgd to set
	 */
	public void setTgd(double tgd) {
		this.tgd = tgd;
	}
	/**
	 * @return the rootA
	 */
	public double getRootA() {
		return rootA;
	}
	/**
	 * @param rootA the rootA to set
	 */
	public void setRootA(double rootA) {
		this.rootA = rootA;
	}
	/**
	 * @return the e
	 */
	public double getE() {
		return e;
	}
	/**
	 * @param e the e to set
	 */
	public void setE(double e) {
		this.e = e;
	}
	/**
	 * @return the i0
	 */
	public double getI0() {
		return i0;
	}
	/**
	 * @param i0 the i0 to set
	 */
	public void setI0(double i0) {
		this.i0 = i0;
	}
	/**
	 * @return the iDot
	 */
	public double getiDot() {
		return iDot;
	}
	/**
	 * @param iDot the iDot to set
	 */
	public void setiDot(double iDot) {
		this.iDot = iDot;
	}
	/**
	 * @return the omega
	 */
	public double getOmega() {
		return omega;
	}
	/**
	 * @param omega the omega to set
	 */
	public void setOmega(double omega) {
		this.omega = omega;
	}
	/**
	 * @return the omega0
	 */
	public double getOmega0() {
		return omega0;
	}
	/**
	 * @param omega0 the omega0 to set
	 */
	public void setOmega0(double omega0) {
		this.omega0 = omega0;
	}
	/**
	 * @return the omegaDot
	 */
	public double getOmegaDot() {
		return omegaDot;
	}
	/**
	 * @param omegaDot the omegaDot to set
	 */
	public void setOmegaDot(double omegaDot) {
		this.omegaDot = omegaDot;
	}
	/**
	 * @return the m0
	 */
	public double getM0() {
		return M0;
	}
	/**
	 * @param m0 the m0 to set
	 */
	public void setM0(double m0) {
		M0 = m0;
	}
	/**
	 * @return the deltaN
	 */
	public double getDeltaN() {
		return deltaN;
	}
	/**
	 * @param deltaN the deltaN to set
	 */
	public void setDeltaN(double deltaN) {
		this.deltaN = deltaN;
	}
	/**
	 * @return the crc
	 */
	public double getCrc() {
		return crc;
	}
	/**
	 * @param crc the crc to set
	 */
	public void setCrc(double crc) {
		this.crc = crc;
	}
	/**
	 * @return the crs
	 */
	public double getCrs() {
		return crs;
	}
	/**
	 * @param crs the crs to set
	 */
	public void setCrs(double crs) {
		this.crs = crs;
	}
	/**
	 * @return the cuc
	 */
	public double getCuc() {
		return cuc;
	}
	/**
	 * @param cuc the cuc to set
	 */
	public void setCuc(double cuc) {
		this.cuc = cuc;
	}
	/**
	 * @return the cus
	 */
	public double getCus() {
		return cus;
	}
	/**
	 * @param cus the cus to set
	 */
	public void setCus(double cus) {
		this.cus = cus;
	}
	/**
	 * @return the cic
	 */
	public double getCic() {
		return cic;
	}
	/**
	 * @param cic the cic to set
	 */
	public void setCic(double cic) {
		this.cic = cic;
	}
	/**
	 * @return the cis
	 */
	public double getCis() {
		return cis;
	}
	/**
	 * @param cis the cis to set
	 */
	public void setCis(double cis) {
		this.cis = cis;
	}
	/**
	 * @return the fitInt
	 */
	public long getFitInt() {
		return fitInt;
	}
	/**
	 * @param fitInt the fitInt to set
	 */
	public void setFitInt(long fitInt) {
		this.fitInt = fitInt;
	}


	/* for GLONASS data */

	public float getTauN() {
		return tauN;
	}
	public void setTauN(float tauN) {
		this.tauN = tauN;
	}

	public float getGammaN() {
		return gammaN;
	}
	public void setGammaN(float gammaN) {
		this.gammaN = gammaN;
	}

	public double gettk() {
		return tk;
	}
	public void settk(double tk) {
		this.tk = tk;
	}

	public double getX() {
		return X;
	}
	public void setX(double X) {
		this.X = X;
	}

	public double getXv() {
		return Xv;
	}
	public void setXv(double Xv) {
		this.Xv = Xv;
	}

	public double getXa() {
		return Xa;
	}
	public void setXa(double Xa) {
		this.Xa = Xa;
	}

	public double getBn() {
		return Bn;
	}
	public void setBn(double Bn) {
		this.Bn = Bn;
	}

	public double getY() {
		return Y;
	}
	public void setY(double Y) {
		this.Y = Y;
	}

	public double getYv() {
		return Yv;
	}
	public void setYv(double Yv) {
		this.Yv = Yv;
	}

	public double getYa() {
		return Ya;
	}
	public void setYa(double Ya) {
		this.Ya = Ya;
	}

	public int getfreq_num() {
		return freq_num;
	}
	public void setfreq_num(int freq_num) {
		this.freq_num = freq_num;
	}

	public double gettb() {
		return tb;
	}
	public void settb(double tb) {
		this.tb = tb;
	}

	public double getZ() {
		return Z;
	}
	public void setZ(double Z) {
		this.Z = Z;
	}

	public double getZv() {
		return Zv;
	}
	public void setZv(double Zv) {
		this.Zv = Zv;
	}

	public double getZa() {
		return Za;
	}
	public void setZa(double Za) {
		this.Za = Za;
	}

	public double getEn() {
		return En;
	}
	public void setEn(double En) {
		this.En = En;
	}

	//	public long getEn() {
	//		return En;
	//	}
	//	public void setEn(long En) {
	//		this.En = En;
	//	}


	/* (non-Javadoc)
	 * @see org.gogpsproject.Streamable#write(java.io.DataOutputStream)
	 */
	@Override
	public int write(DataOutputStream dos) throws IOException {
		int size=5;
		dos.writeUTF(MESSAGE_EPHEMERIS); // 5
		dos.writeInt(STREAM_V); size+=4; // 4

		dos.writeLong(refTime==null?-1:refTime.getMsec());  size +=8;
		dos.write(satID);  size +=1;
		dos.writeInt(week); size +=4;

		dos.writeInt(L2Code); size +=4;
		dos.writeInt(L2Flag); size +=4;

		dos.writeInt(svAccur); size +=4;
		dos.writeInt(svHealth); size +=4;

		dos.writeInt(iode); size +=4;
		dos.writeInt(iodc); size +=4;

		dos.writeDouble(toc); size +=8;
		dos.writeDouble(toe); size +=8;

		dos.writeDouble(af0); size +=8;
		dos.writeDouble(af1); size +=8;
		dos.writeDouble(af2); size +=8;
		dos.writeDouble(tgd); size +=8;


		dos.writeDouble(rootA); size +=8;
		dos.writeDouble(e); size +=8;
		dos.writeDouble(i0); size +=8;
		dos.writeDouble(iDot); size +=8;
		dos.writeDouble(omega); size +=8;
		dos.writeDouble(omega0); size +=8;

		dos.writeDouble(omegaDot); size +=8;
		dos.writeDouble(M0); size +=8;
		dos.writeDouble(deltaN); size +=8;
		dos.writeDouble(crc); size +=8;
		dos.writeDouble(crs); size +=8;
		dos.writeDouble(cuc); size +=8;
		dos.writeDouble(cus); size +=8;
		dos.writeDouble(cic); size +=8;
		dos.writeDouble(cis); size +=8;

		dos.writeDouble(fitInt); size +=8;

		return size;
	}
	/* (non-Javadoc)
	 * @see org.gogpsproject.Streamable#read(java.io.DataInputStream)
	 */
	@Override
	public void read(DataInputStream dai, boolean oldVersion) throws IOException {
		int v=1;
		if(!oldVersion) v=dai.readInt();

		if(v==1){
			long l = dai.readLong();
			refTime = new Time(l>0?l:System.currentTimeMillis());
			satID = dai.read();
			week = dai.readInt();
			L2Code = dai.readInt();
			L2Flag = dai.readInt();
			svAccur = dai.readInt();
			svHealth = dai.readInt();
			iode = dai.readInt();
			iodc = dai.readInt();
			toc = dai.readDouble();
			toe = dai.readDouble();
			af0 = dai.readDouble();
			af1 = dai.readDouble();
			af2 = dai.readDouble();
			tgd = dai.readDouble();
			rootA = dai.readDouble();
			e = dai.readDouble();
			i0 = dai.readDouble();
			iDot = dai.readDouble();
			omega = dai.readDouble();
			omega0 = dai.readDouble();
			omegaDot = dai.readDouble();
			M0 = dai.readDouble();
			deltaN = dai.readDouble();
			crc = dai.readDouble();
			crs = dai.readDouble();
			cuc = dai.readDouble();
			cus = dai.readDouble();
			cic = dai.readDouble();
			cis = dai.readDouble();
			fitInt = dai.readLong();
		}else{
			throw new IOException("Unknown format version:"+v);
		}
	}

}
