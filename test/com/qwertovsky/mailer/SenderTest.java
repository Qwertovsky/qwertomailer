package com.qwertovsky.mailer;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class SenderTest
{	
	String content;
	String contentType;
	String subject;
	String emailFrom;
	String personFrom;
	ArrayList<Address> emailsTo;
	File emlFile = null;
	String smtpHost;
	String smtpPort;
	String smtpUser;
	String smtpPassword;
	String charset;
	String contentTransferEncoding;
	String method;
	String hostname;
	
	public static Logger logger = Logger.getLogger("com.qwertovsky.mailer");
	
	@Parameters
	public static Collection<Object[]> parameters() throws AddressException
	{
		String validAddress1 = "address1";
		String validAddress2 = "address2";
		Address address1 = new InternetAddress(validAddress1);
		Address address2 = new InternetAddress(validAddress2);
		ArrayList<Address> to = new ArrayList<Address>();
		to.add(address1);
		to.add(address2);
		
		return Arrays.asList(new Object[][] { 
				{null, null, null, null, null, null, null, null
					, null, null, null, null, null, null, null} //SMTP server is not specified
				, {null, null, null, null, null, null, null, ""
					, null, null, null, null, null, null, null}  //SMTP server is not specified
				, {null, null, null, null, null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //EML file is null, Bad content
				, {"", "", null, null, null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //EML file not exists, Bad content
				, {"test_bad_subject.eml", "text", null, null, null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Bad subject
				, {"test.eml", "text", null, "subject", null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Bad email in FROM, Bad ContentType
				, {"test.eml", "text", "", "subject", null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Bad email in FROM, Bad ContentType
				, {"test.eml", "text", "text", "subject", null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Bad email in FROM, Bad ContentType
				, {"test.eml", "text", "text/plain", "subject", null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Bad email in FROM
				, {"test.eml", "text", "text/plain", "subject", "from", null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Recipients list is empty
				, {"test.eml", "text", "text/plain", "subject", "from", null, to, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Unknown SMTP host: smtp.host.rus
				, {"test.eml", "text", "text/plain", "subject", "from", null, to, "smtp.mail.ru"
					, "80", null, null, null, null, null, null} //Could not connect to SMTP host: 
				, {"test.eml", "text", "text/plain", "subject", "from", null, to, "mail.ru"
					, null, null, null, null, null, null, null} //Exception reading response 
				
				}); 
	}
	
	public SenderTest(String file, String content, String contentType, String subject
			, String emailFrom, String personFrom, ArrayList<Address> emailsTo
			, String host, String port, String user, String password
			, String charset, String contentTransferEncoding, String method, String hostname)
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
		
		if(file != null)
			this.emlFile = new File(file);
		this.content = content;
		this.contentType = contentType;
		this.subject = subject;
		this.emailFrom = emailFrom;
		this.personFrom = personFrom;
		this.emailsTo = emailsTo;
		smtpHost = host;
		smtpPort = port;
		smtpUser = user;
		smtpPassword = password;
		this.charset = charset;
		this.contentTransferEncoding = contentTransferEncoding;
		this.method = method;
		this.hostname = hostname;
	}
	
	
	
	@Test
	@Ignore
	public void testSendFromFile()
	{
		Sender sender = null;
		try
		{
			sender = new Sender(smtpHost,smtpPort, smtpUser, smtpPassword, hostname);
		} catch (Exception e)
		{
			if("SMTP server is not specified".equals(e.getMessage()))
				return;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		try
		{
			MailMessage message = new MailMessage(emlFile);
			message.setContentTransferEncoding(contentTransferEncoding);
//			message.setSubject(subject);
			message.setAddressFrom(personFrom, emailFrom, charset);
			message.addAttachment(new File("test.eml"));
			message.setAlternativeText("алтернативный", "utf-8");
			sender.send(message, emailsTo);
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
				return;
			if("EML file is null".equals(e.getMessage()))
				return;
			if("EML file not exists".equals(e.getMessage()))
				return;
			if("Unknown SMTP host: smtp.host.rus".equals(e.getMessage()))
				return;
			if(e.getMessage() != null && e.getMessage().startsWith("Could not connect to SMTP host:"))
				return;
			if("Exception reading response".equals(e.getMessage()))
				return;
			if("Bad email in FROM".equals(e.getMessage()))
				return;
			if(e.getMessage() != null && e.getMessage().startsWith("Bad ContentType"))
				return;
			if("Bad content".equals(e.getMessage()))
				return;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		
	}
	//--------------------------------------------
	@Test
//	@Ignore
	public void testSend()
	{
		Sender sender = null;
		try
		{
			sender = new Sender(smtpHost,smtpPort, smtpUser, smtpPassword, hostname);
		} catch (Exception e)
		{
			if("SMTP server is not specified".equals(e.getMessage()))
				return;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		try
		{
			MailMessage message = new MailMessage(content, contentType, subject, charset);
			message.setContentTransferEncoding(contentTransferEncoding);
			message.setAddressFrom(personFrom, emailFrom, charset);
			message.addAttachment(new File(".git/index"));
			message.setAlternativeText("алтернативный", "utf-8");
			sender.send(message, emailsTo);
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
				return;
			if("EML file is null".equals(e.getMessage()))
				return;
			if("EML file not exists".equals(e.getMessage()))
				return;
			if("Unknown SMTP host: smtp.host.rus".equals(e.getMessage()))
				return;
			if(e.getMessage() != null && e.getMessage().startsWith("Could not connect to SMTP host:"))
				return;
			if("Exception reading response".equals(e.getMessage()))
				return;
			if("Bad email in FROM".equals(e.getMessage()))
				return;
			if(e.getMessage() != null && e.getMessage().startsWith("Bad ContentType"))
				return;
			if("Bad content".equals(e.getMessage()))
				return;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		
	}
	

}
