package com.galfins.gogpsextracts;

import java.io.File;
import java.io.InputStream;

public class RinexNavigationSpeedParserGps extends RinexNavigationParserGps {

  public RinexNavigationSpeedParserGps() {
   super((File) null);
  }

  public RinexNavigationSpeedParserGps(File fileNav) {
    super(fileNav);
  }

  public RinexNavigationSpeedParserGps(InputStream is, File cache) {
    super(is, cache);
  }

  public SatellitePosition computePositionGps(Observations obs, int satID, char satType, EphGps eph, double receiverClockError) {
    return computePositionSpeedGps(obs, satID, satType, eph, receiverClockError );
  }
  
}
