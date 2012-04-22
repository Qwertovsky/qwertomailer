package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author Qwertovsky
 *
 */
class Mailer
{
	public static Logger logger = Logger.getLogger("com.qwertovsky.mailer");
	
	
	//--------------------------------------------
	public static void main(String[] args)
	{
		//logger configuration
		String pattern = "[%d{yyyy-MM-dd HH:mm:ss} %-4r][%-5p] %m%n";
	    PatternLayout layout = new PatternLayout(pattern);
	    //file will be rolled over every day
	    DailyRollingFileAppender appender=null;
	    try
		{
			File logDir = new File("log");
			if(!logDir.exists())
				logDir.mkdir();
	    	appender = new DailyRollingFileAppender(layout, "log/qwertomailer.log", "'.'yyyy-MM-dd'.log'");
		} catch (IOException e)
		{
			logger.error(e.getMessage());
			return;
		}
	    logger.addAppender(appender);
	    logger.setLevel(Level.INFO);
		
		logger.info("---------------------------------------");
		logger.info("Program started");
		InputStream manifestStream = Thread.currentThread().getContextClassLoader()
			.getResourceAsStream("META-INF/MANIFEST.MF");
		try
		{
			Manifest manifest = new Manifest(manifestStream);
			Attributes attributes = manifest.getMainAttributes();
			String impVersion = attributes.getValue("Implementation-Version");
			String builtDate = attributes.getValue("Built-Date");
			logger.info("Built date: " + builtDate +", Version: "+impVersion);
		} catch (IOException ex)
		{
			logger.warn("Error while reading version: " + ex.getMessage());
		}
		//write options
		String optionsLine = "";
		for(String arg:args)
		{
			if(arg.contains(" "))
				arg = "\"" + arg +"\"";
			optionsLine = optionsLine + " " + arg;
		}
		logger.info("Command line options: " + optionsLine);
		
	    //options
		Options options = createOptions();
		
		//parse parameters
		CommandLineParser parser = new PosixParser();

		String smtpHost = null;
		String smtpPort = "25";
		String smtpUser = null;
		String smtpPassword = null;
		String hostname = null;
		String charset = "utf-8";
		String contentTransferEncoding = "8bit";
		String text = null;
		String contentType = "text/plain";
		File emlFile = null;
		String subject = null;
		String emailFrom = null;
		String personFrom = null;
		ArrayList<Address> emailsTo = new ArrayList<Address>();
		ArrayList<String[]> personParameters = new ArrayList<String[]>();
		String[] personParamHeaders = null;
		
		CommandLine commandLine = null;
		try
		{
			commandLine = parser.parse(options, args, true);
		}catch(ParseException pe)
		{
			logger.error(pe.getMessage());
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java -jar qwertomailer.jar", options, true);
			System.exit(1);
		}
		
		if(commandLine.hasOption("trace"))
		{
			logger.setLevel(Level.TRACE);
		}
		smtpHost = commandLine.getOptionValue("smtpHost");
		smtpPort = commandLine.getOptionValue("smtpPort", "25");
		smtpUser = commandLine.getOptionValue("smtpUser");
		smtpPassword = commandLine.getOptionValue("smtpPassword");
		hostname = commandLine.getOptionValue("hostname");
		charset = commandLine.getOptionValue("charset", "UTF-8");
		contentTransferEncoding = commandLine.getOptionValue("mimeTransport", "8bit");
		
		if(commandLine.hasOption("body"))
			text = commandLine.getOptionValue("body");
		else if (commandLine.hasOption("bodyFile"))
		{
			String file = commandLine.getOptionValue("bodyFile");
			text = getMessageTextFromFile(file, charset);
		} 
		else if (commandLine.hasOption("bodyEML"))
		{
			emlFile = new File(commandLine.getOptionValue("bodyEML"));
			if(!emlFile.exists())
			{
				logger.error("EML file not exists");
				System.err.println("EML file not exists");
				System.exit(1);
			}
		}
		
		contentType = commandLine.getOptionValue("contentType", "text/plain");
					
		if(commandLine.hasOption("subject"))
			subject = commandLine.getOptionValue("subject");
		else if(commandLine.hasOption("subjectFile"))
		{
			String file = commandLine.getOptionValue("subjectFile");
			subject = getSubjectFromFile(file, charset);
		}
		
		emailFrom = commandLine.getOptionValue("emailFrom");
		personFrom = commandLine.getOptionValue("personFrom");
		
		if(commandLine.hasOption("emailTo"))
		{
			String[] emails = commandLine.getOptionValues("emailTo");
			for(String email:emails)
			{
				try
				{
					emailsTo.add(new InternetAddress(email));
				}catch(AddressException ae)
				{
					logger.warn(email + ":" + ae.getMessage());
				}
			}
		}
		else if(commandLine.hasOption("emailToFile"))
		{
			String file = commandLine.getOptionValue("emailToFile");
			personParamHeaders = getPersonParametersFromFile(personParameters, file, charset);
		}
		
		String alttext = null;
		boolean related = false;
		if(contentType.equalsIgnoreCase("text/html"))
		{
			//get alternative text
			if(commandLine.hasOption("alttext"))
			{
				alttext = commandLine.getOptionValue("alttext");
			}
			else if(commandLine.hasOption("alttextFile"))
			{
				String file = commandLine.getOptionValue("alttextFile");
				alttext = getAltTextFromFile(file, charset);
			}
		
			//is related
			if(commandLine.hasOption("related"))
			{
				related = true;
			}
		}
		
		//get attachments
		List<File> attachFiles = new ArrayList<File>();
		if(commandLine.hasOption("attach"))
		{
			String[] attachFilesPath = commandLine.getOptionValues("attach");
			
			for(String path:attachFilesPath)
			{
				attachFiles.add(new File(path));
			}
		}
		else if(commandLine.hasOption("attachFile"))
		{
			String file = commandLine.getOptionValue("attachFile");
			attachFiles = getAttachFilesFromFile(file);
		}
		
		//create sender
		Sender sender = null;
		try
		{
			sender = new Sender(smtpHost, smtpPort, smtpUser, smtpPassword, hostname);
		} catch (Exception e)
		{
			logger.error(e.getMessage());
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		//create message
		MessageContent message = null;
		try
		{
			if(emlFile != null)
				message = new MessageContent(emlFile);
			else
				message = new MessageContent(text,contentType, subject, charset);
			message.setContentTransferEncoding(contentTransferEncoding);
			message.setAddressFrom(personFrom, emailFrom, charset);
			if(alttext != null)
				message.setAlternativeText(alttext, charset);
			if(attachFiles != null && !attachFiles.isEmpty())
			{
				message.addAttachments(attachFiles);
			}
			if(related)
				message.setRelated();
		} catch (Exception e)
		{
			logger.error(e.getMessage());
			System.err.println(e.getMessage());
			logger.info("Program stoped");
			return;
		}
		
		//send message
		try
		{
			if(!emailsTo.isEmpty())
				sender.send(message, emailsTo);
			else
			{
				//send with parameters
				sender.send(message, personParamHeaders, personParameters);
			}
		}catch(Exception e)
		{
			String errorMessage = e.getMessage();
			if(errorMessage != null)
			{
				logger.error(errorMessage);
				System.err.println(errorMessage);
			}
			else
			{
				logger.error("Error",e);
				System.err.println("Error");
			}
		}
		logger.info("Program stoped");
	}
	
	

	//--------------------------------------------
	private static String getMessageTextFromFile(String file, String charset)
	{
		logger.info("Get message text from file: " + file);
		String text = null;
		File textFile = new File(file);
		try
		{
			Scanner scanner = new Scanner(textFile, charset);
            StringBuilder textBuilder = new StringBuilder();
            while(scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                textBuilder.append(line);
                textBuilder.append("\n");
            }
            text = textBuilder.toString();
		} catch (FileNotFoundException e)
		{
			System.err.println("Body file not exists: " + file);
			logger.error("Body file not exists: " + file);
			System.exit(1);
		}catch(IllegalArgumentException e)
		{
			System.err.println("Specified charset is not found: " + charset);
			logger.error("Specified charset is not found: " + charset);
			System.exit(1);
		}
            
		return text;
	}
	
	//--------------------------------------------
	private static String getAltTextFromFile(String file, String charset)
	{
		logger.info("Get alternative text from file: " + file);
		String text = null;
		File textFile = new File(file);
		try
		{
			Scanner scanner = new Scanner(textFile, charset);
            StringBuilder textBuilder = new StringBuilder();
            while(scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                textBuilder.append(line);
                textBuilder.append("\n");
            }
            text = textBuilder.toString();
		} catch (FileNotFoundException e)
		{
			System.err.println("Alttext file not exists: " + file);
			logger.error("Alttext file not exists: " + file);
			System.exit(1);
		}catch(IllegalArgumentException e)
		{
			System.err.println("Specified charset is not found: " + charset);
			logger.error("Specified charset is not found: " + charset);
			System.exit(1);
		}
            
		return text;
	}
	
	//--------------------------------------------
	private static String getSubjectFromFile(String file, String charset)
	{
		logger.info("Get subject from file: " + file);
		String subject = null;
		File subjectFile = new File(file);
		try
		{
			Scanner scanner = new Scanner(subjectFile, charset);
            if(scanner.hasNextLine())
            	subject = scanner.nextLine();
            else
            {
            	System.err.println("Subject file is empty: " + file);
            	logger.error("Subject file is empty: " + file);
				System.exit(1);
            }
		} catch (FileNotFoundException e)
		{
			System.err.println("Subject file not exists: " + file);
			logger.error("Subject file not exists: " + file);
			System.exit(1);
		}catch(IllegalArgumentException e)
		{
			System.err.println("Specified charset is not found: " + charset);
			logger.error("Specified charset is not found: " + charset);
			System.exit(1);
		}
		return subject;
	}
	
	//--------------------------------------------
	private static String[] getPersonParametersFromFile(List<String[]> personParameters
			, String file, String charset)
	{
		logger.info("Get person parameters from file: " + file);
		String[] headers = null;
		File emailsFile = new File(file);
		CSVReader reader = null;
		try
		{
			FileInputStream fis = new FileInputStream(emailsFile);
			InputStreamReader isr = new InputStreamReader(fis, charset);
			reader=new CSVReader(isr, ',','"',false);
			headers = reader.readNext();
			personParameters.addAll(reader.readAll());
			reader.close();
		}catch (FileNotFoundException fnfe)
		{
			System.err.println("File with emails not exists: " + file);
			logger.error("File with emails not exists: " + file);
			System.exit(1);
		}catch(UnsupportedEncodingException uee)
		{
			System.err.println("Specified charset is not found: " + charset);
			logger.error("Specified charset is not found: " + charset);
			System.exit(1);
		}
		catch (IOException ioe)
		{
			System.err.println("Error read file with emails: " + file + " (" + ioe.getMessage() + ")");
			logger.error("Error read file with emails: " + file + " (" + ioe.getMessage() + ")");
			System.exit(1);
		}
		
        return headers;
	}

	//--------------------------------------------
	private static List<File> getAttachFilesFromFile(String file)
	{
		logger.info("Get attach files from file: " + file);
		List<File> files = new ArrayList<File>();
		File attachFiles = new File(file);
		try
		{
			Scanner scanner = new Scanner(attachFiles);
            while(scanner.hasNextLine())
            {
                String attachFile = scanner.nextLine();
                files.add(new File(attachFile));
            }
		} catch (FileNotFoundException e)
		{
			System.err.println("File with attachments not exists: " + file);
			logger.error("File with attachments not exists: " + file);
			System.exit(1);
		}
            
		return files;
	}
	
	//--------------------------------------------
	@SuppressWarnings("static-access")
	private static Options createOptions()
	{
		Options options = new Options();
		Option oSmtpHost = OptionBuilder.withArgName("host")
				.withDescription("specify SMTP server")
				.hasArg()
				.isRequired()
				.create("smtpHost");
		Option oSmtpPort = OptionBuilder.withArgName("port")
				.withDescription("specify SMTP port (default 25)")
				.hasArg()
				.create("smtpPort");
		Option oSmtpUser = OptionBuilder.withArgName("username")
				.withDescription("specify SMTP user")
				.hasArg()
				.create("smtpUser");
		Option oSmtpPassword = OptionBuilder.withArgName("password")
				.withDescription("specify SMTP password")
				.hasArg()
				.create("smtpPassword");
		Option oHostName = OptionBuilder.withArgName("hostname")
				.withDescription("replace your local machine name")
				.hasArg()
				.create("hostname");
		Option oCharset = OptionBuilder.withArgName("charset")
				.withDescription("specify message encoding (default utf-8)")
				.hasArg()
				.create("charset");
		Option oContentTransferEncoding = OptionBuilder.withArgName("transport")
				.withDescription("specify MIME Transport (default 8bit)")
				.hasArg()
				.create("mimeTransport");
		
		Option oMessage = OptionBuilder.withArgName("message")
				.withDescription("message body")
				.hasArg()
				.create("body");
		Option oMessageFile = OptionBuilder.withArgName("file")
				.withDescription("message body file")
				.hasArg()
				.create("bodyFile");
		Option oEMLFile = OptionBuilder.withArgName("file")
				.withDescription("get content from EML file")
				.hasArg()
				.create("bodyEML");
		OptionGroup ogMessage = new OptionGroup();
		ogMessage.setRequired(true);
		ogMessage.addOption(oMessage);
		ogMessage.addOption(oMessageFile);
		ogMessage.addOption(oEMLFile);
		
		Option oContentType = OptionBuilder.withArgName("type")
				.withDescription("specify content type (default text/plain)")
				.hasArg()
				.create("contentType");
		
		Option oSubject = OptionBuilder.withArgName("subject")
				.withDescription("subject")
				.hasArg()
				.create("subject");
		Option oSubjectFile = OptionBuilder.withArgName("file")
				.withDescription("file with subject line")
				.hasArg()
				.create("subjectFile");
		OptionGroup ogSubject = new OptionGroup();
		ogSubject.setRequired(true);
		ogSubject.addOption(oSubject);
		ogSubject.addOption(oSubjectFile);
		
		Option oEmailTo = OptionBuilder.withArgName("recipients")
				.withDescription("specify recipients (comma separated)")
				.hasArgs()
				.withValueSeparator(',')
				.create("emailTo");
		Option oEmailToFile = OptionBuilder.withArgName("file")
				.withDescription("specify file with recipients list")
				.hasArg()
				.create("emailToFile");
		OptionGroup ogEmailTo = new OptionGroup();
		ogEmailTo.setRequired(true); 
		ogEmailTo.addOption(oEmailTo);
		ogEmailTo.addOption(oEmailToFile);
		
		Option oPersonFrom = OptionBuilder.withArgName("person")
				.withDescription("specify sender name")
				.hasArg()
				.create("personFrom");
		Option oEmailFrom = OptionBuilder.withArgName("email")
				.withDescription("specify sender email")
				.hasArg()
				.isRequired()
				.create("emailFrom");
		
		Option oAltText = OptionBuilder.withArgName("text")
				.withDescription("Add alternative plain text")
				.hasArg()
				.create("alttext");
		Option oAltTextFile = OptionBuilder.withArgName("file")
				.withDescription("Add alternative plain text from file")
				.hasArg()
				.create("alttextFile");
		OptionGroup ogAltText = new OptionGroup();
		ogAltText.addOption(oAltText);
		ogAltText.addOption(oAltTextFile);
		
		Option  oAttach = OptionBuilder.withArgName("files")
				.withDescription("attach files (comma separated)")
				.hasArgs()
				.withValueSeparator(',')
				.create("attach");
		Option  oAttachFile = OptionBuilder.withArgName("file")
				.withDescription("file with list of attach file")
				.hasArgs()
				.create("attachFile");
		OptionGroup ogAttach = new OptionGroup();
		ogAttach.addOption(oAttach);
		ogAttach.addOption(oAttachFile);
		
		Option oRelated = new Option("related","Create message with inline images");
		
		Option oTrace = OptionBuilder
				.withDescription("Set trace log level. Send messages will be saved on disk")
				.create("trace");
				
		options.addOption(oSmtpHost);
		options.addOption(oSmtpPort);
		options.addOption(oSmtpUser);
		options.addOption(oSmtpPassword);
		options.addOption(oHostName);
		options.addOption(oCharset);
		options.addOption(oContentTransferEncoding);
		options.addOptionGroup(ogMessage);
		options.addOption(oContentType);
		options.addOptionGroup(ogSubject);
		options.addOption(oEmailFrom);
		options.addOptionGroup(ogEmailTo);
		options.addOption(oPersonFrom);
		options.addOptionGroup(ogAttach);
		options.addOptionGroup(ogAltText);
		options.addOption(oRelated);
		options.addOption(oTrace);
		return options;
	}

}
