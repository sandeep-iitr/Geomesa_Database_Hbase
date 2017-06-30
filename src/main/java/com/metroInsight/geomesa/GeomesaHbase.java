package com.metroInsight.geomesa;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.*;
import org.geotools.factory.Hints;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.geomesa.utils.text.WKTUtils$;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GeomesaHbase {

	//DataStore is from Geotools, here it is used to return an indexed datastore.
	DataStore dataStore=null;
	static String simpleFeatureTypeName = "MetroInsight";
	static SimpleFeatureBuilder featureBuilder=null;
	
	static SimpleFeatureType createSimpleFeatureType() throws SchemaException {
		
		/*
		 * We use the DataUtilities class from Geotools to create a FeatureType that will describe the data
		 * 
		 */
		SimpleFeatureType simpleFeatureType = DataUtilities.createType(simpleFeatureTypeName,
				"point_loc:Point:srid=4326,"+// a Geometry attribute: Point type
				"data_id:String,"+// a String attribute
				"value:java.lang.Double,"+// a Long number attribute
				"date:Date"// a date attribute for time
				);
		
		return simpleFeatureType;
		
		
	}

	static FeatureCollection createNewFeatures(SimpleFeatureType simpleFeatureType, String data) {
	
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		
		if(featureBuilder==null)
		featureBuilder = new SimpleFeatureBuilder(simpleFeatureType);
		
		SimpleFeature simpleFeature=featureBuilder.buildFeature(null);
		
		/*
		 * "name:String", "value:java.lang.Double", "date:Date",
		 * "point_loc:Point:srid=4326", "data_type:String", "unit:String"
		 */
		
		try {
			JSONParser parser = new JSONParser();
			JSONObject obj2 = (JSONObject) parser.parse(data);
			String name = (String) obj2.get("data_id");
			JSONObject loc = (JSONObject) obj2.get("location");
			double lat = (Double) loc.get("lat");
			double lng = (Double) loc.get("lng");
			Long timestamp = (Long) obj2.get("timeStamp");
			
			double value = (Double) obj2.get("value");
			DateTime dateTime = new DateTime(timestamp);
			
			Geometry geometry = WKTUtils$.MODULE$.read("POINT(" + lat + " " + lng + ")");
			
			simpleFeature.setAttribute("data_id", name);
			simpleFeature.setAttribute("value", value);
			simpleFeature.setAttribute("point_loc", geometry);
			simpleFeature.setAttribute("date", dateTime.toDate());

			// accumulate this new feature in the collection
			featureCollection.add(simpleFeature);
		} catch (Exception e) {

			e.printStackTrace();
		}

		return featureCollection;
	}

	static void insertFeatures(DataStore dataStore, FeatureCollection featureCollection)
			throws IOException {

		FeatureStore featureStore = (FeatureStore) dataStore.getFeatureSource(simpleFeatureTypeName);
		featureStore.addFeatures(featureCollection);
	}

	static Filter createFilter(String geomField, double x0, double y0, double x1, double y1)
			throws CQLException, IOException {

		// there are many different geometric predicates that might be used;
		// here, we just use a bounding-box (BBOX) predicate as an example.
		// this is useful for a rectangular query area
		String cqlGeometry = "BBOX(" + geomField + ", " + x0 + ", " + y0 + ", " + x1 + ", " + y1 + ")";

		String cql = cqlGeometry;
		return CQL.toFilter(cql);
	}

	static JSONArray queryFeatures(DataStore dataStore, String geomField, double x0,
			double y0, double x1, double y1) throws CQLException, IOException {

		// construct a (E)CQL filter from the search parameters,
		// and use that as the basis for the query
		Filter cqlFilter = createFilter(geomField, x0, y0, x1, y1);
		Query query = new Query(simpleFeatureTypeName, cqlFilter);

		// submit the query, and get back an iterator over matching features
		FeatureSource featureSource = dataStore.getFeatureSource(simpleFeatureTypeName);
		FeatureIterator featureItr = featureSource.getFeatures(query).features();

		// FeatureIterator featureItr = featureSource.getFeatures().features();

		JSONArray ja = new JSONArray();

		// loop through all results
		int n = 0;
		while (featureItr.hasNext()) {
			Feature feature = featureItr.next();

			JSONObject Data = new JSONObject();
			Data.put("data_id", feature.getProperty("data_id").getValue());
			Data.put("value", feature.getProperty("value").getValue());
			Data.put("date", feature.getProperty("date").getValue());
			Data.put("point_loc", feature.getProperty("point_loc").getValue());
			ja.add(Data);

		}
		featureItr.close();

		return ja;
	}

	public void geomesa_initialize() {
		
		try {
			if (dataStore == null) {
				Map<String, Serializable> parameters = new HashMap<>();
				parameters.put("bigtable.table.name", "Geomesa");
				
				//DataStoreFinder is from Geotools, returns an indexed datastore if one is available.
				dataStore = DataStoreFinder.getDataStore(parameters);
			}

			assert dataStore != null;

			// establish specifics concerning the SimpleFeatureType to store
			String simpleFeatureTypeName = "MetroInsight";
			SimpleFeatureType simpleFeatureType = createSimpleFeatureType();

			// write Feature-specific metadata to the destination table in HBase
			// (first creating the table if it does not already exist); you only
			// need
			// to create the FeatureType schema the *first* time you write any
			// Features
			// of this type to the table
			System.out.println("Creating feature-type (schema):  " + simpleFeatureTypeName);
			dataStore.createSchema(simpleFeatureType);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void geomesa_insertData(String data) {
		// find out where -- in HBase -- the user wants to store data

		try {

			if (dataStore == null) {
				geomesa_initialize();
			}

			// establish specifics concerning the SimpleFeatureType to store
			String simpleFeatureTypeName = "MetroInsight";
			SimpleFeatureType simpleFeatureType = createSimpleFeatureType();

			// create new features locally, and add them to this table
			System.out.println("Creating new features");
			FeatureCollection featureCollection = createNewFeatures(simpleFeatureType, data);
			System.out.println("Inserting new features");
			insertFeatures(dataStore, featureCollection);
			System.out.println("done inserting Data");

			/*
			 * //querying Data now, results as shown below:
			 * System.out.println("querying Data now, results as shown below:");
			 * Query();
			 */
			System.out.println("Done");

		} // end try
		catch (Exception e) {

			e.printStackTrace();
		}

	}// end function

	public JSONArray Query() {
		try {

			if (dataStore == null) {
				geomesa_initialize();
			}
		

			// query a few Features from this table
			System.out.println("Submitting query");
			JSONArray result = queryFeatures(dataStore, "point_loc", -30, 60, -40, 70);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	
	public JSONArray Query_Box_Lat_Lng(double lat_min, double lat_max, double lng_min, double lng_max) {
		try {

			if (dataStore == null) {
				geomesa_initialize();
			}
		

			// query a few Features from this table
			System.out.println("Submitting query");
			JSONArray result = queryFeatures(dataStore, "point_loc", lat_min, lng_min, lat_max, lng_max);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}
	
	
}// end class
