package com.qwertovsky.mailer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

/**
 * @author Qwertovsky
 *
 */
@RunWith(value = Parameterized.class)
public class SenderTest
{	
	List<Address> emailsTo;
	File emlFile = null;
	String smtpHost;
	int smtpPort;
	String smtpUser;
	String smtpPassword;
	String method;
	String hostname;
	
	static Wiser wiser;
	
	public static Logger logger1 = Logger.getLogger("org");
	public static Logger logger2 = Logger.getLogger("com");
	
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
		
		String pattern = "[%d{yyyy-MM-dd HH:mm:ss} %-4r][%-5p] %m%n";
	    PatternLayout layout = new PatternLayout(pattern);
		ConsoleAppender appender = new ConsoleAppender(layout);
		logger1.addAppender(appender);
		logger2.addAppender(appender);
		logger1.setLevel(Level.WARN);
		logger2.setLevel(Level.WARN);
		
		wiser = new Wiser(2500);
		wiser.start();
		
		return Arrays.asList(new Object[][] { 
				{null, null, 0, null, null, null, null} //SMTP server is not specified
				, {null, "", 0, null, null, null, null}  //SMTP server is not specified
				, {null, "smtp.host.rus", 0, null, null, null, null} //Recipients list is empty
				, {to, "smtp.host.rus", 0, null, null, null, null} //Unknown SMTP host: smtp.host.rus
				, {to, "smtp.mail.ru", 80, null, null, null, null} //Could not connect to SMTP host: 
				, {to, "mail.ru", 0, null, null, null, null} //Exception reading response 
				, 
				{to, "localhost", 2500, null, null, null, null} 
				}); 
	}
	
	public SenderTest(List<Address> emailsTo
			, String host, int port, String user, String password
			,  String method, String hostname)
	{
		this.emailsTo = emailsTo;
		smtpHost = host;
		smtpPort = port;
		smtpUser = user;
		smtpPassword = password;
		this.method = method;
		this.hostname = hostname;
		
		
	}
	
	
	//--------------------------------------------
	@Test
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
			if("Unknown SMTP host: smtp.host.rus".equals(e.getMessage()))
				return;
			if(e.getMessage() != null && e.getMessage().startsWith("Could not connect to SMTP host:"))
				return;
			if("Exception reading response".equals(e.getMessage()))
				return;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		//--------------------
		try
		{
			emlFile = new File("test.eml");
			MessageContent messageContent = new MessageContent(emlFile);
			messageContent.setAddressFrom("from", "from", "utf-8");
			sender.send(messageContent, emailsTo);
			
			//check results
			List<WiserMessage> wiserMessages = wiser.getMessages();
			WiserMessage wiserMessage = wiserMessages.get(0);
			String from = wiserMessage.getEnvelopeSender();
		    assertEquals("not correct from", "from",from);
		    String to = wiserMessage.getEnvelopeReceiver();
		    assertEquals("not correct to", "address1",to);
		    String contentType = wiserMessage.getMimeMessage().getContentType();
		    assertTrue("not correct content type", contentType.startsWith("text/html"));
		    
		    wiserMessage = wiserMessages.get(1);
			from = wiserMessage.getEnvelopeSender();
		    assertEquals("not correct from", "from",from);
		    to = wiserMessage.getEnvelopeReceiver();
		    assertEquals("not correct to", "address2",to);
		    contentType = wiserMessage.getMimeMessage().getContentType();
		    assertTrue("not correct content type", contentType.startsWith("text/html"));
		    
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
				return;
			
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		//--------------------
		try
		{
			emlFile = new File("test_multipart_alt.eml");
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from", "utf-8");
			sender.send(message, emailsTo);

			//check results
			List<WiserMessage> wiserMessages = wiser.getMessages();
			WiserMessage wiserMessage = wiserMessages.get(2);
			String from = wiserMessage.getEnvelopeSender();
		    assertEquals("not correct from", "from",from);
		    String to = wiserMessage.getEnvelopeReceiver();
		    assertEquals("not correct to", "address1",to);
		    String contentType = wiserMessage.getMimeMessage().getContentType();
		    assertTrue("not correct content type", contentType.startsWith("multipart/alternative"));
		    
		    wiserMessage = wiserMessages.get(3);
			from = wiserMessage.getEnvelopeSender();
		    assertEquals("not correct from", "from",from);
		    to = wiserMessage.getEnvelopeReceiver();
		    assertEquals("not correct to", "address2",to);
		    contentType = wiserMessage.getMimeMessage().getContentType();
		    assertTrue("not correct content type", contentType.startsWith("multipart/alternative"));
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
				return;
			
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		//--------------------
		try
		{
			emlFile = new File("test_multipart_mixed.eml");
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from", "utf-8");
			sender.send(message, emailsTo);

			//check results
			List<WiserMessage> wiserMessages = wiser.getMessages();
			WiserMessage wiserMessage = wiserMessages.get(4);
			String from = wiserMessage.getEnvelopeSender();
		    assertEquals("not correct from", "from",from);
		    String to = wiserMessage.getEnvelopeReceiver();
		    assertEquals("not correct to", "address1",to);
		    String contentType = wiserMessage.getMimeMessage().getContentType();
		    assertTrue("not correct content type", contentType.startsWith("multipart/mixed"));
		    
		    wiserMessage = wiserMessages.get(5);
			from = wiserMessage.getEnvelopeSender();
		    assertEquals("not correct from", "from",from);
		    to = wiserMessage.getEnvelopeReceiver();
		    assertEquals("not correct to", "address2",to);
		    contentType = wiserMessage.getMimeMessage().getContentType();
		    assertTrue("not correct content type", contentType.startsWith("multipart/mixed"));
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
				return;
			
			else
			{
				e.printStackTrace();
				fail(e.getMessage());	
			}
		}
		
	}
	
	
}
