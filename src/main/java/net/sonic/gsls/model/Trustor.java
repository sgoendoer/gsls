package net.sonic.gsls.model;

import org.json.JSONObject;

public class Trustor {
	private String globalID;
	private String signedMessage;
	
	protected  Trustor(){
	}
	
	protected static Trustor createFromJSONObject(JSONObject json)
	{
		Trustor t = new Trustor();
		t.globalID = json.getString("globalID");
		t.signedMessage = json.getString("signedMessage");
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

	public String getSignedMessage()
	{
		return signedMessage;
	}
	
	public void setSignedMessage(String signedMessage)
	{
		this.signedMessage = signedMessage;
	}	

}
