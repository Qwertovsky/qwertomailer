package com.qwertovsky.mailer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.velocity.runtime.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import com.qwertovsky.mailer.errors.QwertoMailerException;


public class SenderParametersTest
{
	public static Logger logger1 = Logger.getLogger("org.subethamail");
	public static Logger logger2 = Logger.getLogger("com.qwertovsky.mailer");
	Wiser wiser;
	
	public SenderParametersTest()
	{
		String pattern = "[%d{yyyy-MM-dd HH:mm:ss} %-4r][%-5p] %m%n";
	    PatternLayout layout = new PatternLayout(pattern);
		ConsoleAppender appender = new ConsoleAppender(layout);
		logger1.addAppender(appender);
		logger1.setLevel(Level.WARN);
		logger2.addAppender(appender);
		
		wiser = new Wiser(2500);
		wiser.start();
		
	}
	
	//--------------------------------------------
	@Test
	@Ignore
	public void testSendParameters()
	throws QwertoMailerException, MessagingException, IOException, ParseException
	{
		//error
		Sender sender = new Sender("localhost",2500,null,null,null);
		MessageContent messageContent = new MessageContent("message $message"
				, "text/plain", "subject $subject", "utf-8");
		messageContent.setAlternativeText("alternative $message", "utf-8");
		
		String[] personParamHeaders = new String[]{"message", "subject", "parameter"};
		ArrayList<String[]> personParameters = new ArrayList<String[]>();
		String[] parameters = new String[]{"message1"
				, "subject1", "parameter1"};
		personParameters.add(parameters);
		parameters = new String[]{"message2"
				, "subject2", "parameter2"};
		personParameters.add(parameters);
		parameters = new String[]{"message3"
				, "subject3", "parameter3"};
		personParameters.add(parameters);
		try
		{
			sender.send(messageContent, personParamHeaders, personParameters);
		} catch (QwertoMailerException qme)
		{
			//pass
			//From email has not been specified
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect send");
		}
		
		//--------------------
		//error
		messageContent.setAddressFrom("from", "addressFrom@domain", "utf-8");
		try
		{
			sender.send(messageContent, personParamHeaders, personParameters);
		}catch(Exception e)
		{
			if(!"Emails not present in file".equals(e.getMessage()))
				fail("incorrect send");
		}
		//--------------------
		//error
		personParamHeaders = new String[]{"message", "subject", "parameter"};
		personParameters = new ArrayList<String[]>();
		parameters = new String[]{"message1"
				, "subject1", "parameter1", "address1"};
		personParameters.add(parameters);
		parameters = new String[]{"message2"
				, "subject2", "parameter2", "address2"};
		personParameters.add(parameters);
		parameters = new String[]{"message3"
				, "subject3", "parameter3", "address3"};
		personParameters.add(parameters);
		try
		{
			sender.send(messageContent, personParamHeaders, personParameters);
		}catch(Exception e)
		{
			if(!"Emails not present in file".equals(e.getMessage()))
				fail("incorrect send");
		}
		//--------------------
		//success 0 message
		personParamHeaders = new String[]{"message", "subject", "parameter"
				, "email"};
		personParameters = new ArrayList<String[]>();
		parameters = new String[]{"message1"
				, "subject1", "parameter1"};
		personParameters.add(parameters);
		parameters = new String[]{"message2"
				, "subject2", "parameter2"};
		personParameters.add(parameters);
		parameters = new String[]{"message3"
				, "subject3", "parameter3"};
		personParameters.add(parameters);
		sender.send(messageContent, personParamHeaders, personParameters);
		assertEquals(0, wiser.getMessages().size());
		
	}
	
	//--------------------------------------------
	@Test
	public void testSendInlineParameters()
	throws QwertoMailerException, MessagingException, IOException
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		MessageContent messageContent = new MessageContent("message $message"
				, "text/plain", "subject $subject", "utf-8");
		messageContent.setAlternativeText("alternative $message", "utf-8");
		messageContent.setAddressFrom("from", "addressFrom@domain", "utf-8");
		
		String[] personParamHeaders = new String[]{"message", "subject", "parameter"
				, "email", "email"};
		ArrayList<String[]> personParameters = new ArrayList<String[]>();
		
		String[] parameters;
		parameters = new String[]{"message1"
				, "subject1", "parameter1", "address1@domain", "second_address@domain"};
		personParameters.add(parameters);
		
