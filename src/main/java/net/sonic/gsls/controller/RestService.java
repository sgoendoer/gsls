package net.sonic.gsls.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.impl.Base64UrlCodec;
import net.sonic.gsls.config.Config;
import net.sonic.gsls.model.SocialRecord;
import net.sonic.gsls.model.SocialRecordIntegrityException;
import net.sonic.gsls.util.IntegrityException;
import net.sonic.gsls.util.KeyPairManager;
import net.tomp2p.peers.PeerAddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * Main class for GSLS REST interface
 * 
 * @date 18.01.2017
 * @version 1
 * @author Sebastian Göndör
 */
@RestController
@RequestMapping("/")
public class RestService
{
	@Autowired
	private static final Logger LOGGER = LoggerFactory.getLogger(RestService.class);
	
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> index() throws URISyntaxException
	{
		LOGGER.error("Incoming request: GET /");
		List<PeerAddress> AllNeighbors = DHTManager.getInstance().getAllNeighbors();
		
		JSONArray connectedNodes = new JSONArray();
		
		for (PeerAddress neighbor : AllNeighbors) {
			connectedNodes.put(neighbor.inetAddress().getHostAddress());
		}
		
		JSONObject version = new JSONObject();
		version.put("version", Config.getInstance().getVersionName());
		version.put("build", Config.getInstance().getVersionNumber());
		
		JSONObject response = new JSONObject();
		response.put("status", 200);
		response.put("version", version);
		response.put("connectedNodes", connectedNodes);
		
		return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
		
	}
	
	//STATUS: Index Function is working absolutely fine.
	
