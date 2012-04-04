package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;





public class Sender
{

	private String smtpHostName;
	private String smtpPort;
	private String smtpUser;
	private String smtpPassword;
	private String hostname;
	private String charset;
	private String contentTransferEncoding;
	private Method recipientType;
	
	private Properties mailProp;
	
	
	public enum Method {TO, CC, BCC, PERSON};

	public Sender(String smtpHostName, String smtpPort, String smtpUser,
			String smtpPassword, String hostname)
	{
		this.smtpHostName = smtpHostName;
		this.smtpPort = smtpPort;
		this.smtpUser = smtpUser;
		this.smtpPassword = smtpPassword;
		this.hostname = hostname;
		
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
		if(emailsTo.isEmpty())
		{
			throw new Exception("Recipients list is empty");
		}
		//get content from file
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
		Address addressFrom = new InternetAddress(emailFrom, personFrom, charset);
		send(content, contentType, subject, addressFrom, emailsTo);
	}
	//-----------------------------------------------
	private void send(Object text, String contentType, String subject, Address addressFrom
			, ArrayList<Address> emailsTo) throws Exception
	{
		if(emailsTo.isEmpty())
		{
			throw new Exception("Recipients list is empty");
		}
		//set mail Properties
		mailProp = new Properties();
		mailProp.put("mail.smtp.host", smtpHostName);
		mailProp.put("mail.smtp.port", smtpPort);
		mailProp.put("mail.smtp.localhost", hostname);
		mailProp.put("mail.user", smtpUser);
		mailProp.put("mail.password", smtpPassword);
		mailProp.put("mail.mime.charset", charset);
		mailProp.put("mail.transport.protocol", "smtp");
		
		//create session
		Session session = Session.getInstance(mailProp, null);
		//create messages
		ArrayList<MimeMessage> messages = new ArrayList<MimeMessage>();
		if(recipientType == Method.PERSON)
		{
			for(Address toEmail:emailsTo)
			{
				MimeMessage message = new MimeMessage(session);
				makeMessage(message, text, contentType, subject, addressFrom);
				try
				{
					message.setRecipient(RecipientType.TO, toEmail);
				} catch (MessagingException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				messages.add(message);
			}
		}
		else
		{
			MimeMessage message = new MimeMessage(session);
			makeMessage(message, text, contentType, subject, addressFrom);
			RecipientType rt = null;
			if(recipientType == Method.TO)
				rt = RecipientType.TO;
			if(recipientType == Method.CC)
				rt = RecipientType.CC;
			if(recipientType == Method.BCC)
				rt = RecipientType.BCC;
			try
			{
				message.setRecipients(rt, (Address[])emailsTo.toArray());
			} catch (MessagingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			messages.add(message);
		}
		
		//send messages
		Mailer.logger.info("Отсылаем");
		for(Message message:messages)
		{
			try
			{
				Transport.send(message);
			} catch (MessagingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Mailer.logger.info("Отправлено письмо");
	}
	//-----------------------------------------------
	private void makeMessage(MimeMessage message, Object text
			, String contentType, String subject, Address addressFrom) throws MessagingException
	{
		if(text == null)
			text = "";
		
		try
		{
			message.setFrom(addressFrom);
			message.setContent(text, contentType);
			message.setSubject(subject);
			message.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
			
		}catch (MessagingException e)
		{
			throw e;
		}
		
		
		
	}
	//-----------------------------------------------
	public void setCharset(String charset)
	{
		this.charset = charset;
	}
	//-----------------------------------------------
	public void setContentTransferEncoding(String contentTransferEncoding)
	{
		this.contentTransferEncoding = contentTransferEncoding;
	}
	//-----------------------------------------------
	public void setRecipientType(String recipientType)
	{
		Method rt;
		rt = Method.valueOf(recipientType);
		this.recipientType = rt;
		
	}
}
