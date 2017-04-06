package net.sonic.gsls;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import net.sonic.gsls.config.Config;
import net.sonic.gsls.controller.DHTManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

/**
 * Main class for GSLS daemon
 * 
 * @date 17.01.2017
 * @version 1
 * @author Sebastian Göndör
 */
@SpringBootApplication
@EnableScheduling
public class GSLSServer implements Daemon
{
	private static Logger LOGGER;
	
	public static void main(String[] args)
	{
		System.out.println("Initializing GSLS");
		
		Config config = Config.getInstance();
		
		Options options = new Options();
		
		Option helpOption = Option.builder("h")
				.longOpt("help")
				.desc("displays help on cli parameters")
				.build();
		
		Option portRESTOption = Option.builder("p")
				.longOpt("port_rest")
				.desc("sets the port for the REST interface [" + config.getPortREST() + "]")
				.hasArg()
				.build();
		
		Option networkInterfaceOption = Option.builder("n")
				.longOpt("network_interface")
				.desc("sets the network interface [" + config.getNetworkInterface() + "]")
				.hasArg()
				.build();
		
		Option logPathOption = Option.builder("l")
				.longOpt("log_path")
				.desc("sets the directory for the log files [" + config.getLogPath() + "]")
				.hasArg()
				.build();
		
		Option connectNodeOption = Option.builder("c")
				.longOpt("connect_node")
				.desc("sets the GReg node to connect to [" + config.getConnectNode() + "]")
				.hasArg()
				.build();
		
		options.addOption(helpOption);
		options.addOption(portRESTOption);
		options.addOption(networkInterfaceOption);
		options.addOption(logPathOption);
		options.addOption(connectNodeOption);
		
		// parse common line parameters
		CommandLineParser parser = new DefaultParser();
		
		try
		{
			CommandLine cmd = parser.parse(options, args);
			if(cmd.hasOption("h"))
			{
				HelpFormatter formater = new HelpFormatter();
				formater.printHelp("GReg help", options);
				System.exit(0);
			}
			
			if(cmd.hasOption("p"))
			{
				config.setPortREST(Integer.parseInt(cmd.getOptionValue("p"))); // TODO check for valid values
			}
			if(cmd.hasOption("n"))
			{
				config.setNetworkInterface(cmd.getOptionValue("n")); // TODO check for valid values
			}
			if(cmd.hasOption("l"))
			{
				config.setLogPath(cmd.getOptionValue("l")); // TODO check for valid values
			}
			if(cmd.hasOption("c"))
			{
				config.setConnectNode(cmd.getOptionValue("c")); // TODO check for valid values
			}
			
			System.out.println("-----Configuration: ");
			System.out.println("connectNode: " + config.getConnectNode());
			System.out.println("portREST: " + config.getPortREST());
			System.out.println("networkInterface: " + config.getNetworkInterface());
			System.out.println("logPath: " + config.getLogPath() + "\n-----");
			
			// setup logging
			System.setProperty("loginfofile", config.getLogPath() + "log-info.log");
			System.setProperty("logdebugfile", config.getLogPath() + "log-debug.log");
			
			LOGGER = LoggerFactory.getLogger(GSLSServer.class);
			
			LOGGER.info(config.getProductName() + " " + config.getVersionName() + " " + config.getVersionCode());
			LOGGER.info("Build #" + config.getVersionNumber() + " (" + config.getVersionDate() + ")\n");
		}
		catch (ParseException e)
		{
			System.out.println("Wrong parameter. Error: " + e.getMessage());
		}
		
		try
		{
			LOGGER.info("initializing DHT... ");
			
			DHTManager.getInstance().initDHT();
			LOGGER.info("DHT initialized successfully");
			
			LOGGER.info("initializing GSLS server... ");
			
			// Registering the port for the REST interface to listen on 
			System.getProperties().put("server.port", config.getPortREST());
			LOGGER.info("REST interface listening on  " + config.getPortREST());
			
			SpringApplication.run(GSLSServer.class, args);
		}
		catch (Exception e)
		{
			LOGGER.info("failed! Service terminated!");
			e.printStackTrace();
		}
	}
	
	// 5 minute delay, then every 12 hours
	//@Scheduled(initialDelay=5 * 60 * 1000, fixedRate=12 * 60 * 60 * 1000)
	@Scheduled(initialDelay=2 * 60 * 1000, fixedRate=1 * 60 * 60 * 1000)
	protected void reconnect()
	{
		LOGGER.info("running reconnect functionality...");
		
		try
		{
			DHTManager.getInstance().connectToConnectNode();
		}
		catch (Exception e)
		{
			LOGGER.info("reconnect functionality failed!");
			e.printStackTrace();
		}
	}
	
	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception
	{
		//System.out.println("deamon: init()");
		String arguments[] = daemonContext.getArguments();
		System.out.println(arguments);
		GSLSServer.main(arguments);
	}
	
	@Override
	public void start() throws Exception
	{
		//System.out.println("deamon: start()");
	}
	
	@Override
	public void stop() throws Exception
	{
		//System.out.println("deamon: exception()");
	}
	
	@Override
	public void destroy()
	{
		//System.out.println("deamon: destroy()");
	}
}