package org.destil.gpsaveraging.location;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

import com.squareup.otto.Bus;

import org.destil.gpsaveraging.App;
import org.destil.gpsaveraging.location.event.CurrentLocationEvent;
import org.destil.gpsaveraging.location.event.FirstFixEvent;
import org.destil.gpsaveraging.location.event.GpsNotAvailableEvent;
import org.destil.gpsaveraging.location.event.SatellitesEvent;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper for accessing GPS.
 */
@Singleton
public class GpsObserver implements GpsStatus.Listener, LocationListener {

    private final Context mContext;
    private final Bus mBus;
    private LocationManager locationManager;

    private boolean hasFix = false;

    @Inject
    public GpsObserver(Context context, Bus bus) {
        mContext = context;
        mBus = bus;
    }

    @SuppressWarnings("ResourceType")
    public void start() {
        if (locationManager == null) {
            hasFix = false;
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates("gps", 0, 0, this);
            locationManager.addGpsStatusListener(this);
        }
    }

    @SuppressWarnings("ResourceType")
    public void stop() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager.removeGpsStatusListener(this);
        }
    }

    public boolean hasFix() {
        return hasFix;
    }

    @Override
    public void onGpsStatusChanged(int event) {
        if (locationManager == null) {
            return;
        }
        if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
            hasFix = true;
            mBus.post(new FirstFixEvent());
        } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            GpsStatus gpsStatus = locationManager.getGpsStatus(null);
            int all = 0;
            Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
            for (GpsSatellite satellite : satellites) {
                all++;
            }
            mBus.post(new SatellitesEvent(all));
        } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
            hasFix = false;
            mBus.post(new GpsNotAvailableEvent());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!hasFix) {
            hasFix = true;
            mBus.post(new FirstFixEvent());
        }
        mBus.post(new CurrentLocationEvent(location));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (status == LocationProvider.AVAILABLE) {
            hasFix = true;
            mBus.post(new FirstFixEvent());
        } else {
            hasFix = false;
            mBus.post(new GpsNotAvailableEvent());
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // ignore
    }

    @Override
    public void onProviderDisabled(String provider) {
        hasFix = false;
        mBus.post(new GpsNotAvailableEvent());
    }

    @SuppressWarnings("ResourceType")
    public Location getLastLocation() {
        if (locationManager != null) {
            return locationManager.getLastKnownLocation("gps");
        }
        return null;
    }
}