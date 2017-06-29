package com.metroInsight.geomesa;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.cli.*;
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
import java.util.Random;


public class GeomesaHbase {

	static String TABLE_NAME = "bigtable.table.name".replace(".", "_");

    // sub-set of parameters that are used to create the HBase DataStore
    static String[] HBASE_CONNECTION_PARAMS = new String[]{
            TABLE_NAME
    };
    
    
    /**
     * Creates a common set of command-line options for the parser.  Each option
     * is described separately.
     */
    static Options getCommonRequiredOptions() {
        Options options = new Options();

        @SuppressWarnings("static-access")
		Option tableNameOpt = OptionBuilder.withArgName(TABLE_NAME)
                .hasArg()
                .isRequired()
                .withDescription("table name")
                .create(TABLE_NAME);
        options.addOption(tableNameOpt);

        return options;
    }

    static Map<String, Serializable> getHBaseDataStoreConf(CommandLine cmd) {
        Map<String , Serializable> dsConf = new HashMap<>();
        for (String param : HBASE_CONNECTION_PARAMS) {
            dsConf.put(param.replace("_", "."), cmd.getOptionValue(param));
        }
        return dsConf;
    }
    

    static SimpleFeatureType createSimpleFeatureType(String simpleFeatureTypeName)
            throws SchemaException {

        // list the attributes that constitute the feature type
        List<String> attributes = Lists.newArrayList(
                "name:String",
                "value:java.lang.Long",     
                "date:Date",               
                "*Where:Point:srid=4326",  
                "data_type:String",
                "unit:String"
        );
        
        // create the bare simple-feature type
        String simpleFeatureTypeSchema = Joiner.on(",").join(attributes);
        SimpleFeatureType simpleFeatureType =
                DataUtilities.createType(simpleFeatureTypeName, simpleFeatureTypeSchema);

        return simpleFeatureType;
    }

    
    static FeatureCollection createNewFeatures(SimpleFeatureType simpleFeatureType, String data) {
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();

        String id;
        Object[] NO_VALUES = {};
       
            // create the new (unique) identifier and empty feature shell
            id = "Observation." + Integer.toString(1);
            SimpleFeature simpleFeature = SimpleFeatureBuilder.build(simpleFeatureType, NO_VALUES, id);
/*
            "name:String",
            "value:java.lang.Long",     
            "date:Date",               
            "*location:Point:srid=4326",  
            "data_type:String",
            "unit:String"
 */      
            try{
              JSONParser parser = new JSONParser();
			  JSONObject obj2=(JSONObject)parser.parse(data);
			 
			  String name=(String) obj2.get("name");
			  String unit =(String)obj2.get("unit");
			  
			  JSONObject loc=(JSONObject)obj2.get("location");
			  double lat =(Double)loc.get("lat");
			  double lng =(Double)loc.get("lng");
			  
			  Long timestamp =(Long)obj2.get("timeStamp");
			  
			  String value = (String) obj2.get("value");
			  
			  String data_type = (String)obj2.get("data_type");
			  
			    DateTime dateTime=new DateTime(timestamp);
	            //simpleFeature.getUserData().put(Hints.USE_PROVIDED_FID, java.lang.Boolean.TRUE);
	            simpleFeature.setAttribute("name",name);
	            simpleFeature.setAttribute("unit",unit);
	            simpleFeature.setAttribute("value",value);
	            simpleFeature.setAttribute("data_type", data_type);
	            Geometry geometry = WKTUtils$.MODULE$.read("POINT(" + lat + " " + lng + ")");
	            simpleFeature.setAttribute("Where", geometry);
	            simpleFeature.setAttribute("date", dateTime.toDate());

	            // Why: another string value
	            // left empty, showing that not all attributes need values

	            // accumulate this new feature in the collection
	            featureCollection.add(simpleFeature);
            }
            catch(Exception e){
           
            	e.printStackTrace();
            }
			 //System.out.print("Record Num: "+obj2.get("RecordNum"));
			// System.out.print(" DataType: "+obj2.get("DataType"));
			 //System.out.print(" DataValue: "+obj2.get("DataValueInt"));
			// System.out.println();
			 
            
           
       

        return featureCollection;
    }
    
    static void insertFeatures(String simpleFeatureTypeName,
            DataStore dataStore,
            FeatureCollection featureCollection)
            		throws IOException 
    {

    	FeatureStore featureStore = (FeatureStore)dataStore.getFeatureSource(simpleFeatureTypeName);
    	featureStore.addFeatures(featureCollection);
    }
    

    static Filter createFilter(String geomField, double x0, double y0, double x1, double y1
                               )
            throws CQLException, IOException {

        // there are many different geometric predicates that might be used;
        // here, we just use a bounding-box (BBOX) predicate as an example.
        // this is useful for a rectangular query area
        String cqlGeometry = "BBOX(" + geomField + ", " +
                x0 + ", " + y0 + ", " + x1 + ", " + y1 + ")";

        String cql = cqlGeometry;
        return CQL.toFilter(cql);
    }