	/**
	 * retrieve a SocialRecord
	 * @param globalID
	 * @return ResponseEntity
	 */
	@RequestMapping(value = "/{globalID}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<String> getEntityByGlobalID(@PathVariable("globalID") String globalID)
	{
		SocialRecord socialRecord;
		
		LOGGER.error("Incoming request: GET /" + globalID);
		
		if(globalID == null)
		{
			// received get request for path /guid/, but no guid.
			JSONObject response = new JSONObject();
			
			response.put("status", 400);
			response.put("message", "GlobalID not specified in request URL");
			
			return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
		}
		else //if(globalID != null)
		{
			String jwt = null;
			
			try
			{
				// get JWT from DHT
				try
				{
					jwt = DHTManager.getInstance().get(globalID);
				}
				catch (GIDNotFoundException e)
				{
					// tried to get SocialRecord from dht, caught an exception
					JSONObject response = new JSONObject();
					
					response.put("status", 404);
					response.put("message", "GlobalID not found");
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.NOT_FOUND);
				}
				
				if(jwt == null)
				{
					// tried to get dataset from dht, found null. this should NEVER happen!
					JSONObject response = new JSONObject();
					
					response.put("status", 404);
					response.put("message", "GlobalID not found. DHT returned NULL.");
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.NOT_FOUND);
				}
				else //if(jwt != null)
				{
					// decode JWT
					JSONObject jwtPayload = new JSONObject(new String(Base64UrlCodec.BASE64URL.decodeToString(jwt.split("\\.")[1])));
					JSONObject data = new JSONObject(Base64UrlCodec.BASE64URL.decodeToString(jwtPayload.get("data").toString()));
					
					LOGGER.info("decoded JWT payload: " + data.toString());
					
					// verify dataset integrity
					try
					{
						//SocialRecord.checkSocialRecordValidity(data);
						socialRecord = SocialRecord.createFromJSONObject(data);
						socialRecord.validateSchema();
					}
					catch (SocialRecordIntegrityException e)
					{
						// read jwt from dht, integrity check for the enclosed json failed
						LOGGER.error("Integrity Exception found for JWT: " + jwt + " e: " + e.getMessage());
						
						JSONObject response = new JSONObject();
						
						response.put("status", 500);
						response.put("message", "Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
						
						return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
					}
					
					// decode key
					PublicKey publicKey;
					
					try
					{
						publicKey = KeyPairManager.decodePublicKey(data.getString("personalPublicKey"));
					}
					catch (InvalidKeySpecException | NoSuchAlgorithmException e)
					{
						// got jwt from dht, tried to extract public key, failed while doing so
						LOGGER.error("Malformed public key found in SocialRecord: " + jwt + " e: " + e.getMessage());
						
						JSONObject response = new JSONObject();
						
						response.put("status", 500);
						response.put("message", "Malformed personal public key found in DHT: " + jwt + " e: " + e.getMessage());
						
						return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
					}
					
					// verify jwt
					try
					{
						Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt);
					}
					catch (MalformedJwtException | UnsupportedJwtException e)
					{
						// got jwt from dht, jwt seems to be malformed
						LOGGER.error("Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
						
						JSONObject response = new JSONObject();
						
						response.put("status", 500);
						response.put("message", "Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
						
						return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
					}
					catch (SignatureException e)
					{
						// got jwt from dht, jwt signature check failed
						LOGGER.error("Malformed JWT found in DHT: " + jwt + e.getMessage());
						
						JSONObject response = new JSONObject();
						
						response.put("status", 500);
						response.put("message", "Malformed signature for JWT found in DHT: " + jwt + " e: " + e.getMessage());
						
						return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
					}
					
					LOGGER.info("JWT for GlobalID " + globalID + " verified");
					
					JSONObject response = new JSONObject();
					
					response.put("status", 200);
					response.put("message", jwt);
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
				}
			}
			catch(JSONException e)
			{
				// somewhere, a json exception was thrown
				LOGGER.error("Faulty JSON data in DHT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 500);
				response.put("message", "Faulty JSON data in DHT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			catch (IOException | ClassNotFoundException e)
			{
				// somewhere, a more severe exception was thrown
				LOGGER.error("Internal Server Error: " + jwt + " e: "+ e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 500);
				response.put("message", "Internal Server Error: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	/**
	 * upload a new SocialRecord
	 * @param globalID
	 * @return ResponseEntity
	 */
	@RequestMapping(value = "/{globalID}", method = RequestMethod.POST)
	public ResponseEntity<String> postDdata(@RequestBody String jwt, @PathVariable("globalID") String globalID)
	{
		LOGGER.error("Incoming request: POST /" + globalID + " - JWT: " + jwt);
		
		SocialRecord socialRecord;
		JSONObject data; // the new jwt
		PublicKey publicKey; // the public key of the NEW version
		
		try
		{
			// decode JWT
			JSONObject jwtPayload = new JSONObject(new String(Base64UrlCodec.BASE64URL.decodeToString(jwt.split("\\.")[1])));
			data = new JSONObject(Base64UrlCodec.BASE64URL.decodeToString(jwtPayload.get("data").toString()));
			
			//LOGGER.info("decoded JWT payload: " + data.toString());
			
			// verify SocialRecord integrity
			try
			{
				//SocialRecord.checkSocialRecordValidity(data);
				socialRecord = SocialRecord.createFromJSONObject(data);
				socialRecord.validateSchema();
			}
			catch (SocialRecordIntegrityException e)
			{
				LOGGER.error("Integrity Exception found for received JWT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "JWT is malformed: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			
			// decode key
			try
			{
				publicKey = KeyPairManager.decodePublicKey(data.getString("publicKey"));
			}
			catch (InvalidKeySpecException | NoSuchAlgorithmException e)
			{
				LOGGER.error("Malformed public key found in JWT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "Malformed public key found in JWT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			
			// verify jwt
			try
			{
				Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwt);
			}
			catch (MalformedJwtException | UnsupportedJwtException e)
			{
				LOGGER.error("Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "Malformed JWT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			catch (SignatureException e)
			{
				LOGGER.error("Malformed JWT found in DHT: " + jwt + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "Malformed signature for JWT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			
			LOGGER.info("JWT for GlobalID " + globalID + " verified");
			
			try
			{
				// everything is fine. write SocialRecord to DHT
				DHTManager.getInstance().put(globalID, jwt);
				
				LOGGER.info("SocialRecord for GlobalID " + globalID + " uploaded: \n" + jwt);
			}
			catch (IOException e)
			{
				// tried to write SocialRecord, found an existing one. Encountered an IO error while writing
				JSONObject response = new JSONObject();
				
				response.put("status", 500);
				response.put("message", "Error while writing to DHT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
			LOGGER.info("SocialRecord for [" + globalID + "] written to DHT: \n" + jwt);
			
			JSONObject response = new JSONObject();
			
			response.put("status", 200);
			response.put("message", "SocialRecord for GlobalID " + globalID + " updated: " + jwt);
			
			return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
		}
		catch(JSONException e)
		{
			// somewhere, a json exception was thrown
			LOGGER.error("Faulty JSON data: " + jwt + " e: " + e.getMessage());
			
			JSONObject response = new JSONObject();
			
			response.put("status", 500);
			response.put("message", "Faulty JSON data: " + jwt + " e: " + e.getMessage());
			
			return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * edit an existing SocialRecord by overwriting with a new version
	 * 
	 * @param jwt
	 * @param globalID
	 * @return ResonseEntity
	 */
	@RequestMapping(value = "/{globalID}", method = RequestMethod.PUT)
	public ResponseEntity<String> putdata(@RequestBody String jwt, @PathVariable("globalID") String globalID)
	{
		LOGGER.error("Incoming request: PUT /" + globalID + " - JWT: " + jwt);
		
		SocialRecord newSocialRecord;
		SocialRecord existingSocialRecord;
		String existingJWT = null;
		JSONObject newData; // the new version of the jwt
		JSONObject existingData; // the already existing version (if there is any)
		
		PublicKey newPersonalPublicKey; // the personal public key of the NEW version
		PublicKey existingPersonalPublicKey;
		
		try
		{
			// decode JWT
			JSONObject jwtPayload = new JSONObject(new String(Base64UrlCodec.BASE64URL.decodeToString(jwt.split("\\.")[1])));
			newData = new JSONObject(Base64UrlCodec.BASE64URL.decodeToString(jwtPayload.get("data").toString()));
			
			//LOGGER.info("decoded JWT payload: " + newData.toString());
			
			// verify SocialRecord integrity
			try
			{
				//SocialRecord.checkDatasetValidity(newData);
				newSocialRecord = SocialRecord.createFromJSONObject(newData);
				newSocialRecord.validateSchema();
			}
			catch (SocialRecordIntegrityException e)
			{
				LOGGER.error("Integrity Exception found for received JWT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "JWT is malformed: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			
			// decode key
			try
			{
				newPersonalPublicKey = KeyPairManager.decodePublicKey(newData.getString("personalPublicKey"));
			}
			catch (InvalidKeySpecException | NoSuchAlgorithmException e)
			{
				LOGGER.error("Malformed public key found in JWT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "Malformed public key found in JWT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			
			// verify jwt
			try
			{
				Jwts.parser().setSigningKey(newPersonalPublicKey).parseClaimsJws(jwt);
			}
			catch (MalformedJwtException | UnsupportedJwtException e)
			{
				LOGGER.error("Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "Malformed JWT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			catch (SignatureException e)
			{
				LOGGER.error("Malformed JWT found in DHT: " + jwt + e.getMessage());
				
				JSONObject response = new JSONObject();
				
				response.put("status", 400);
				response.put("message", "Malformed signature for JWT: " + jwt + " e: " + e.getMessage());
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
			}
			
			LOGGER.info("JWT for GlobalID " + globalID + " verified");
			
			// match new JWT to existing JWT
			try
			{
				existingJWT = DHTManager.getInstance().get(globalID);
				
				// GUID found. Ergo, we are updating an existing SocialRecord
				
				JSONObject jwtPayloadFromDHT = new JSONObject(new String(Base64UrlCodec.BASE64URL.decodeToString(existingJWT.split("\\.")[1])));
				existingData = new JSONObject(Base64UrlCodec.BASE64URL.decodeToString(jwtPayloadFromDHT.get("data").toString()));
				
				// verify the existing dataset's integrity
				try
				{
					//SocialRecord.checkDatasetValidity(existingData);
					existingSocialRecord = SocialRecord.createFromJSONObject(existingData);
					existingSocialRecord.validateSchema();
				}
				catch (SocialRecordIntegrityException e)
				{
					// tried to write socialrecord. found an existing one. the existing one failed the integrity test
					LOGGER.error("Integrity exception found for existing SocialRecord: " + existingJWT + " e: " + e.getMessage());
					
					JSONObject response = new JSONObject();
					
					response.put("status", 500);
					response.put("message", "Malformed JWT found in DHT: " + existingJWT + " e: " + e.getMessage());
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
				// decode key
				try
				{
					existingPersonalPublicKey = KeyPairManager.decodePublicKey(existingData.getString("personalPublicKey"));
				}
				catch (InvalidKeySpecException | NoSuchAlgorithmException e)
				{
					// tried to write SocialRecord. found an existing one. the public key of the existing one couldnt be extracted
					LOGGER.error("Malformed personal public key found in DHT: " + jwt + " e: " + e.getMessage());
					
					JSONObject response = new JSONObject();
					
					response.put("status", 500);
					response.put("message", "Malformed personal public key found in DHT: " + jwt + " e: " + e.getMessage());
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
				// verify jwt
				try
				{
					Jwts.parser().setSigningKey(existingPersonalPublicKey).parseClaimsJws(jwt);
				}
				catch (MalformedJwtException | UnsupportedJwtException e)
				{
					// tried to write SocialRecord. found an existing one. the existing one seems to be malformed jwt
					LOGGER.error("Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
					
					JSONObject response = new JSONObject();
					
					response.put("status", 500);
					response.put("message", "Malformed JWT found in DHT: " + jwt + " e: " + e.getMessage());
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				catch (SignatureException e)
				{
					// tried to write SocialRecord. found an existing one. the signature check of the existing one failed
					LOGGER.error("Malformed JWT found in DHT: " + jwt + e.getMessage());
					
					JSONObject response = new JSONObject();
					
					response.put("status", 500);
					response.put("message", "Malformed signature for JWT found in DHT: " + jwt + " e: " + e.getMessage());
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
				try
				{
					// verify that GlobalIDs are matching
					if(!newData.getString("globalID").equals(existingData.getString("globalID")))
						throw new IntegrityException("GlobalIDs are not matching!");
				}
				catch (IntegrityException e)
				{
					// tried to write SocialRecord, found an existing one. GlobalIDs do not match. Should NEVER happen!
					JSONObject response = new JSONObject();
					
					response.put("status", 400);
					response.put("message", "GUIDs do not match: " + jwt + " e: " + e.getMessage());
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.BAD_REQUEST);
				}
				
				// everything is fine. overwrite existing dataset with new one
				try
				{
					DHTManager.getInstance().put(globalID, jwt);
					
					LOGGER.info("Dataset for GlobalID " + globalID + " updated: \n" + jwt);
				}
				catch (IOException e)
				{
					// tried to write SocialRecord, found an existing one. Encountered an IO error while overwriting the existing one
					JSONObject response = new JSONObject();
					
					response.put("status", 500);
					response.put("message", "Error while writing to DHT: " + jwt + " e: " + e.getMessage());
					
					return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				
				LOGGER.info("SocialRecord for [" + globalID + "] written to DHT: \n" + jwt);
				
				JSONObject response = new JSONObject();
				
				response.put("status", 200);
				response.put("message", "SocialRecord for GlobalID " + globalID + " updated: " + jwt);
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
			}
			catch (GIDNotFoundException e)
			{
				// GlobalID not found. Aborting
				
				// tried to write SocialRecord, found an existing one. Encountered an IO error while overwriting the existing one
				JSONObject response = new JSONObject();
				
				response.put("status", 404);
				response.put("message", "SocialRecord for GlobalID not found: " + globalID);
				
				return new ResponseEntity<String>(response.toString(), HttpStatus.NOT_FOUND);
			}
		}
		catch(JSONException e)
		{
			// somewhere, a json exception was thrown
			LOGGER.error("Faulty JSON data in DHT: " + jwt + " e: " + e.getMessage());
			
			JSONObject response = new JSONObject();
			
			response.put("status", 500);
			response.put("message", "Faulty JSON data in DHT: " + jwt + " e: " + e.getMessage());
			
			return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch (IOException | ClassNotFoundException e)
		{
			// somewhere, a more severe exception was thrown
			LOGGER.error("Internal Server Error: " + jwt + " e: "+ e.getMessage());
			
			JSONObject response = new JSONObject();
			
			response.put("status", 500);
			response.put("message", "Internal Server Error: " + jwt + " e: " + e.getMessage());
			
			return new ResponseEntity<String>(response.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}