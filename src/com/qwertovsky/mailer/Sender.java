package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;


public class Sender
{

	private String smtpHostName;
	private String smtpPort;
	private String smtpUser;
	private String smtpPassword;
	private String hostname;
	private String charset = "UTF-8";
	private String contentTransferEncoding = "8bit";
	private Method recipientType = Method.PERSON;
	
	private Properties mailProp;
	
	
	public enum Method {TO, CC, BCC, PERSON};

	public Sender(String smtpHostName, String smtpPort, String smtpUser,
			String smtpPassword, String hostname) throws Exception
	{
		this.smtpHostName = smtpHostName;
		this.smtpPort = smtpPort;
		this.smtpUser = smtpUser;
		this.smtpPassword = smtpPassword;
		this.hostname = hostname;
		
		if(smtpHostName == null || smtpHostName.length() == 0)
			throw new Exception("SMTP server is not specified");
		if(smtpPort == null || smtpPort.length() == 0)
			this.smtpPort = "25";
		if(smtpUser == null)
			this.smtpUser = "";
		if(smtpPassword == null)
			this.smtpPassword = "";
		if(hostname == null)
			this.hostname = "";
	}
	//---------------------------------------------
	public void send(File emlFile, String emailFrom, String personFrom, ArrayList<Address> emailsTo)
	throws Exception
	{
		send(emlFile, null, emailFrom, personFrom, emailsTo);
	}
	//---------------------------------------------
	public void send(File emlFile, String subject, String emailFrom
			, String personFrom, ArrayList<Address> emailsTo) throws Exception
	{
		
		if(emlFile == null)
		{
			throw new Exception("EML file is null");
		}
		//get content from file
		System.out.println("Get content from EMl file");
		Session mailSession = Session.getDefaultInstance(new Properties(), null);
		
		Object content;
		String contentType;
		try
		{
			InputStream isEML = null;
			isEML = new FileInputStream(emlFile);
			Message message = new MimeMessage(mailSession, isEML);
			content = message.getContent();
			contentType = message.getContentType();
			if(subject == null)
				subject = message.getSubject();
		} catch (FileNotFoundException e)
		{
			throw new Exception("EML file not exists");
		} catch (MessagingException e)
		{
			throw e;
		} catch (IOException e)
		{
			throw e;
		}
		
			
		send(content, contentType, subject, emailFrom, personFrom, emailsTo);
	}
	//---------------------------------------------
	public void send(Object content, String contentType, String subject, String emailFrom
			, String personFrom, ArrayList<Address> emailsTo) throws Exception
	{
		if(emailFrom == null || emailFrom.length()==0)
			throw new Exception("Bad email in FROM");
		Address addressFrom = new InternetAddress(emailFrom, personFrom, charset);
		send(content, contentType, subject, addressFrom, emailsTo);
	}
	//-----------------------------------------------
	private void send(Object text, String contentType, String subject, Address addressFrom
			, ArrayList<Address> emailsTo) throws Exception
	{
		if(emailsTo == null || emailsTo.isEmpty())
		{
			throw new Exception("Recipients list is empty");
		}
		if(text == null || text.equals(new String("")))
		{
			throw new Exception("Bad content");
		}
		if(contentType == null || contentType.length() == 0)
		{
			throw new Exception("Bad ContentType");
		}
		try
		{
			new ContentType(contentType);
		}catch(ParseException pe)
		{
			throw new Exception("Bad ContentType: "+ pe.getMessage());
		}
		
		//set mail Properties
		mailProp = new Properties();
		mailProp.put("mail.smtp.host", smtpHostName);
		mailProp.put("mail.smtp.port", smtpPort);
		mailProp.put("mail.smtp.localhost", hostname);
		mailProp.put("mail.mime.charset", charset);
		mailProp.put("mail.transport.protocol", "smtp");
		
		mailProp.put("mail.smtp.auth", "true");
		mailProp.put("mail.smtp.starttls.enable", "true");
		mailProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		
		//create session
		Session session = Session.getInstance(mailProp, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(smtpUser,smtpPassword);
			}
		});
		try
		{
			Transport transport = session.getTransport();
			transport.connect();
			transport.close();
		}catch(NoSuchProviderException nspe)
		{
			throw nspe;
		}
		//create messages
		ArrayList<MimeMessage> messages = new ArrayList<MimeMessage>();
		if(recipientType == Method.PERSON)
		{
			System.out.println("Create personal messages");
			for(Address emailTo:emailsTo)
			{
				MimeMessage message = new MimeMessage(session);
				try
				{
					makeMessage(message, text, contentType, subject, addressFrom);
					message.setRecipient(RecipientType.TO, emailTo);
				} catch (MessagingException e)
				{
					System.err.println(e.getMessage());
					System.err.println("Message not created for "+ emailTo);
					continue;
				}
				messages.add(message);
			}
		}
		else
		{
			System.out.println("Create message");
			MimeMessage message = new MimeMessage(session);
			
			RecipientType rt = null;
			if(recipientType == Method.TO)
				rt = RecipientType.TO;
			if(recipientType == Method.CC)
				rt = RecipientType.CC;
			if(recipientType == Method.BCC)
				rt = RecipientType.BCC;
			try
			{
				makeMessage(message, text, contentType, subject, addressFrom);
				message.setRecipients(rt, (Address[])emailsTo.toArray());
			} catch (MessagingException e)
			{
				System.err.println(e.getMessage());
				return;
			}
			messages.add(message);
		}
		
		//send messages
		System.out.println("Start sending");
		for(Message message:messages)
		{
			try
			{
				Transport.send(message);
			} catch (MessagingException e)
			{
				StringBuilder sb = new StringBuilder("Error send message to: ");
				for(Address a:message.getAllRecipients())
				{
					sb.append(((InternetAddress)a).getAddress() +", ");
				}
				System.err.println(sb.toString() + e.getMessage());
				
			}
		}
		System.out.println("End sending");
	}
	//-----------------------------------------------
	private void makeMessage(MimeMessage message, Object text
			, String contentType, String subject, Address addressFrom) throws MessagingException
	{
		//content can't be empty
		if(text == null)
			text = " ";
		//add charset 
		if(!contentType.contains("charset="))
			contentType = contentType + ";charset=" + charset;
		try
		{
			message.setFrom(addressFrom);
			message.setContent(text, contentType);
			message.setSubject(subject);
			message.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
		}
		catch (MessagingException e)
		{
			throw e;
		}
	}
	//-----------------------------------------------
	public void setCharset(String charset)
	{
		if(charset != null)
			this.charset = charset;
	}
	//-----------------------------------------------
	public void setContentTransferEncoding(String contentTransferEncoding)
	{
		if(contentTransferEncoding != null)
			this.contentTransferEncoding = contentTransferEncoding;
	}
	//-----------------------------------------------
	public void setRecipientType(String recipientType)
	{
		if(recipientType != null)
		{
			Method sendMethod = Method.valueOf(recipientType.toUpperCase());
			this.recipientType = sendMethod;
		}
	}
}
