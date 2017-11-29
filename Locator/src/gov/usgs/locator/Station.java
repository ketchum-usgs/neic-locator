package gov.usgs.locator;

/**
 * Keep all data for one seismic station here.
 * 
 * @author Ray
 *
 */
public class Station {
	// Inputs:
	StationID staID;			// Full station designation
	double latitude;			// Geographic station latitude in degrees
	double longitude;			// Station longitude in degrees
	double elevation;			// Station elevation in kilometers
	// Internal use:
	double coLat;					// Geocentric co-latitude in degrees
	double sinLat;				// Sine of the geocentric co-latitude
	double cosLat;				// Cosine of the geocentric co-latitude
	double sinLon;				// Sine of the longitude
	double cosLon;				// Cosine of the longitude
	boolean used;					// Keep track of stations that have used picks

	/**
	 * Initialize the station and compute the sines and cosines.
	 * 
	 * @param staID Station designation
	 * @param latitude Geographical latitude in degrees
	 * @param longitude Longitude in degrees
	 * @param elevation Elevation in kilometers
	 */
	public Station(StationID staID, double latitude, double longitude, 
			double elevation) {
		// Remember the inputs.
		this.staID = staID;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		// Set up the sines and cosines.
		coLat = TauUtil.geoCen(latitude);
		sinLat = Math.sin(Math.toRadians(coLat));
		cosLat = Math.cos(Math.toRadians(coLat));
		sinLon = Math.sin(Math.toRadians(longitude));
		cosLon = Math.cos(Math.toRadians(longitude));
		// Initialize used.
		used = false;
	}
	
	/**
	 * Create a toString suitable for making a station list.
	 */
	@Override
	public String toString() {
		return String.format("%-5s %-2s %-2s %8.4f %9.4f %6.2f %b", 
				staID.staCode, staID.locCode, staID.netCode, latitude, 
				longitude, elevation, used);
	}
}