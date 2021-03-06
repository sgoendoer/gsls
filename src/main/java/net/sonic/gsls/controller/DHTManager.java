package net.sonic.gsls.controller;

import net.sonic.gsls.config.Config;
import net.tomp2p.connection.Bindings;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.replication.IndirectReplication;
import net.tomp2p.storage.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;

/**
 * DHT Manager class for accessing the DHT
 * 
 * @date 18.01.2017
 * @version 1
 * @author Sebastian Göndör
 */
@Repository
public class DHTManager
{
	@Autowired
	private static DHTManager	instance	= null;
	
	private PeerDHT peer;
	
	private DHTManager()
	{
	
	}
	
	public static DHTManager getInstance()
	{
		if(instance == null)
			instance = new DHTManager();
		return instance;
	}
	
	public DHTManager initDHT() throws IOException
	{
		Random rand = new Random();
		Bindings bind = new Bindings();
		bind.addInterface(Config.getInstance().getNetworkInterface());
		peer = new PeerBuilderDHT(new PeerBuilder(new Number160(rand)).ports(Config.getInstance().getPortDHT()).start()).start();
		
		this.connectToConnectNode();
		
		/*new IndirectReplication(peer).start();
		
		InetAddress address = Inet4Address.getByName(Config.getInstance().getConnectNode());
		FutureDiscover futureDiscover = peer.peer().discover().inetAddress(address).ports(Config.getInstance().getPortDHT()).start();
		futureDiscover.awaitUninterruptibly();
		FutureBootstrap futureBootstrap = peer.peer().bootstrap().inetAddress(address).ports(Config.getInstance().getPortDHT()).start();
		futureBootstrap.awaitUninterruptibly();*/
		
		return this;
	}
	
	public DHTManager connectToConnectNode() throws IOException
	{
		new IndirectReplication(peer).start();
		
		InetAddress address = Inet4Address.getByName(Config.getInstance().getConnectNode());
		FutureDiscover futureDiscover = peer.peer().discover().inetAddress(address).ports(Config.getInstance().getPortDHT()).start();
		futureDiscover.awaitUninterruptibly();
		FutureBootstrap futureBootstrap = peer.peer().bootstrap().inetAddress(address).ports(Config.getInstance().getPortDHT()).start();
		futureBootstrap.awaitUninterruptibly();
		
		return this;
	}
	
	/**
	 * Retrieves the social record from the DHT.
	 *
	 * @param key
	 * @return the social record
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * @throws GIDNotFoundException 
	 */
	public String get(String key) throws GIDNotFoundException
	{
		try
		{
			FutureGet futureGet = peer.get(Number160.createHash(key)).start();
			futureGet.awaitUninterruptibly();
			
			// TODO: use non-blocking?
			if(futureGet.isSuccess() && futureGet.data() != null)
			{
				return futureGet.data().object().toString();
			}
			else
			{
				throw new GIDNotFoundException("GID not found");
			}
			
			//return null; // TODO: decide on sentinel value
		}
		catch(ClassNotFoundException | IOException e)
		{
			throw new GIDNotFoundException("GID not found");
		}
	}
	
	/**
	 * Stores the social record in the DHT.
	 *
	 * @param key
	 *            : the GID
	 * @param value
	 *            : the social record
	 * @throws IOException
	 */
	public void put(String key, String value) throws IOException
	{
		FuturePut futurePut = peer.put(Number160.createHash(key)).data(new Data(value)).start();
		futurePut.awaitUninterruptibly();
		
		if(futurePut.isSuccess())
		{
			
		}
		// TODO: use non-blocking?
	}
	
	/**
	 * removes a key from the DHT. Should ONLY be used for the tests
	 *
	 * @param key
	 * @throws IOException
	 */
	public void delete(String key) throws IOException
	{
		peer.remove(Number160.createHash(key)).start();
	}
	
	/**
	 * retrieves a list of all connected peers
	 * 
	 * @return List<PeerAddress>
	 */
	public List<PeerAddress> getAllNeighbors()
	{
		return peer.peerBean().peerMap().all();
	}
}