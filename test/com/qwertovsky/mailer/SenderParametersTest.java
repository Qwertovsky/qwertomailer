package com.qwertovsky.mailer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.velocity.runtime.parser.ParseException;
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
	public void testSendParameters()
	throws QwertoMailerException, MessagingException, IOException, ParseException
	{
		//error
		Sender sender = new Sender("localhost",2500,null,null,null);
		MessageContent messageContent = new MessageContent("message $message"
				, "text/plain", "subject $subject", "utf-8");
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
		//--------------------
		//success
		personParamHeaders = new String[]{"message", "subject", "parameter"
				, "email"};
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
			List<WiserMessage> wiserMessages = wiser.getMessages();
			for(int i = 0; i < wiserMessages.size(); i++)
			{
				MimeMessage message = wiserMessages.get(i).getMimeMessage();
				String subject = message.getSubject();
				String content = (String) message.getContent();
				content = content.trim();
				String address = ((InternetAddress)message.getRecipients(RecipientType.TO)[0])
					.getAddress();
				if(!("subject subject"+String.valueOf(i+1)).equals(subject))
					fail("incorrect subject");
				if(!("message message"+String.valueOf(i+1)).equals(content))
					fail("incorrect message");
				if(!("address"+String.valueOf(i+1)).equals(address))
					fail("incorrect recipient");
			}
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
		} catch (QwertoMailerException qme)
		{
			//pass
			//Halt on failure
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect send");
		}
		
		
	}
	
	//--------------------------------------------
	@Test
	public void testGetEmailIndexes() 
	throws NoSuchProviderException, Exception
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		String[] headers = new String[]{"Email","email","email1","mail","parameter"," email2"};
		int[] indexes = sender.getEmailIndexes(headers);
		if(indexes == null)
			fail("email indexes are null");
		assertEquals(4, indexes.length);
		assertEquals(0, indexes[0]);
		assertEquals(1, indexes[1]);
		assertEquals(2, indexes[2]);
		assertEquals(5, indexes[3]);
	}
	
	//--------------------------------------------
	@Test
	public void testGetAttachIndexes() throws NoSuchProviderException, Exception
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		String[] headers = new String[]{"Email","Attach","attach","attach1","parameter"," attach2"};
		int[] indexes = sender.getAttachIndexes(headers);
		if(indexes == null)
			fail("attach indexes are null");
		assertEquals(4, indexes.length);
		assertEquals(1, indexes[0]);
		assertEquals(2, indexes[1]);
		assertEquals(3, indexes[2]);
		assertEquals(5, indexes[3]);
		
	}
	
	//--------------------------------------------
	@Test
	public void testGetRecipientsList() throws NoSuchProviderException, Exception
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		String[] parameters = new String[]{"address1@domain, address2@domain"
				, "address3@domain address4@domain"
				, "parameter"
				, " address5@domain "
				, "address6@domain"};
		int[] indexes = new int[]{0,1,3,4};
		InternetAddress[] recipients = sender.getRecipientsList(indexes, parameters);
		if(recipients == null || recipients.length != 6)
			fail("incorrect getRecipientsList");
		assertEquals("incorrect getRecipientsList","address1@domain",recipients[0].getAddress());
		assertEquals("incorrect getRecipientsList","address2@domain",recipients[1].getAddress());
		assertEquals("incorrect getRecipientsList","address3@domain",recipients[2].getAddress());
		assertEquals("incorrect getRecipientsList","address4@domain",recipients[3].getAddress());
		assertEquals("incorrect getRecipientsList","address5@domain",recipients[4].getAddress());
		assertEquals("incorrect getRecipientsList","address6@domain",recipients[5].getAddress());
	}
	
	//--------------------------------------------
	@Test
	public void testGetAttachments() throws NoSuchProviderException, Exception
	{
		Sender sender = new Sender("localhost",2500,null,null,null);
		String[] parameters = new String[]{"test.png"
				, "test.eml"
				, "parameter"
				, "notexists_file"
				, " test.png "};
		int[] indexes = new int[]{0,1,3,4};
		List<File> attachments = sender.getAttachments(indexes, parameters);
		if(attachments == null || attachments.size() != 3)
			fail("incorrect getAttachments");
		assertEquals("incorrect getAttachments","test.png",attachments.get(0).getName());
		assertEquals("incorrect getAttachments","test.eml",attachments.get(1).getName());
		assertEquals("incorrect getAttachments","test.png",attachments.get(2).getName());
		
	}
}
