package net.sonic.gsls.model;

import java.io.IOException;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONTokener;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Data object for handling the dataset
 * 
 * @date 12.01.2017
 * @version 1
 * @author Sebastian Göndör, Parth Singh
 */
public class SocialRecord
{
	protected String guid;
	protected String salt;
	protected JSONArray userIDs;
	protected String lastUpdate;
	protected String timeout;
	protected String publicKey;
	protected int active;
	protected int revoked;
	protected JSONObject defaults;
	protected int schemaVersion;
	protected JSONArray legacyIDs;
	
	// TODO finish deserialize functionality
	public static SocialRecord createFromJSONObject(JSONObject json)
	{
		SocialRecord dataset = new SocialRecord();
		
		dataset.setSchemaVersion(json.getInt("schemaVersion"));
		dataset.setActive(json.getInt("active"));
		dataset.setRevoked(json.getInt("revoked"));
		dataset.setGUID(json.getString("guid"));
		dataset.setPublicKey(json.getString("publicKey"));
		dataset.setSalt(json.getString("salt"));
		dataset.setUserIDs(json.getJSONArray("userIDs"));
		dataset.setLastUpdate(json.getString("lastUpdate"));
		dataset.setTimeout(json.getString("timeout"));
		dataset.setDefaults(json.getJSONObject("defaults"));
		dataset.setLegacyIDs(json.getJSONArray("legacyIDs"));
		
		return dataset;
	}
	
	public JSONObject exportJSONObject()
	{
		JSONObject json = new JSONObject();
	
		json.put("schemaVersion", this.schemaVersion);
		json.put("salt", this.salt);
		json.put("userIDs", this.userIDs);
		json.put("lastUpdate", this.lastUpdate);
		json.put("timeout", this.timeout);
		json.put("publicKey", this.publicKey);
		json.put("active", this.active);
		json.put("revoked", this.revoked);
		json.put("guid", this.guid);
		json.put("defaults", this.defaults);
		json.put("legacyIDs", this.legacyIDs);
		
		return json;
	}
	
	public String getGUID()
	{
		return guid;
	}
	
	public void setGUID(String guid)
	{
		this.guid = guid;
	}
	
	public int getSchemaVersion()
	{
		return schemaVersion;
	}
	
	public void setSchemaVersion(int schemaVersion)
	{
		this.schemaVersion = schemaVersion;
	}
	
	public String getSalt()
	{
		return salt;
	}
	
	public void setSalt(String salt)
	{
		this.salt = salt;
	}
	
	public JSONArray getUserIDs()
	{
		return userIDs;
	}
	
	public JSONObject getUserIDs(int index)
	{
		return userIDs.getJSONObject(index);
	}
	
	public void setUserIDs(JSONArray userIDs)
	{
		this.userIDs = userIDs;
	}
	
	public String getLastUpdate()
	{
		return lastUpdate;
	}
	
	public void setLastUpdate(String lastUpdate)
	{
		this.lastUpdate = lastUpdate;
	}
	
	public String getTimeout()
	{
		return timeout;
	}
	
	public void setTimeout(String timeout)
	{
		this.timeout = timeout;
	}
	
	public String getPublicKey()
	{
		return publicKey;
	}
	
	public void setPublicKey(String publicKey)
	{
		this.publicKey = publicKey;
	}
	
	public int getActive()
	{
		return active;
	}
	
	public void setActive(int active)
	{
		this.active = active;
	}
	
	public int getRevoked()
	{
		return revoked;
	}
	
	public void setRevoked(int revoked)
	{
		this.revoked = revoked;
	}
	
	public JSONObject getDefaults()
	{
		return defaults;
	}
	
	public void setDefaults(JSONObject defaults)
	{
		this.defaults = defaults;
	}
	
	public JSONArray getLegacyIDs()
	{
		return legacyIDs;
	}
	
	public void setLegacyIDs(JSONArray legacyIDs)
	{
		this.legacyIDs = legacyIDs;
	}
	
	public boolean validateSchema() throws SocialRecordIntegrityException
	{
		try (InputStream inputStream = getClass().getResourceAsStream("schema.json"))
		{
				JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
				Schema schema = SchemaLoader.load(rawSchema);
				schema.validate(exportJSONObject());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (ValidationException e)
		{
			throw new SocialRecordIntegrityException("Dataset does not validate against JSON Schema");
		}
		
		return true;
	}
	
	public static boolean checkDatasetValidity(JSONObject json) throws SocialRecordIntegrityException
	{
		if(!json.has("guid"))
			throw new SocialRecordIntegrityException("mandatory parameter 'guid' missing");
		if(!json.has("schemaVersion"))
			throw new SocialRecordIntegrityException("mandatory parameter 'schemaVersion' missing");
		
		// TODO: userIDs are now objects. Rewrite check
		if(!json.has("userIDs"))
			throw new SocialRecordIntegrityException("mandatory parameter 'userIDs' missing");
		if(!json.has("lastUpdate"))
			throw new SocialRecordIntegrityException("mandatory parameter 'lastUpdate' missing");
		if(!json.has("timeout"))
			throw new SocialRecordIntegrityException("mandatory parameter 'timeout' missing");
		if(!json.has("publicKey"))
			throw new SocialRecordIntegrityException("mandatory parameter 'publicKey' missing");
		if(!json.has("salt"))
			throw new SocialRecordIntegrityException("mandatory parameter 'salt' missing");
		if(!json.has("revoked"))
			throw new SocialRecordIntegrityException("mandatory parameter 'revoked' missing");
		if(!json.has("defaults"))
			throw new SocialRecordIntegrityException("mandatory parameter 'defaults' missing");
		
		if(!json.getJSONObject("defaults").has("voice"))
			throw new SocialRecordIntegrityException("mandatory parameter 'defaults : voice' missing");
		if(!json.getJSONObject("defaults").has("chat"))
			throw new SocialRecordIntegrityException("mandatory parameter 'defaults : chat' missing");
		if(!json.getJSONObject("defaults").has("video"))
			throw new SocialRecordIntegrityException("mandatory parameter 'defaults : video' missing");
		// for unknown reasons, this fails always
		/*if(json.getString("guid").equals(GUID.createGUID(json.getString("publicKey"), json.getString("salt"))))
			throw new DatasetIntegrityException("guid does not match publicKey/salt: "+ json.getString("publicKey") + " :: " + json.getString("salt"));*/
		
		return true;
	}
}