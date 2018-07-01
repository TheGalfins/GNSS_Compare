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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>
 * Observations class
 * </p>
 *
 * @author Eugenio Realini, Cryms.com
 */
public class Observations implements Streamable {

	SimpleDateFormat sdfHeader = getGMTdf();
	DecimalFormat dfX4 = new DecimalFormat("0.0000");

	private final static int STREAM_V = 1;

	private Time refTime; /* Reference time of the dataset */
	private int eventFlag; /* Event flag */

	private ArrayList<ObservationSet> obsSet; /* sets of observations */
	private int issueOfData = -1;
  public int index;

	/**
	 * The Rinex filename
	 */
	public String rinexFileName;
	
	public static SimpleDateFormat getGMTdf(){
	  SimpleDateFormat sdfHeader = new SimpleDateFormat("dd-MMM-yy HH:mm:ss");
    sdfHeader.setTimeZone( TimeZone.getTimeZone("GMT"));
    return sdfHeader;
	}

	public Object clone(){
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			this.write(new DataOutputStream(baos));
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
			baos.reset();
			dis.readUTF();
			return new Observations(dis, false);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		return null;
	}

	public Observations(Time time, int flag){
		this.refTime = time;
		this.eventFlag = flag;
	}
	
	public Observations(DataInputStream dai, boolean oldVersion) throws IOException{
		read(dai, oldVersion);
	}
	
	public void cleanObservations(){
		if(obsSet != null)
			for (int i=obsSet.size()-1;i>=0;i--)
				if(obsSet.get(i)==null || Double.isNaN(obsSet.get(i).getPseudorange(0)))
					obsSet.remove(i);
	}
	
	public int getNumSat(){
		if(obsSet == null) return 0;
		int nsat = 0;
		for(int i=0;i<obsSet.size();i++)
			if(obsSet.get(i)!=null) nsat++;
		return obsSet==null?-1:nsat;
	}
	
	public ObservationSet getSatByIdx(int idx){
		return obsSet.get(idx);
	}
	
	public ObservationSet getSatByID(Integer satID){
		if(obsSet == null || satID==null) return null;
		for(int i=0;i<obsSet.size();i++)
			if(obsSet.get(i)!=null && obsSet.get(i).getSatID()==satID.intValue()) return obsSet.get(i);
		return null;
	}
	
	public ObservationSet getSatByIDType(Integer satID, char satType){
		if(obsSet == null || satID==null) return null;
		for(int i=0;i<obsSet.size();i++)
			if(obsSet.get(i)!=null && obsSet.get(i).getSatID()==satID.intValue() && obsSet.get(i).getSatType()==satType) return obsSet.get(i);
		return null;
	}
	
//	public ObservationSet getGpsByID(char satGnss){
//		String sub = String.valueOf(satGnss); 
//		String str = sub.substring(0, 1);  
//		char satType = str.charAt(0);
//		sub = sub.substring(1, 3);  
//		Integer satID = Integer.parseInt(sub);
//		
//		if(gps == null || satID==null) return null;
//		for(int i=0;i<gps.size();i++)
//			if(gps.get(i)!=null && gps.get(i).getSatID()==satID.intValue() && gps.get(i).getSatType()==satType) return gps.get(i);
//		return null;
//	}
	
	public Integer getSatID(int idx){
		return getSatByIdx(idx).getSatID();
	}
	
	public char getGnssType(int idx){
		return getSatByIdx(idx).getSatType();
	}
	
	public boolean containsSatID(Integer id){
		return getSatByID(id) != null;
	}
	
