/*
 * Author: Sandeep Singh Sandha
 * Created: 29 June, 2017
 * Web Page: https://sites.google.com/view/sandeep-/home 
 */

package com.metroInsight.test;

import java.util.Random;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.omg.CORBA.INITIALIZE;

import com.metroInsight.geomesa.GeomesaHbase;

public class Geomesa_simple_test {

	static GeomesaHbase gmh;
	static int count=300;//number of data points to insert
	
	public static void main(String[] args) {	

		try {
			
			// initializing the schema
			 initialize();
			
			// inserting data points
			// insert_data(count);
			
			// querying the data points
			double lat_min=33.0;
			double lat_max=33.1;
			double lng_min=62.0;
			double lng_max=62.1;
			
			JSONArray result = gmh.Query_Box_Lat_Lng(lat_min, lat_max, lng_min, lng_max);
			System.out.println(": Side of Result _Box_Lat_Lng :" + result.size());
			if(result.size()<400)
			System.out.println(": Result of _Box_Lat_Lng is:" + result.toJSONString());
			
			long date_min = 1499110975914L;//1499110975914 System.currentTimeMillis()-1000000;//past 100 seconds
			long date_max = 1499110976514L;//1499110977514 System.currentTimeMillis();//1499110977514
			
			
			result = gmh.Query_Date_Range(date_min,date_max);
			System.out.println(": Side of Result _Date_Range :" + result.size());
			if(result.size()<400)
			System.out.println(": Result of _Date_Range is:" + result.toJSONString());
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		
	}// end main
	
	
	 static void initialize()
	 {
	   gmh = new GeomesaHbase();
	   gmh.geomesa_initialize();
	 }
	
	
	//inserting count data points in Geomesa
	 static void insert_data(int count)
	 {

			double value_min=10.0;
			double value_max=20.0;
			Random random=new Random(5771);
			
			double lat_min=30.0;
			double lng_min=60.0;
			double diff_loc=5.0;
			
			for (int i = 0; i < count; i++)
			{
			JSONObject Data = new JSONObject();
			String data_id = ""+i;
			
			double value = value_min+random.nextDouble()*(value_max-value_min);
			long millis = System.currentTimeMillis();
			
			long unixTimestamp = millis;//note time is used in millisec in the System

			double lat=lat_min+random.nextDouble()*diff_loc;
			double lng=lng_min+random.nextDouble()*diff_loc;		
			
			Data.put("data_id", data_id);
			Data.put("value", value);
			Data.put("timeStamp", unixTimestamp);

			JSONObject location = new JSONObject();
			location.put("lat", lat);
			location.put("lng", lng);

			Data.put("location", location);

			System.out.println(i+ ": Data Point to Insert is:" + Data.toString());
            gmh.geomesa_insertData(Data.toString());
         
			}//end for
			
	 }
	

}// end class
