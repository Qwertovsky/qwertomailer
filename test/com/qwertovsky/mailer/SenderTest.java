package com.qwertovsky.mailer;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class SenderTest
{	
	Object content;
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
	
	@Parameters
	public static Collection<Object[]> parameters() throws AddressException
	{
		String validAddress1 = "address";
		String validAddress2 = "address";
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
					, null, null, null, null, null, null, null} //EML file is null
				, {"", null, null, null, null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //EML file not exists
				, {"test.eml", null, null, null, null, null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Bad email in FROM
				, {"test.eml", null, null, null, "from", null, null, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Recipients list is empty
				, {"test.eml", null, null, null, "from", null, to, "smtp.host.rus"
					, null, null, null, null, null, null, null} //Unknown SMTP host: smtp.host.rus
				, {"test.eml", null, null, null, "from", null, to, "mail.ru"
					, "80", null, null, null, null, null, null} //Could not connect to SMTP host: mail.ru, port: 80
				, {"test.eml", null, null, null, "from", null, to, "mail.ru"
					, null, null, null, null, null, null, null} //Exception reading response
				,
				{"test.eml", "", "texts", "", "from", "", to, "smtp.mail.ru"
					, "", "", "", "", "", "", ""} //Bad ContentType
				,
				{"test.eml", "", "text/plain", "", "from", "", to, "smtp.mail.ru"
					, "", "", "", "", "", "", ""} //Bad content
				
				}); 
	}
	
	public SenderTest(String file, Object content, String contentType, String subject
			, String emailFrom, String personFrom, ArrayList<Address> emailsTo
			, String host, String port, String user, String password
			, String charset, String contentTransferEncoding, String method, String hostname)
	{
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
	public void testSendFileStringStringArrayListOfAddress()
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
			sender.send(emlFile, emailFrom, personFrom, emailsTo);
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

	@Test
	public void testSendFileStringStringStringArrayListOfAddress()
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
			sender.send(emlFile, subject, emailFrom, personFrom, emailsTo);
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

	@Test
	public void testSendObjectStringStringStringStringArrayListOfAddress()
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
			sender.send(content, contentType, subject, emailFrom, personFrom, emailsTo);
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