	public boolean containsSatIDType(Integer id, Character satType){
		return getSatByIDType(id, satType) != null;
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
	 * Epoch flag
	 * 0: OK
	 * 1: power failure between previous and current epoch
	 * >1: Special event
	 *  2: start moving antenna
     *  3: new site occupation
     *  (end of kinem. data)
     * (at least MARKER NAME record
     * follows)
     * 4: header information follows
     * 5: external event (epoch is significant)
     * 6: cycle slip records follow
     * to optionally report detected
     * and repaired cycle slips
     * (same format as OBSERVATIONS
     * records; slip instead of observation;
     * LLI and signal strength blank)
     *
	 * @return the eventFlag
	 */
	public int getEventFlag() {
		return eventFlag;
	}

	/**
	 * @param eventFlag the eventFlag to set
	 */
	public void setEventFlag(int eventFlag) {
		this.eventFlag = eventFlag;
	}

//	public void init(int nGps, int nGlo, int nSbs){
//		gpsSat = new ArrayList<Integer>(nGps);
//		gloSat = new ArrayList<Integer>(nGlo);
//		sbsSat = new ArrayList<Integer>(nSbs);
//
//		// Allocate array of observation objects
//		if (nGps > 0) gps = new ObservationSet[nGps];
//		if (nGlo > 0) glo = new ObservationSet[nGlo];
//		if (nSbs > 0) sbs = new ObservationSet[nSbs];
//	}

	public void setGps(int i, ObservationSet os ){
		if(obsSet==null) obsSet = new ArrayList<ObservationSet>(i+1);
		if(i==obsSet.size()){
			obsSet.add(os);
		}else{
			int c=obsSet.size();
			while(c++<=i) obsSet.add(null);
			obsSet.set(i,os);
		}
		//gps[i] = os;
		//gpsSat.add(os.getSatID());
	}

	public int write(DataOutputStream dos) throws IOException{
		dos.writeUTF(MESSAGE_OBSERVATIONS); // 5
		dos.writeInt(STREAM_V); // 4
		dos.writeLong(refTime==null?-1:refTime.getMsec()); // 13
		dos.writeDouble(refTime==null?-1:refTime.getFraction());
		dos.write(eventFlag); // 14
		dos.write(obsSet==null?0:obsSet.size()); // 15
		int size=19;
		if(obsSet!=null){
			for(int i=0;i<obsSet.size();i++){
				size += ((ObservationSet)obsSet.get(i)).write(dos);
			}
		}
		return size;
	}
	
	public String toString(){

		String lineBreak = System.getProperty("line.separator");

		String out= " GPS Time:"+getRefTime().getGpsTime()+" "+sdfHeader.format(new Date(getRefTime().getMsec()))+" evt:"+eventFlag+lineBreak;
		for(int i=0;i<getNumSat();i++){
			ObservationSet os = getSatByIdx(i);
			out+="satType:"+ os.getSatType() +"  satID:"+os.getSatID()+"\tC:"+fd(os.getCodeC(0))
				+" cP:"+fd(os.getCodeP(0))
				+" Ph:"+fd(os.getPhaseCycles(0))
				+" Dp:"+fd(os.getDoppler(0))
				+" Ss:"+fd(os.getSignalStrength(0))
				+" LL:"+fd(os.getLossLockInd(0))
				+" LL2:"+fd(os.getLossLockInd(1))
				+lineBreak;
		}
		return out;
	}

	private String fd(double n){
		return Double.isNaN(n)?"NaN":dfX4.format(n);
	}
	
	/* (non-Javadoc)
	 * @see org.gogpsproject.Streamable#read(java.io.DataInputStream)
	 */
	@Override
	public void read(DataInputStream dai, boolean oldVersion) throws IOException {
		int v=1;
		if(!oldVersion) v=dai.readInt();

		if(v==1){
			refTime = new Time(dai.readLong(), dai.readDouble());
			eventFlag = dai.read();
			int size = dai.read();
			obsSet = new ArrayList<ObservationSet>(size);

			for(int i=0;i<size;i++){
				if(!oldVersion) dai.readUTF();
				ObservationSet os = new ObservationSet(dai, oldVersion);
				obsSet.add(os);
			}
		}else{
			throw new IOException("Unknown format version:"+v);
		}
	}

	public void setIssueOfData(int iOD) {
		this.issueOfData = iOD;
	}

	public int getIssueOfData() {
		return this.issueOfData;
	}
}
