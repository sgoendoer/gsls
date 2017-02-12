package net.sonic.gsls.model;

import org.json.JSONObject;

public class Trustee {
	private String globalID;
	
	protected  Trustee(){
	}
	
	protected static Trustee createFromJSONObject(JSONObject json)
	{
		Trustee t = new Trustee();
		t.globalID = json.getString("globalID");
		return t;
	}	
	
	public String getGlobalID()
	{
		return globalID;
	}
	
	public void setGlobalID(String globalID)
	{
		this.globalID = globalID;
	}


}