    static JSONArray queryFeatures(String simpleFeatureTypeName,
            DataStore dataStore,
            String geomField, double x0, double y0, double x1, double y1)
            		throws CQLException, IOException 
    {

    	// construct a (E)CQL filter from the search parameters,
    	// and use that as the basis for the query
	Filter cqlFilter = createFilter(geomField, x0, y0, x1, y1);
	Query query = new Query(simpleFeatureTypeName, cqlFilter);
	
	// submit the query, and get back an iterator over matching features
	FeatureSource featureSource = dataStore.getFeatureSource(simpleFeatureTypeName);
	FeatureIterator featureItr = featureSource.getFeatures(query).features();
	
	//FeatureIterator featureItr = featureSource.getFeatures().features();
	
	 JSONArray ja=new JSONArray();
	 
	    
	// loop through all results
	int n = 0;
	while (featureItr.hasNext()) {
	Feature feature = featureItr.next();
	System.out.println((++n) + ".  " +
	  feature.getProperty("name").getValue() + "|" +
	  feature.getProperty("data_type").getValue() + "|" +
	  feature.getProperty("value").getValue() + "|" +
	  feature.getProperty("date").getValue() + "|" +
	  feature.getProperty("Where").getValue() + "|" +
	  feature.getProperty("unit").getValue());
	
	    JSONObject Data = new JSONObject();
	    Data.put("name", feature.getProperty("name").getValue());
	    Data.put("data_type", feature.getProperty("data_type").getValue());
	    Data.put("unit", feature.getProperty("value").getValue());
	    Data.put("value", "25");
	    Data.put("date", feature.getProperty("date").getValue());
	    Data.put("where", feature.getProperty("Where").getValue());
	    Data.put("unit", feature.getProperty("unit").getValue());
	    
	    ja.add(Data);
	    
	}
	featureItr.close();
	
	return ja;
	}


    
    
    
    
    public  void geomesa_insertData(String data) {
        // find out where -- in HBase -- the user wants to store data
        CommandLineParser parser = new BasicParser();
        Options options = getCommonRequiredOptions();
        
    try{
        String[] args =new String[2];
        args[0]="--bigtable_table_name";
        args[1]="Geomesa";
        
        CommandLine cmd=null;
		
			cmd = parser.parse( options, args);
		
       
        // verify that we can see this HBase destination in a GeoTools manner
        Map<String, Serializable> dsConf = getHBaseDataStoreConf(cmd);
        DataStore dataStore;
		
			dataStore = DataStoreFinder.getDataStore(dsConf);
			assert dataStore != null;
			

        // establish specifics concerning the SimpleFeatureType to store
        String simpleFeatureTypeName = "MetroInsight";
        SimpleFeatureType simpleFeatureType = createSimpleFeatureType(simpleFeatureTypeName);

        // write Feature-specific metadata to the destination table in HBase
        // (first creating the table if it does not already exist); you only need
        // to create the FeatureType schema the *first* time you write any Features
        // of this type to the table
        System.out.println("Creating feature-type (schema):  " + simpleFeatureTypeName);
        dataStore.createSchema(simpleFeatureType);

        // create new features locally, and add them to this table
        System.out.println("Creating new features");
        FeatureCollection featureCollection = createNewFeatures(simpleFeatureType, data);
        System.out.println("Inserting new features");
        insertFeatures(simpleFeatureTypeName, dataStore, featureCollection);
        System.out.println("done inserting Data");
        
       
        /*
        //querying Data now, results as shown below:
        System.out.println("querying Data now, results as shown below:");
        Query();
        */
        System.out.println("Done");
        
    }//end try
    catch(Exception e)
    {
    
    	e.printStackTrace();
    }
    
    }//end function
	
    public JSONArray Query()
    {
    	try{
    	  CommandLineParser parser = new BasicParser();
          Options options = getCommonRequiredOptions();
          
    	  String[] args =new String[2];
          args[0]="--bigtable_table_name";
          args[1]="Geomesa";
          
          CommandLine cmd=null;
  		
  			cmd = parser.parse( options, args);
  			
    	String simpleFeatureTypeName = "MetroInsight";
    	// verify that we can see this HBase destination in a GeoTools manner
        Map<String, Serializable> dsConf = getHBaseDataStoreConf(cmd);
        DataStore dataStore;
		
			dataStore = DataStoreFinder.getDataStore(dsConf);
			assert dataStore != null;
			
			// water data|temperature|25|Thu Sep 01 06:28:00 PDT 2016|POINT (-77.5577293170671 38.384020026677035)|degree celcius
    	// query a few Features from this table
        System.out.println("Submitting query");
        JSONArray result=queryFeatures(simpleFeatureTypeName, dataStore,
                "Where", -76.5, 37.5, -78.0, 39.0);
        
        return result;
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
		return null;
    	
    	
    }
    
	}//end class
