package com.qwertovsky.mailer;

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
	private Type recipientType;
	
	private Properties mailProp;
	
	
	public enum Type {TO, CC, BCC, PERSON};

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
	public int send(String text, String contentType, String subject, String fromEmail
			, String fromUser, ArrayList<Address> toEmails)
	{
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
		if(recipientType == Type.PERSON)
		{
			for(Address toEmail:toEmails)
			{
				MimeMessage message = new MimeMessage(session);
				makeMessage(message, text, contentType, subject, fromEmail, fromUser);
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
			makeMessage(message, text, contentType, subject, fromEmail, fromUser);
			RecipientType rt = null;
			if(recipientType == Type.TO)
				rt = RecipientType.TO;
			if(recipientType == Type.CC)
				rt = RecipientType.CC;
			if(recipientType == Type.BCC)
				rt = RecipientType.BCC;
			try
			{
				message.setRecipients(rt, (Address[])toEmails.toArray());
			} catch (MessagingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			messages.add(message);
		}
		
		if (messages.isEmpty())
	    {
	      Mailer.logger.warn("Список адресатов пуст");
	      return -1;
	    }
		
		//send messages
		Mailer.logger.trace("Отсылаем");
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
	    return 0;
	}
	//-----------------------------------------------
	private void makeMessage(MimeMessage message, String text
			, String contentType, String subject, String emailFrom, String userFrom)
	{
		if(text == null)
			text = "";
		InternetAddress addressFrom = null;
		try
		{
			addressFrom = new InternetAddress(emailFrom, userFrom, charset);
			message.setFrom(addressFrom);
			message.setContent(text, contentType);
			message.setSubject(subject);
			message.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
			
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MessagingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		Type rt;
		rt = Type.valueOf(recipientType);
		this.recipientType = rt;
		
	}
}
