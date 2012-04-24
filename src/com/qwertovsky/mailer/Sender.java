package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
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

	private Properties mailProp;
	private Session session; 
	
	final Logger logger = LoggerFactory.getLogger(Sender.class);


	/**
	 * Create Sender class
	 * @param smtpHostName SMTP server
	 * @param smtpPort SMTP server port (default 25)
	 * @param smtpUser SMTP server account
	 * @param smtpPassword SMTP server password
	 * @param hostname local machine name
	 * @throws Exception SMTP server is not specified (NULL or empty)
	 * @throws NoSuchProviderException
	 */
	public Sender(String smtpHostName, int smtpPort, final String smtpUser,
			final String smtpPassword, String hostname)
		throws Exception, NoSuchProviderException
	{

		
		if(smtpHostName == null || smtpHostName.length() == 0)
			throw new Exception("SMTP server is not specified");
		if(smtpPort == 0)
			smtpPort = 25;
		if(hostname == null)
			hostname = "";
		
		//set mail Properties
		mailProp = new Properties();
		mailProp.put("mail.smtp.host", smtpHostName);
		mailProp.put("mail.smtp.port", smtpPort);
		mailProp.put("mail.smtp.localhost", hostname);
		
		mailProp.put("mail.transport.protocol", "smtp");
		mailProp.put("mail.smtp.connectiontimeout","5000");
		mailProp.put("mail.smtp.timeout","5000");
		
		mailProp.put("mail.smtp.auth", "true");
		mailProp.put("mail.smtp.starttls.enable", "true");
		mailProp.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		
		//create session
		if(smtpUser != null && smtpPassword != null)
		{
			Authenticator authenticator = new Authenticator()
			{
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(smtpUser,smtpPassword);
				}
			};
			session = Session.getInstance(mailProp,	authenticator);
		}
		else
		{
			Authenticator authenticator = new Authenticator()
			{
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication("","");
				}
			};
			session = Session.getInstance(mailProp,	authenticator);
		}
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
	}
		
	//-----------------------------------------------
	/**
	 * Send personal messages to recipients
	 * @param messageContent message body
	 * @param emailsTo list of recipients
	 * @throws Exception
	 * 
	 */
	public void send(MessageContent messageContent, List<Address> emailsTo)
		throws Exception
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
		
		mailProp.put("mail.mime.charset", charset);
		
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
	
	//--------------------------------------------
	public void send(MessageContent messageContent, String[] personParamHeaders,
			ArrayList<String[]> personParameters) throws Exception
	{
		if(messageContent == null)
			throw new Exception("Message is not created");
		String charset = messageContent.getCharset();
		Address from = messageContent.getAddressFrom();
		if(from == null)
			throw new Exception("Bad email in FROM");
	
		if(personParameters == null || personParameters.isEmpty())
		{
			throw new Exception("Recipients list is empty");
		}
		
		mailProp.put("mail.mime.charset", charset);
		Velocity.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
	      "org.apache.velocity.slf4j.Slf4jLogChute");
		Velocity.setProperty("runtime.log.logsystem.slf4j.name",
			"com.qwertovsky.mailer");
		Velocity.init();
		
		//get indexes of emails (email%) and attachments (attach%)
		int[] emailIndexes = getEmailIndexes(personParamHeaders);
		int[] attachIndexes = getAttachIndexes(personParamHeaders);
		
		//recipients must be
		if(emailIndexes.length == 0)
			throw new Exception("Emails not present in file");
		
		//create messages
		ArrayList<Message> messages = new ArrayList<Message>();
		logger.info("Create personal messages");
		for(String[] parameters:personParameters)
		{
			//get emails
			InternetAddress[] recipientsArray = getRecipientsList(emailIndexes, parameters);
			
			//get attachments
			List<File> attachments = getAttachments(attachIndexes, parameters);
			
			//create individual message content
			MessageContent content = new MessageContent(messageContent);
			content.addAttachments(attachments);
			try
			{
				content.setParameters(personParamHeaders, parameters);
			}
			catch(Exception e)
			{
				if("Bad parameters for message".equals(e.getMessage())
						|| "Parameters must be not less then headers".equals(e.getMessage()))
				{
					StringBuilder sb = new StringBuilder();
					sb.append("Message has not been created (" + e.getMessage() + ") for: ");
					for(int i = 0; i < parameters.length; i++)
					{
						sb.append("\"" + parameters[i] + "\"");
						if((i + 1) < parameters.length)
							sb.append(", ");
					}
					logger.warn(sb.toString());
					continue;
				}
				throw e;
			}
			
			Message message = new Message(session);
			try
			{
				makeMessage(message, content);
				message.setRecipients(RecipientType.TO, recipientsArray);
			} catch (MessagingException e)
			{
				logger.warn("Message has not been created for "
						+ recipientsArray[0].getAddress() + "("+e.getMessage()+")");
				continue;
			}
			messages.add(message);
		}
		
		//send messages
		logger.info("Start sending");
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
					
					//append recipients to log message
					StringBuilder sb = new StringBuilder("");
					Address[] recipients = message.getAllRecipients();
					int i=0;
					for(; i < 3 && i < recipients.length; i++)
					{
						if(sb.length() > 0)
							sb.append(", ");
						sb.append(((InternetAddress)recipients[i]).getAddress());
					}
					if(i < recipients.length)
						sb.append("...");
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
	
	//--------------------------------------------
	/**
	 * Get indexes of emails in parameters array
	 * <br />Email headers start with "email"
	 * @param personParamHeaders headers of parameters
	 * @return indexes array
	 */
	protected int[] getEmailIndexes(String[] personParamHeaders)
	{
		if(personParamHeaders == null || personParamHeaders.length == 0)
			return null;
		int[] emailIndexes = new int[0];
		for(int i=0; i < personParamHeaders.length; i++)
		{
			String header = personParamHeaders[i];
			if(header == null)
				continue;
			if(header.toLowerCase().trim().startsWith("email"))
			{
				int[] temp = emailIndexes.clone();
				emailIndexes = new int[emailIndexes.length + 1];
				System.arraycopy(temp, 0, emailIndexes, 0, temp.length);
				emailIndexes[emailIndexes.length-1] = i;
				continue;
			}
		}
		return emailIndexes;
	}
	
	//--------------------------------------------
	/**
	 * Get indexes of attachments in parameters array
	 * <br />Attachment headers start with "attach"
	 * @param personParamHeaders headers of parameters
	 * @return indexes array
	 */
	protected int[] getAttachIndexes(String[] personParamHeaders)
	{
		if(personParamHeaders == null || personParamHeaders.length == 0)
			return null;
		int[] attachIndexes = new int[0];
		for(int i=0; i < personParamHeaders.length; i++)
		{
			String header = personParamHeaders[i];
			if(header == null)
				continue;
			if(header.toLowerCase().trim().startsWith("attach"))
			{
				int[] temp = attachIndexes.clone();
				attachIndexes = new int[attachIndexes.length + 1];
				System.arraycopy(temp, 0, attachIndexes, 0, temp.length);
				attachIndexes[attachIndexes.length-1] = i;
				continue;
			}
		}
		return attachIndexes;
	}

	//--------------------------------------------
	/**
	 * Get attachments list
	 * @param attachIndexes array of indexes in parameters array
	 * @param parameters array of parameters
	 * @return attachments list
	 */
	protected List<File> getAttachments(int[] attachIndexes, String[] parameters)
	{
		if(attachIndexes == null || attachIndexes.length == 0
				|| parameters == null || parameters.length == 0)
			return null;
		
		List<File> attachments = new ArrayList<File>(attachIndexes.length);
		for(int index:attachIndexes)
		{
			if(parameters.length <= index)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("Attachment not present in column " + index + " (first is 0):");
				for(int i = 0; i < parameters.length; i++)
				{
					sb.append("\"" + parameters[i] + "\"");
					if((i + 1) < parameters.length)
						sb.append(", ");
				}
				logger.warn(sb.toString());
				continue;
			}
			String fileString = parameters[index];
			fileString = fileString.trim();
			if(fileString == null || fileString.length() == 0)
				continue;
			File file = new File(fileString);
			if(file.exists())
				attachments.add(file);
			else
			{
				logger.warn("File " + fileString + " not exists");
			}
		}
		return attachments;
	}

	//--------------------------------------------
	/**
	 * Get recipient list
	 * @param emailIndexes array of indexes (index of email-parameter in parameters)
	 * @param parameters array of parameters
	 * @return recipients addresses list
	 */
	protected InternetAddress[] getRecipientsList(int[] emailIndexes,
			String[] parameters)
	{
		if(emailIndexes == null || emailIndexes.length == 0
				|| parameters == null || parameters.length == 0)
			return null;
		List<InternetAddress> recipientsList = new ArrayList<InternetAddress>(emailIndexes.length);
		for(int index:emailIndexes)
		{
			if(parameters.length <= index)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("Email not present in column " + index + " (first is 0):");
				for(int i = 0; i < parameters.length; i++)
				{
					sb.append("\"" + parameters[i] + "\"");
					if((i + 1) < parameters.length)
						sb.append(", ");
				}
				logger.warn(sb.toString());
				continue;
			}
			String emailString = parameters[index];
			if(emailString == null)
				continue;
			String[] emails = emailString.split(",| ");
			for(String email:emails)
			{
				if(email == null || email.length() == 0)
					continue;
				try
				{
					recipientsList.add(new InternetAddress(email));
				}catch(AddressException ae)
				{
					logger.warn(email + ":" + ae.getMessage());
				}
			}
		}
		if(recipientsList == null || recipientsList.isEmpty())
			return null;
		return recipientsList.toArray(new InternetAddress[0]);
	}

	//-----------------------------------------------
	/**
	 * Put data to MimeMessage from MessageContent
	 * @param message
	 * @param mailMessage
	 * @throws MessagingException
	 */
	protected void makeMessage(Message message, MessageContent mailMessage) throws MessagingException
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
