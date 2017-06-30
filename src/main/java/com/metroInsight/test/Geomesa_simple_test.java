/*
 * Author: Sandeep Singh Sandha
 * Created: 29 June, 2017
 * Web Page: https://sites.google.com/view/sandeep-/home 
 */

package com.metroInsight.test;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.metroInsight.geomesa.GeomesaHbase;


public class Geomesa_simple_test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//inserting a simple data point
		try{
			
		 JSONObject Data = new JSONObject();
		 
		 String name="data_name1";
		 String data_type="double";
		 String unit = "CM";
		 String value = "25";
		 int unixTimestamp=0;
		 
		    Data.put("name", name);
		    Data.put("data_type",data_type );
		    Data.put("unit",unit );
		    Data.put("value", value);
		    Data.put("timeStamp", unixTimestamp);
		    
		    JSONObject location = new JSONObject();
		    location.put("lat",24.066186);//34.066186, -118.445967
		    location.put("lng",18.445967);
        
		    Data.put("location", location);
		    
		    System.out.println("Data Point to Insert is:"+Data.toString());
		    
		GeomesaHbase gmh=new GeomesaHbase();
        gmh.geomesa_insertData(Data.toString());

		}//end try
		
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//querying the data points
		
		try
		{
		GeomesaHbase gmh=new GeomesaHbase();
		JSONArray result= gmh.Query();
		
		System.out.println("Data returned is:"+result.toString());
		
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}//end main

}//end class
