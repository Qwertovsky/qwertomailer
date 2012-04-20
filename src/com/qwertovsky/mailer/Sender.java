package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send messages.<br \>
 * Contain setting for connect to mail server: server host, server port, user, password.<br \>
 * Contain setting, that same for many messages: recipientType.
 * @author Qwertovsky
 *
 */
public class Sender
{

	private String smtpHostName;
	private String smtpPort;
	private String smtpUser;
	private String smtpPassword;
	private String hostname;
	
	private Properties mailProp;
	
	final Logger logger = LoggerFactory.getLogger(Sender.class);


	/**
	 * Create Sender class
	 * @param smtpHostName SMTP server
	 * @param smtpPort SMTP server port (default 25)
	 * @param smtpUser SMTP server account
	 * @param smtpPassword SMTP server password
	 * @param hostname local machine name
	 * @throws Exception SMTP server is not specified (NULL or empty)
	 */
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
		
	//-----------------------------------------------
	/**
	 * @param messageContent message body
	 * @param emailsTo list of recipients
	 * @throws Exception
	 * @throws NoSuchProviderException
	 */
	public void send(MessageContent messageContent, List<Address> emailsTo)
		throws Exception, NoSuchProviderException
	{
		if(messageContent == null)
			throw new Exception("Message is not created");
		String charset = messageContent.getCharset();
		Address from = messageContent.getAddressFrom();
		if(from == null)
			throw new Exception("Bad email in FROM");
	
		if(emailsTo == null || emailsTo.isEmpty())
		{
			throw new Exception("Recipients list is empty");
		}
		
		//set mail Properties
		mailProp = new Properties();
		mailProp.put("mail.smtp.host", smtpHostName);
		mailProp.put("mail.smtp.port", smtpPort);
		mailProp.put("mail.smtp.localhost", hostname);
		mailProp.put("mail.mime.charset", charset);
		mailProp.put("mail.transport.protocol", "smtp");
		mailProp.put("mail.smtp.connectiontimeout","5000");
		mailProp.put("mail.smtp.timeout","5000");
		
		mailProp.put("mail.smtp.auth", "true");
		mailProp.put("mail.smtp.starttls.enable", "true");
		mailProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		
		//create session
		Session session = Session.getInstance(mailProp
				, new javax.mail.Authenticator()
					{
						protected PasswordAuthentication getPasswordAuthentication()
						{
							return new PasswordAuthentication(smtpUser,smtpPassword);
						}
					}
				);
		//check mail server
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
		ArrayList<Message> messages = new ArrayList<Message>();
		logger.info("Create personal messages");
		for(Address emailTo:emailsTo)
		{
			Message message = new Message(session);
			try
			{
				makeMessage(message, messageContent);
				message.setRecipient(RecipientType.TO, emailTo);
			} catch (MessagingException e)
			{
				logger.warn("Message has not been created for "+ emailTo + "("+e.getMessage()+")");
				continue;
			}
			messages.add(message);
		}
		
		//send messages
		logger.info("Start sending");
		int i = 0;
		for(Message message:messages)
		{
			try
			{
				message.saveChanges();
				Transport.send(message);
				if(logger.isTraceEnabled())
				{
					String messageId = message.getMessageID();
					messageId = messageId.substring(1, messageId.length()-1);
					File dir = new File("messages");
					if(!dir.exists())
						dir.mkdir();
					File file = new File("messages/" + messageId + ".eml");
					message.writeTo(new FileOutputStream(file));
					i++;
					StringBuilder sb = new StringBuilder("");
					//append all recipients to message
					for(Address a:message.getAllRecipients())
					{
						sb.append(((InternetAddress)a).getAddress() +", ");
					}
					logger.trace("Message " + messageId +" has been send to: " + sb.toString());
				}
			} catch (MessagingException e)
			{
				StringBuilder sb = new StringBuilder("Error ("+ e.getMessage() +") send message to: ");
				//append all recipients to message
				for(Address a:message.getAllRecipients())
				{
					sb.append(((InternetAddress)a).getAddress() +", ");
				}
				logger.warn(sb.toString());
			}
		}
		logger.info("End sending");
	}
	
	//-----------------------------------------------
	private void makeMessage(Message message, MessageContent mailMessage) throws MessagingException
	{
		Object content = mailMessage.getContent();
		String contentType = null;
		if(content instanceof Multipart)
			contentType = ((Multipart)content).getContentType();
		else
			contentType = mailMessage.getContentType();
		Address from = mailMessage.getAddressFrom();
		String subject = mailMessage.getSubject();
		String charset = mailMessage.getCharset();
		String contentTransferEncoding = mailMessage.getContentTransferEncoding();
		
		message.setFrom(from);
		message.setContent(content, contentType);
		message.setSubject(subject, charset);
		message.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
	}
	
}