		try
		{
			sender.send(messageContent, personParamHeaders, personParameters);
			List<WiserMessage> wiserMessages = wiser.getMessages();
			//2 addresses in parameters - 2 messages on server
			if(wiserMessages.size() != 2)
				fail("Incorrect messages count");
			MimeMessage message = wiserMessages.get(0).getMimeMessage();
			String subject = message.getSubject();
			Multipart body = (Multipart) message.getContent();
			String htmlPart = (String) body.getBodyPart(1).getContent();
			String altPart = (String) body.getBodyPart(0).getContent();
			String address = ((InternetAddress)message.getRecipients(RecipientType.TO)[0])
				.getAddress();
			
			if(!subject.contains("subject subject"))
				fail("incorrect subject");
			if(!htmlPart.contains("message message"))
				fail("incorrect message html part");
			if(!altPart.contains("alternative message"))
				fail("incorrect message alternative part");
			if(!address.contains("address"))
				fail("incorrect recipient");
			
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect sendParameters");
		}
	}
	//--------------------------------------------
	@Test
	public void testSendParametersHaltOnFailure()
	throws QwertoMailerException, MessagingException, IOException, ParseException
	{
		//error
		//QwertoMailerException must rise
		Sender sender = new Sender("localhost",2500,null,null,null);
		MessageContent messageContent = new MessageContent("message $message"
				, "text/plain", "subject $subject", "utf-8");
		String[] personParamHeaders = new String[]{"message", "subject", "parameter"
				, "email"};
		ArrayList<String[]> personParameters = new ArrayList<String[]>();
		String[] parameters = new String[]{"message1"
				, "subject1", "parameter1", "address1"};
		personParameters.add(parameters);
		parameters = new String[]{"message2"
				, "subject2", "parameter2", "address2"};
		personParameters.add(parameters);
		parameters = new String[]{"message3"
				, "subject3", "parameter3", "address3"};
		personParameters.add(parameters);
		messageContent.setAddressFrom("from", "addressFrom@domain", "utf-8");
		try
		{
			sender.send(messageContent, personParamHeaders, personParameters, true);
			fail("Incorrect send");
		} catch (QwertoMailerException qme)
		{
			if((sender.getBadParameters() == null || sender.getBadParameters().isEmpty())
					&& (sender.getBadEmails() == null || sender.getBadEmails().isEmpty())
				)
				fail("incorrect halt on failure");
			
			//pass
			//Halt on failure
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect halt on failure");
		}
		
		
	}
	
	//--------------------------------------------
	@Test
	public void testGetRecipientsList() throws NoSuchProviderException, Exception
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("email1", "address1@domain, address2@domain");
		parameters.put("email2", "address3@domain address4@domain");
		parameters.put("param", "parameter");
		parameters.put("email3", " address5@domain ");
		parameters.put("email4", "address6@domain");
		
		Set<InternetAddress> recipients = sender.getRecipientsList(parameters);
		if(recipients == null || recipients.size() != 6)
			fail("incorrect getRecipientsList");
		assertTrue("incorrect getRecipientsList",recipients.contains(new InternetAddress("address1@domain")));
		assertTrue("incorrect getRecipientsList",recipients.contains(new InternetAddress("address2@domain")));
		assertTrue("incorrect getRecipientsList",recipients.contains(new InternetAddress("address3@domain")));
		assertTrue("incorrect getRecipientsList",recipients.contains(new InternetAddress("address4@domain")));
		assertTrue("incorrect getRecipientsList",recipients.contains(new InternetAddress("address5@domain")));
		assertTrue("incorrect getRecipientsList",recipients.contains(new InternetAddress("address6@domain")));
	}
	
	//--------------------------------------------
	@Test
	public void testGetAttachments() throws NoSuchProviderException, Exception
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("attach1", "test.png");
		parameters.put("attach2", "test.eml");
		parameters.put("param", "parameter");
		parameters.put("attach3", "notexists_file");
		parameters.put("attach4", " test.png ");

		List<File> attachments = sender.getAttachments(parameters);
		if(attachments == null || attachments.size() != 3)
			fail("incorrect getAttachments");
		assertEquals("incorrect getAttachments","test.png",attachments.get(0).getName());
		assertEquals("incorrect getAttachments","test.eml",attachments.get(1).getName());
		assertEquals("incorrect getAttachments","test.png",attachments.get(2).getName());
		
	}
}
