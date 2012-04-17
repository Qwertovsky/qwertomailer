package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

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

public class Mailer
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
			appender = new DailyRollingFileAppender(layout, "qwertomailer.log", "'.'yyyy-MM-dd'.log'");
		} catch (IOException e)
		{
			logger.error(e.getMessage());
			return;
		}
	    logger.addAppender(appender);
	    logger.setLevel(Level.INFO);
		
		logger.info("---------------------------------------");
		logger.info("Program started");
		
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
		String recipientType = "PERSON";
		
		CommandLine commandLine = null;
		try
		{
			commandLine = parser.parse(options, args, true);
		}catch(ParseException pe)
		{
			logger.error(pe.getMessage());
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("java -jar mailer.jar", options, true);
			System.exit(1);
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
					logger.warn(ae.getMessage());
				}
			}
		}
		else if(commandLine.hasOption("emailToFile"))
		{
			String file = commandLine.getOptionValue("emailToFile");
			emailsTo = getEmailsFromFile(file);
		}
		
		recipientType = commandLine.getOptionValue("sendMethod","PERSON");
			
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
		sender.setRecipientType(recipientType);
		
		//create message
		MailMessage message = null;
		try
		{
			if(emlFile != null)
				message = new MailMessage(emlFile);
			else
				message = new MailMessage(text,contentType, subject, charset);
			message.setContentTransferEncoding(contentTransferEncoding);
			message.setAddressFrom(personFrom, emailFrom, charset);
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
			sender.send(message, emailsTo);
		}catch(Exception e)
		{
			logger.error(e.getMessage());
			System.err.println(e.getMessage());
		}
		logger.info("Program stoped");
	}
	
	

	//--------------------------------------------
	private static String getMessageTextFromFile(String file, String charset)
	{
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
			System.err.println("Body file not exists");
			logger.error("Body file not exists");
			System.exit(1);
		}catch(IllegalArgumentException e)
		{
			System.err.println("Specified charset is not found");
			logger.error("Specified charset is not found");
			System.exit(1);
		}
            
		return text;
	}
	//--------------------------------------------
	private static String getSubjectFromFile(String file, String charset)
	{
		logger.info("Get subject from file");
		String subject = null;
		File subjectFile = new File(file);
		try
		{
			Scanner scanner = new Scanner(subjectFile, charset);
            if(scanner.hasNextLine())
            	subject = scanner.nextLine();
            else
            {
            	System.err.println("Subject file is empty");
            	logger.error("Subject file is empty");
				System.exit(1);
            }
		} catch (FileNotFoundException e)
		{
			System.err.println("Subject file not exists");
			logger.error("Subject file not exists");
			System.exit(1);
		}catch(IllegalArgumentException e)
		{
			System.err.println("Specified charset is not found");
			logger.error("Specified charset is not found");
			System.exit(1);
		}
		return subject;
	}
	//--------------------------------------------
	private static ArrayList<Address> getEmailsFromFile(String file)
	{
		logger.info("Get emails from file");
		ArrayList<Address> emailsTo = new ArrayList<Address>();
		File emailsFile = new File(file);
		try
		{
			Scanner scanner = new Scanner(emailsFile);
            while(scanner.hasNextLine())
            {
                String email = scanner.nextLine();
                try
                {
                	emailsTo.add(new InternetAddress(email));
                }catch(AddressException ae)
                {
                	logger.warn(email +": "+ ae.getMessage());
                }
            }
		} catch (FileNotFoundException e)
		{
			System.err.println("File with emails not exists");
			logger.error("File with emails not exists");
			System.exit(1);
		}
            
		return emailsTo;
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
		//ogSubject.setRequired(true); //not required if emlFile was specified
		ogSubject.addOption(oSubject);
		ogSubject.addOption(oSubjectFile);
		
		Option oEmailTo = OptionBuilder.withArgName("recipients")
				.withDescription("specify recipients (comma separated)")
				.hasArgs()
				.withValueSeparator(',')
				.create("emailTo");
		Option oEmailToFile = OptionBuilder.withArgName("type>:<file")
				.withDescription("specify file with recipients list")
				.hasArgs(2)
				.withValueSeparator(':')
				.create("emailToFile");
		OptionGroup ogEmailTo = new OptionGroup();
		ogEmailTo.setRequired(true); 
		ogEmailTo.addOption(oEmailTo);
		ogEmailTo.addOption(oEmailToFile);
		
		Option oSendMethod = OptionBuilder.withArgName("method")
				.withDescription("method of sending a message to multiple recipients (to/cc/bcc/person)")
				.hasArg()
				.isRequired()
				.create("sendMethod");
		Option oPersonFrom = OptionBuilder.withArgName("person")
				.withDescription("specify sender name")
				.hasArg()
				.create("personFrom");
		Option oEmailFrom = OptionBuilder.withArgName("email")
				.withDescription("specify sender email")
				.hasArg()
				.isRequired()
				.create("emailFrom");
		
		Option oEmailToMaxPerMessage = OptionBuilder.withArgName("number")
				.withDescription("for to/cc method number of recipients per message")
				.hasArgs()
				.create("emailToMaxPerMessage");
		Option oEmailToBCC = OptionBuilder.withArgName("email")
				.withDescription("for BCC method specify TO (replace :Undisclosed)")
				.hasArg()
				.create("emailToBCC");
		OptionGroup ogMultiRecipients = new OptionGroup();
		ogMultiRecipients.addOption(oEmailToMaxPerMessage);
		ogMultiRecipients.addOption(oEmailToBCC);
		
		
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
		options.addOption(oSendMethod);
		options.addOption(oPersonFrom);
		options.addOptionGroup(ogMultiRecipients);
		return options;
	}

}
