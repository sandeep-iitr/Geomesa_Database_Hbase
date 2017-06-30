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
		
		
		//initializing the schema
		
		try
		{	
		GeomesaHbase gmh=new GeomesaHbase();
		gmh.geomesa_initialize();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//inserting  data points
		try{
			
		 JSONObject Data = new JSONObject();
		 String data_id="data_name1";
		 String data_type="double";
		 String unit = "CM";
		 String value = "25";
		 int unixTimestamp=0;
		 
		    Data.put("data_id", data_id);
		    Data.put("value", value);
		    Data.put("timeStamp", unixTimestamp);
		    
		    JSONObject location = new JSONObject();
		    location.put("lat",24.066186);//34.066186, -118.445967
		    location.put("lng",18.445967);
        
		    Data.put("location", location);
		    
		    System.out.println("Data Point to Insert is:"+Data.toString());
		    
		GeomesaHbase gmh=new GeomesaHbase();
		
		//inserting 100 readings
		  for(int i=0;i<100;i++)
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
		
		//quering 100 times
			for(int i=0;i<100;i++)
			{
			JSONArray result= gmh.Query();
			System.out.println(i+"Side of Result :"+result.size());
			System.out.println(i+"Result is:"+result.toJSONString());
			}
		
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}//end main

}//end class
