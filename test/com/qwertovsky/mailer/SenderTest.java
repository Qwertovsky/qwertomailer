package com.qwertovsky.mailer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.qwertovsky.mailer.errors.QwertoMailerException;


/**
 * @author Qwertovsky
 *
 */
@RunWith(value = Parameterized.class)
public class SenderTest
{	
	Set<InternetAddress> emailsTo;
	File emlFile = null;
	String smtpHost;
	int smtpPort;
	String smtpUser;
	String smtpPassword;
	String method;
	String hostname;
	
	static Wiser wiser;
	static Set<Address> validTo;
	static Set<Address> invalidTo;
	
	public static Logger loggerOrg = Logger.getLogger("org");
	public static Logger loggerCom = Logger.getLogger("com");
	
	@Parameters
	public static Collection<Object[]> parameters() throws AddressException, UnknownHostException, IOException
	{
		validTo = new HashSet<Address>();
		validTo.add(new InternetAddress("address1@from"));
		validTo.add(new InternetAddress("address2"));
		invalidTo = new HashSet<Address>();
		invalidTo.add(new InternetAddress("address1"));
		invalidTo.add(new InternetAddress("address2"));
		
		String pattern = "[%d{yyyy-MM-dd HH:mm:ss} %-4r][%-5p] %m%n";
	    PatternLayout layout = new PatternLayout(pattern);
		ConsoleAppender appender = new ConsoleAppender(layout);
		loggerOrg.addAppender(appender);
		loggerCom.addAppender(appender);
		loggerOrg.setLevel(Level.WARN);
		loggerCom.setLevel(Level.WARN);
		
		wiser = new Wiser(2500);
		wiser.start();
		
		return Arrays.asList(new Object[][] { 
				{null, null, 0, null, null, null, null} //QwertoMailerException
				, {null, "", 0, null, null, null, null}  //QwertoMailerException
				, {null, "smtp.host.rus", 0, null, null, null, null} //Unknown SMTP host:
				, {null, "mail.ru", 0, null, null, null, null} //Could not connect to SMTP host: 
				, {null, "localhost", 2500, null, null, null, null} //QwertoMailerException 
				, {invalidTo, "localhost", 2500, null, null, null, null} 
				
				}); 
	}
	
	public SenderTest(Set<InternetAddress> emailsTo
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
		} catch (QwertoMailerException qme)
		{
			//pass
			return;
		} catch (Exception e)
		{
			if(smtpHost.equals("localhost"))
			{
				e.printStackTrace();
				fail(e.getMessage());
			}
			if("Unknown SMTP host: smtp.host.rus".equals(e.getMessage()))
				return;
			if(!smtpHost.equals("") && e.getMessage() != null && e.getMessage().startsWith("Could not connect to SMTP host:"))
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
			emlFile = new File("test_files/test.eml");
			MessageContent messageContent = new MessageContent(emlFile);
			messageContent.setAddressFrom("from", "from@domain", "utf-8");
			sender.send(messageContent, emailsTo);
			
			//check results
			List<WiserMessage> wiserMessages = wiser.getMessages();
			if(emailsTo.equals(invalidTo))
			{
				if(!wiserMessages.isEmpty())
					fail("incorrect send method");
			}
			else
			{
				WiserMessage wiserMessage = wiserMessages.get(0);
				String from = wiserMessage.getEnvelopeSender();
			    assertEquals("not correct from", "from@domain",from);
			    String to = wiserMessage.getEnvelopeReceiver();
			    assertEquals("not correct to", "address1",to);
			    String contentType = wiserMessage.getMimeMessage().getContentType();
			    assertTrue("not correct content type", contentType.startsWith("text/html"));
			    
			    wiserMessage = wiserMessages.get(1);
				from = wiserMessage.getEnvelopeSender();
			    assertEquals("not correct from", "from@domain",from);
			    to = wiserMessage.getEnvelopeReceiver();
			    assertEquals("not correct to", "address2",to);
			    contentType = wiserMessage.getMimeMessage().getContentType();
			    assertTrue("not correct content type", contentType.startsWith("text/html"));
			}
		} catch(QwertoMailerException qme)
		{
			if(emailsTo == null || emailsTo.isEmpty())
				;//pass
			else
			{
				qme.printStackTrace();
				fail(qme.getMessage());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());	
		}
		//--------------------
		try
		{
			emlFile = new File("test_files/test_multipart_alt.eml");
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from@domain", "utf-8");
			sender.send(message, emailsTo);

			//check results
			List<WiserMessage> wiserMessages = wiser.getMessages();
			if(emailsTo.equals(invalidTo))
			{
				if(!wiserMessages.isEmpty())
					fail("incorrect send method");
			}
			else
			{
				WiserMessage wiserMessage = wiserMessages.get(2);
				String from = wiserMessage.getEnvelopeSender();
			    assertEquals("not correct from", "from@domain",from);
			    String to = wiserMessage.getEnvelopeReceiver();
			    assertEquals("not correct to", "address1",to);
			    String contentType = wiserMessage.getMimeMessage().getContentType();
			    assertTrue("not correct content type", contentType.startsWith("multipart/alternative"));
			    
			    wiserMessage = wiserMessages.get(3);
				from = wiserMessage.getEnvelopeSender();
			    assertEquals("not correct from", "from@domain",from);
			    to = wiserMessage.getEnvelopeReceiver();
			    assertEquals("not correct to", "address2",to);
			    contentType = wiserMessage.getMimeMessage().getContentType();
			    assertTrue("not correct content type", contentType.startsWith("multipart/alternative"));
			}
		} catch(QwertoMailerException qme)
		{
			if(emailsTo == null || emailsTo.isEmpty())
				;//pass
			else
			{
				qme.printStackTrace();
				fail(qme.getMessage());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());	
		}
		//--------------------
		try
		{
			emlFile = new File("test_files/test_multipart_mixed.eml");
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from@domain", "utf-8");
			sender.send(message, emailsTo);

			//check results
			List<WiserMessage> wiserMessages = wiser.getMessages();
			if(emailsTo.equals(invalidTo))
			{
				if(!wiserMessages.isEmpty())
					fail("incorrect send method");
			}
			else
			{
				WiserMessage wiserMessage = wiserMessages.get(4);
				String from = wiserMessage.getEnvelopeSender();
			    assertEquals("not correct from", "from@domain",from);
			    String to = wiserMessage.getEnvelopeReceiver();
			    assertEquals("not correct to", "address1",to);
			    String contentType = wiserMessage.getMimeMessage().getContentType();
			    assertTrue("not correct content type", contentType.startsWith("multipart/mixed"));
			    
			    wiserMessage = wiserMessages.get(5);
				from = wiserMessage.getEnvelopeSender();
			    assertEquals("not correct from", "from@domain",from);
			    to = wiserMessage.getEnvelopeReceiver();
			    assertEquals("not correct to", "address2",to);
			    contentType = wiserMessage.getMimeMessage().getContentType();
			    assertTrue("not correct content type", contentType.startsWith("multipart/mixed"));
			}
		} catch(QwertoMailerException qme)
		{
			if(emailsTo == null || emailsTo.isEmpty())
				;//pass
			else
			{
				qme.printStackTrace();
				fail(qme.getMessage());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());	
		}
		
	}
	
	
}
