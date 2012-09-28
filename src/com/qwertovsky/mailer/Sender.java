package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwertovsky.mailer.errors.QwertoMailerException;


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
	private boolean traceMessages = false;
	private List<String> badEmails;
	private List<String[]> badParameters;
	private List<Message> errorSendMessages;
	
	final Logger logger = LoggerFactory.getLogger(Sender.class);


	/**
	 * Create Sender class
	 * @param smtpHostName SMTP server
	 * @param smtpPort SMTP server port (default 25)
	 * @param smtpUser SMTP server account
	 * @param smtpPassword SMTP server password
	 * @param hostname local machine name
	 * @throws QwertoMailerException SMTP server is not specified (NULL or empty)
	 * @throws MessagingException 
	 */
	public Sender(String smtpHostName, int smtpPort, final String smtpUser,
			final String smtpPassword, String hostname)
		throws QwertoMailerException, MessagingException
	{
		if(smtpHostName == null || smtpHostName.length() == 0)
			throw new QwertoMailerException("SMTP server is not specified");
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
		mailProp.put("mail.smtp.socketFactory.port", smtpPort);
//		mailProp.put("mail.smtp.socketFactory.fallback", "false");
		
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
		Transport transport = session.getTransport("smtp");
		transport.connect();
		transport.close();
		
	}
		
	//-----------------------------------------------
	/**
	 * 
	 * Send personal messages to recipients
	 * @param messageContent message content
	 * @param emailsTo list of recipients
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * @throws MessagingException  
	 * @see #send(MessageContent, List, boolean)
	 * 
	 */
	public void send(MessageContent messageContent, List<InternetAddress> emailsTo)
	throws QwertoMailerException, MessagingException
		
	{
		send(messageContent, emailsTo, false);
	}
	
	//-----------------------------------------------
	/**
	 * 
	 * Send personal messages to recipients
	 * <br />If {@code haltOnFailure} is true and bad emails present
	 * , throw {@link QwertoMailerException}. Default value of  {@code haltOnFailure} is false.
	 * @param messageContent message content
	 * @param emailsTo list of recipients
	 * @param haltOnFailure
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * , Halt on failure (bad emails present)
	 * @throws MessagingException 
	 * @see #send(MessageContent, List) 
	 * 
	 */
	public void send(MessageContent messageContent, List<InternetAddress> emailsTo
			, boolean haltOnFailure)
	throws QwertoMailerException, MessagingException
		
	{
		if(messageContent == null)
			throw new QwertoMailerException("Message is null");
		String charset = messageContent.getCharset();
		Address from = messageContent.getAddressFrom();
		if(from == null)
			throw new QwertoMailerException("From email has not been specified");
	
		if(emailsTo == null || emailsTo.isEmpty())
		{
			throw new QwertoMailerException("Recipients list is empty");
		}
		
		mailProp.put("mail.mime.charset", charset);
		
		//create messages
		ArrayList<Message> messages = new ArrayList<Message>();
		logger.info("Create personal messages");
		badEmails = new ArrayList<String>();
		for(InternetAddress emailTo:emailsTo)
		{
			try
			{
				emailTo.validate();
				Message message = new Message(session);
				makeMessage(message, messageContent);
				message.setRecipient(RecipientType.TO, emailTo);
				messages.add(message);
			} catch(AddressException ae)
			{
				//bad address of recipient
				logger.error("Email " + emailTo.getAddress() + " is incorrect: "
						+ ae.getMessage());
				badEmails.add(emailTo.getAddress());
			} catch (MessagingException e)
			{
				logger.error("Message has not been created for "+ emailTo + "("+e.getMessage()+")");
				throw e;
			}
		}
		
		//halt on failure
		if(haltOnFailure && !badEmails.isEmpty())
		{
			throw new QwertoMailerException("Halt on failure");
		}
		
		//send messages
		logger.info("Start sending: " + messages.size() + " messages");
		for(Message message:messages)
		{
			sendMessage(message);
		}
		logger.info("End sending");
	}
	
	//--------------------------------------------
	/**
	 * Send messages. One array of parameters - one message.
	 * <br />Headers must contain "email*". Headers may contain "attach*".
	 * @param messageContent message content
	 * @param personParamHeaders headers of parameters
	 * @param personParameters list of parameters
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws MessagingException
	 * @see #send(MessageContent, String[], List, boolean) 
	 * 
	 */
	public void send(MessageContent messageContent, String[] personParamHeaders,
			List<String[]> personParameters)
	throws QwertoMailerException, IOException, MessagingException, ParseException
	{
		send(messageContent, personParamHeaders, personParameters, false);
	}
	
	//--------------------------------------------
	/**
	 * Send messages. One array of parameters - one message.
	 * <br />Headers must contain "email*". Headers may contain "attach*".
	 * <br />If {@code haltOnFailure} is true and bad emails present or bad parameters present
	 * , throw {@link QwertoMailerException}. Default value of  {@code haltOnFailure} is false.
	 * @param messageContent message content
	 * @param personParamHeaders headers of parameters
	 * @param personParameters list of parameters
	 * @param haltOnFailure
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * , Emails not present in file
	 * , Halt on failure (bad emails present)
	 * @throws ParseException 
	 * @throws IOException 
	 * @throws MessagingException
	 * @see #send(MessageContent, String[], List) 
	 * 
	 */
	public void send(MessageContent messageContent, String[] personParamHeaders,
			List<String[]> personParameters, boolean haltOnFailure)
	throws QwertoMailerException, IOException, MessagingException, ParseException
	{
		if(messageContent == null)
			throw new QwertoMailerException("Message is null");
		String charset = messageContent.getCharset();
		Address from = messageContent.getAddressFrom();
		if(from == null)
			throw new QwertoMailerException("From email has not been specified");
	
		if(personParameters == null || personParameters.isEmpty())
		{
			throw new QwertoMailerException("Recipients list is empty");
		}
		
		mailProp.put("mail.mime.charset", charset);
		Velocity.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
	      "org.apache.velocity.slf4j.Slf4jLogChute");
		Velocity.setProperty("runtime.log.logsystem.slf4j.name",
			"com.qwertovsky.mailer");
		Velocity.init();
		
		
		badEmails = new ArrayList<String>();
		badParameters = new ArrayList<String[]>();
		
		//get indexes of emails (email%) and attachments (attach%)
		int[] emailIndexes = getEmailIndexes(personParamHeaders);
		int[] attachIndexes = getAttachIndexes(personParamHeaders);
		
		//recipients must be
		if(emailIndexes.length == 0)
			throw new QwertoMailerException("Emails not present in file");
		
		//create messages
		ArrayList<Message> messages = new ArrayList<Message>();
		
		logger.info("Create personal messages");
		
		for(String[] parameters:personParameters)
		{
			//get emails
			InternetAddress[] recipientsArray = getRecipientsList(emailIndexes, parameters);
			if(recipientsArray == null)
			{
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < parameters.length; i++)
				{
					sb.append("\"" + parameters[i] + "\"");
					if((i + 1) < parameters.length)
						sb.append(", ");
				}
				logger.error("Recipients list is empty: " + sb.toString());
				badParameters.add(parameters);
				continue;
			}
			
			//get attachments
			List<File> attachments = getAttachments(attachIndexes, parameters);
			
			//create individual message content
			MessageContent content = new MessageContent(messageContent);
			content.addAttachments(attachments);
			try
			{
				content.setParameters(personParamHeaders, parameters);
			} catch (QwertoMailerException qme)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("Message has not been created (" + qme.getMessage() + ") for: ");
				for(int i = 0; i < parameters.length; i++)
				{
					sb.append("\"" + parameters[i] + "\"");
					if((i + 1) < parameters.length)
						sb.append(", ");
				}
				logger.error(sb.toString());
				badParameters.add(parameters);
				continue;
			}
			
			
			Message message = new Message(session);
			try
			{
				makeMessage(message, content);
				message.setRecipients(RecipientType.TO, recipientsArray);
			} catch (MessagingException e)
			{
				logger.error("Message has not been created for "
						+ recipientsArray[0].getAddress() + "("+e.getMessage()+")");
				throw e;
			}
			message.setParameters(parameters);
			messages.add(message);
		}
		
		//halt on failure
		if(haltOnFailure && (!badEmails.isEmpty() || !badParameters.isEmpty()))
			throw new QwertoMailerException("Halt on failure");
		
		//send messages
		logger.info("Start sending");
		errorSendMessages = new ArrayList<Message>();
		for(Message message:messages)
		{
			sendMessage(message);
		}
		logger.info("End sending");
		
	}
	
	//--------------------------------------------
	/**
	 * Send MimeMessage
	 * @param message MimeMessage
	 */
	protected void sendMessage(Message message)
	{
		try
		{
			//update message-id
			message.saveChanges();
			
			//send message
			Transport.send(message);
			
			if(traceMessages || logger.isTraceEnabled())
			{
				//save message to file messageId.eml and write log
				String messageId = null;
				try
				{
					messageId = message.getMessageID();
					messageId = messageId.substring(1, messageId.length()-1);
					File dir = new File("messages");
					if(!dir.exists())
						dir.mkdir();
					File file = new File("messages/" + messageId + ".eml");
					message.writeTo(new FileOutputStream(file));
				} catch (Exception e)
				{
					logger.warn("Error save message: " + messageId
							+ "(" + e.getMessage() + ")");
				} 
				
				//log about send message
				//append recipients to log message
				StringBuilder sb = new StringBuilder();
				Address[] recipients = null;
				try
				{
					recipients = message.getAllRecipients();
					int i=0;
					for(; i < 3 && i < recipients.length; i++)
					{
						if(sb.length() > 0)
							sb.append(", ");
						sb.append(((InternetAddress)recipients[i]).getAddress());
					}
					if(i < recipients.length)
						sb.append("...");
				} catch (Exception e1)
				{
					sb.append("error get recipients");
				}
				logger.trace("Message " + messageId +" has been send to: " + sb.toString());
			}
		} catch (Exception me)
		{
			//log about not send message
			//append recipients to log message
			StringBuilder sb = new StringBuilder();
			Address[] recipients = null;
			try
			{
				recipients = message.getAllRecipients();
				int i=0;
				for(; i < 3 && i < recipients.length; i++)
				{
					if(sb.length() > 0)
						sb.append(", ");
					sb.append(((InternetAddress)recipients[i]).getAddress());
				}
				if(i < recipients.length)
					sb.append("...");
			} catch (Exception e1)
			{
				sb.append("error get recipients");
			}
			errorSendMessages.add(message);			
			logger.error("Error ("+ me.getMessage() +") send message to: " + sb.toString());
		}
		
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
				try
				{
					int[] temp = emailIndexes.clone();
					emailIndexes = new int[emailIndexes.length + 1];
					System.arraycopy(temp, 0, emailIndexes, 0, temp.length);
					emailIndexes[emailIndexes.length-1] = i;
				} catch (Exception e)
				{
					//nothing
				}
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
				try
				{
					int[] temp = attachIndexes.clone();
					attachIndexes = new int[attachIndexes.length + 1];
					System.arraycopy(temp, 0, attachIndexes, 0, temp.length);
					attachIndexes[attachIndexes.length-1] = i;
				} catch (Exception e)
				{
					//nothing
				}
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
				for(int i = 0; i < parameters.length; i++)
				{
					sb.append("\"" + parameters[i] + "\"");
					if((i + 1) < parameters.length)
						sb.append(", ");
				}
				logger.error("Attachment not present in column " + index + " (first is 0):"
						 + sb.toString());
				badParameters.add(parameters);
				return attachments;
			}
			
			try
			{
				String fileString = parameters[index];
				if(fileString == null)
					continue;
				fileString = fileString.trim();
				if(fileString.length() == 0)
					continue;
				File file = new File(fileString);
				if(file.exists())
					attachments.add(file);
				else
				{
					logger.error("File " + fileString + " not exists");
					if(badParameters != null)
						badParameters.add(parameters);
				}
			} catch (Exception e)
			{
				logger.error("Error on get attachments: " + e.getMessage());
				if(badParameters != null)
					badParameters.add(parameters);
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
				for(int i = 0; i < parameters.length; i++)
				{
					sb.append("\"" + parameters[i] + "\"");
					if((i + 1) < parameters.length)
						sb.append(", ");
				}
				logger.error("Email not present in column " + index + " (first is 0):"
						 + sb.toString());
				if(badParameters != null)
					badParameters.add(parameters);
				continue;
			}
			String emailString = parameters[index];
			if(emailString == null)
				continue;
			emailString = emailString.trim();
			if(emailString.length() == 0)
				continue;
			String[] emails = emailString.split(",| ");
			for(String email:emails)
			{
				if(email == null || email.length() == 0)
					continue;
				try
				{
					InternetAddress address = new InternetAddress(email);
					address.validate();
					recipientsList.add(address);
				}catch(AddressException ae)
				{
					logger.warn("Email " + email + " is incorrect: " + ae.getMessage());
					if(badEmails != null)
						badEmails.add(email);
				}
			}
		}
		if(recipientsList == null || recipientsList.isEmpty())
		{
			return null;
		}
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

	//--------------------------------------------
	public void setTraceMessages(boolean trace)
	{
		this.traceMessages = trace;
	}
	
	//--------------------------------------------
	/**
	 * Get bad emails of recipients, that were in the last send operation.
	 * @return bad email addresses
	 */
	public List<String> getBadEmails()
	{
		return badEmails;
	}
	
	//--------------------------------------------
	/**
	 * Get bad parameters of recipients, that were in the last send operation.
	 * @return bad parameters
	 */
	public List<String[]> getBadParameters()
	{
		return badParameters;
	}
	
	//--------------------------------------------
	/**
	 * Get not send messages that were in the last send operation.
	 * @return not send messages
	 */
	public List<Message> getErrorSendMessages()
	{
		return errorSendMessages;
	}
}
