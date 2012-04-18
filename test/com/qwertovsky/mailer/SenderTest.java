package com.qwertovsky.mailer;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
	List<Address> emailsTo;
	File emlFile = null;
	String smtpHost;
	String smtpPort;
	String smtpUser;
	String smtpPassword;
	String method;
	String hostname;
	
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
				{null, null, null, null, null, null, null} //SMTP server is not specified
				, {null, "", null, null, null, null, null}  //SMTP server is not specified
				, {null, "smtp.host.rus", null, null, null, null, null} //Recipients list is empty
				, {to, "smtp.host.rus", null, null, null, null, null} //Unknown SMTP host: smtp.host.rus
				, {to, "smtp.mail.ru", "80", null, null, null, null} //Could not connect to SMTP host: 
				, {to, "mail.ru", null, null, null, null, null} //Exception reading response 
				}); 
	}
	
	public SenderTest(List<Address> emailsTo
			, String host, String port, String user, String password
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
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from", "utf-8");
			sender.send(message, emailsTo);
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
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
			emlFile = new File("test_multipart_alt.eml");
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from", "utf-8");
			sender.send(message, emailsTo);
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
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
			emlFile = new File("test_multipart_mixed.eml");
			MessageContent message = new MessageContent(emlFile);
			message.setAddressFrom("from", "from", "utf-8");
			sender.send(message, emailsTo);
		} catch (Exception e)
		{
			if("Recipients list is empty".equals(e.getMessage()))
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
		
	}
	

}
