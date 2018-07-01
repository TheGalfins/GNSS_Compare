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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Streamable {

	public final static String MESSAGE_OBSERVATIONS = "obs";
	public final static String MESSAGE_IONO = "ion";
	public final static String MESSAGE_EPHEMERIS = "eph";
	public final static String MESSAGE_OBSERVATIONS_SET = "eps";
	public final static String MESSAGE_COORDINATES = "coo";

	public int write(DataOutputStream dos) throws IOException;
	public void read(DataInputStream dai, boolean oldVersion) throws IOException;
}
