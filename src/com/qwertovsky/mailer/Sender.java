package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qwertovsky.mailer.errors.QwertoMailerException;


/**
 * Sends messages.<br \>
 * Contains setting for connect to mail server: server host, server port, user, password.<br \>
 * 
 * <p>For example:</p>
 * <pre class="code">
 * //simple text message
 * Hello, $name.
 * Introduces a new tariff plan $tariff.</pre>
 * 
 * <pre class="code">
 * //and code
 * MessageContent message = new MessageContent(new File("message.eml"));
 * message.setAddressFrom("Company", "sender@company.com", "utf-8");
 * message.setSubject("New tariff $tariff");
 * 
 * //parameters same for all recipients
 * String[] parametersHeaders = new String[]{"tariff"};
 * String[] parameters = new String[]{"Super 146"};
 * message.setParameters(parametersHeaders, parameters);
 * message.addAttachment(new File("super146.pdf"));
 * 
 * //person parameters
 * parametersHeaders = new String[]{"email1", "name"};
 * List<String[]> personParameters = new ArrayList<String[]>();
 * personParameters.add(new String[]{"abon16545@gmail.com", "John Doe"});
 * personParameters.add(new String[]{"abon2111@gmail.com", "Mr.X"});
 * Sender sender = new Sender("smtp.company.com", 25, "login", "password", "Company");
 * sender.send(message, parametersHeaders, personParameters);</pre>
 * 
 * @author Qwertovsky
 *
 */
public class Sender
{

	private Properties mailProp;
	private Session session;
	private boolean traceMessages = false;
	private List<String> badEmails;
	private List<Map<String, String>> badParametersMap;
	private List<Message> notSentMessages;
	private List<Message> sentMessages;
	
	final Logger logger = LoggerFactory.getLogger(Sender.class);


	/**
	 * Create Sender class
	 * @param smtpHostName SMTP server
	 * @param smtpPort SMTP server port (default 25)
	 * @param smtpUser SMTP server account
	 * @param smtpPassword SMTP server password
	 * @param hostname local machine name
	 * @throws QwertoMailerException SMTP server is not specified (NULL or empty)
	 * @throws Exception 
	 */
	public Sender(String smtpHostName, int smtpPort, final String smtpUser,
			final String smtpPassword, String hostname)
		throws QwertoMailerException, Exception
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
	 * <br />New message for each address
	 * @param messageContent message content
	 * @param emailsTo list of recipients
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * @throws Exception  
	 * @see #send(MessageContent, List, boolean)
	 * 
	 */
	public void send(MessageContent messageContent, Set<InternetAddress> emailsTo)
	throws QwertoMailerException, Exception
		
	{
		send(messageContent, emailsTo, false);
	}
	
	//-----------------------------------------------
	/**
	 * 
	 * Send personal messages to recipients
	 * <br />New message for each address
	 * <br />If {@code haltOnFailure} is true and bad emails present
	 * , throw {@link QwertoMailerException}. Default value of  {@code haltOnFailure} is false.
	 * @param messageContent message content
	 * @param emailsTo list of recipients
	 * @param haltOnFailure
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * , Halt on failure (bad emails present)
	 * @throws Exception 
	 * @see #send(MessageContent, List) 
	 * 
	 */
	public void send(MessageContent messageContent, Set<InternetAddress> emailsTo
			, boolean haltOnFailure)
	throws QwertoMailerException, Exception
		
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
		notSentMessages = new ArrayList<Message>();
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
	 * <br />One message for parameters array. If array contains more than one addresses
	 * , will be created one message with several addresses in field TO:
	 * @param messageContent message content
	 * @param personParamHeaders headers of parameters
	 * @param personParameters list of parameters
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * @throws Exception 
	 * @see #send(MessageContent, String[], List, boolean) 
	 * 
	 */
	public void send(MessageContent messageContent, String[] personParamHeaders,
			List<String[]> personParameters)
	throws QwertoMailerException, Exception
	{
		send(messageContent, personParamHeaders, personParameters, false);
	}
	
	//--------------------------------------------
	/**
	 * Send messages. One array of parameters - one message.
	 * <br />Headers must contain "email*". Headers may contain "attach*".
	 * <br />One message for parameters array. If array contains more than one addresses
	 * , will be created one message with several addresses in field TO:
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
	 * @throws Exception 
	 * @see #send(MessageContent, String[], List) 
	 * 
	 */
	public void send(MessageContent messageContent, String[] personParamHeaders,
			List<String[]> personParameters, boolean haltOnFailure)
	throws QwertoMailerException, Exception
	{
		//create list of parameters maps
		int listSize = personParameters.size();
		int mapSize = personParamHeaders.length;
		List<Map<String, String>> parametersMapList = new ArrayList<Map<String, String>>(listSize);
		for(String[] parameters:personParameters)
		{
			Map<String, String> parametersMap = new HashMap<String,String>(mapSize);
			for(int i=0; i<personParamHeaders.length; i++)
			{
				String parameterValue = null;
				if(i < parameters.length)
					parameterValue = parameters[i];
				String header = personParamHeaders[i];
				if(parametersMap.containsKey(header))
	    		{
					int index = 0;
					do
					{
						index++;
					}
					while(parametersMap.containsKey(header + "_" + index));
					header = header + "_" + index;
			    }
				parametersMap.put(header, parameterValue);
			}
			parametersMapList.add(parametersMap);
		}
		
		//invoke #send(MessageContent, List)
		send(messageContent, parametersMapList, haltOnFailure);
	}
	
	//--------------------------------------------
	/**
	 * Send messages. One array of parameters - one message.
	 * <br />Headers must contain "email*". Headers may contain "attach*".
	 * <br />One message for parameters array. If array contains more than one addresses
	 * , will be created one message with several addresses in field TO:
	 * <br />If {@code haltOnFailure} is true and bad emails present or bad parameters present
	 * , throw {@link QwertoMailerException}
	 * @param messageContent message content
	 * @param personParameters list of parameters map
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * , Emails not present in file
	 * @throws Exception 
	 * @see #send(MessageContent, List, boolean) 
	 * 
	 */
	public void send(MessageContent messageContent, List<Map<String, String>> personParameters)
	throws QwertoMailerException, Exception
	{
		send(messageContent, personParameters, false);
	}
	
	//--------------------------------------------
	/**
	 * Send messages. One array of parameters - one message.
	 * <br />Headers must contain "email*". Headers may contain "attach*".
	 * <br />One message for parameters array. If array contains more than one addresses
	 * , will be created one message with several addresses in field TO:
	 * <br />If {@code haltOnFailure} is true and bad emails present or bad parameters present
	 * , throw {@link QwertoMailerException}. Default value of  {@code haltOnFailure} is false.
	 * @param messageContent message content
	 * @param personParameters list of parameters map
	 * @param haltOnFailure
	 * @throws QwertoMailerException Message is null
	 * , From email has not been specified
	 * , Recipients list is empty
	 * , Emails not present in file
	 * , Halt on failure (bad emails present)
	 * @throws Exception 
	 * @see #send(MessageContent, List) 
	 * 
	 */
	public void send(MessageContent messageContent, List<Map<String, String>> personParameters
			, boolean haltOnFailure)
	throws QwertoMailerException, Exception
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
		Velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
	      "org.apache.velocity.slf4j.Slf4jLogChute");
		Velocity.setProperty("runtime.log.logsystem.slf4j.name",
			"com.qwertovsky.mailer");
		Velocity.init();
		
		badEmails = new ArrayList<String>();
		badParametersMap = new ArrayList<Map<String, String>>(); 
		
		//create messages
		ArrayList<Message> messages = new ArrayList<Message>();
		
		logger.info("Create personal messages");
		
		for(Map<String, String> parameters:personParameters)
		{
			//get emails
			Set<InternetAddress> recipients = getRecipientsList(parameters);
			//error if list is empty
			if(recipients == null)
			{
				StringBuilder sb = new StringBuilder();
				Set<String> keys = parameters.keySet();
				for(String key:keys)
				{
					if(sb.length() > 0)
						sb.append(", ");
					sb.append("\"" + parameters.get(key) + "\"");
				}
				logger.error("Recipients list is empty: " + sb.toString());
				continue;
			}
			
			//get attachments
			List<File> attachments = getAttachments(parameters);
			
			//create individual message content
			MessageContent content = new MessageContent(messageContent);
			if(attachments != null && !attachments.isEmpty())
				content.addAttachments(attachments);
			try
			{
				content.setParameters(parameters);
			} catch (QwertoMailerException qme)
			{
				String errorMessage = "Message has not been created (" + qme.getMessage() + ") for: ";
				StringBuilder sb = new StringBuilder();
				Set<String> headers = parameters.keySet();
				for(String header:headers)
				{
					if(sb.length() > 0)
						sb.append(", ");
					sb.append("\"" + parameters.get(header) + "\"");
				}
				logger.error(errorMessage + sb.toString());
				badParametersMap.add(parameters);
				continue;
			}
			
			
			Message message = new Message(session);
			try
			{
				makeMessage(message, content);
				message.setRecipients(RecipientType.TO, recipients.toArray(new InternetAddress[0]));
			} catch (MessagingException e)
			{
				logger.error("Message has not been created for "
						+ recipients.toArray() + "("+e.getMessage()+")");
				throw e;
			}
			message.setParameters(parameters);
			messages.add(message);
		}
		
		//halt on failure
		if(haltOnFailure && (!badEmails.isEmpty() || !badParametersMap.isEmpty()))
			throw new QwertoMailerException("Halt on failure");
		
		//send messages
		logger.info("Start sending");
		notSentMessages = new ArrayList<Message>();
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
				sentMessages.add(message);
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
			notSentMessages.add(message);			
			logger.error("Error ("+ me.getMessage() +") send message to: " + sb.toString());
		}
		
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
	 * @param parameters array of parameters
	 * @return attachments list
	 */
	protected List<File> getAttachments(Map<String, String> parameters)
	{
		if(parameters == null || parameters.size() == 0)
			return null;
		
		List<File> attachments = new ArrayList<File>();
		Set<String> headers = parameters.keySet();
		for(String header:headers)
		{
			if(header.toLowerCase().trim().startsWith("attach"))
			{
				String fileString = parameters.get(header);
				if(fileString == null)
				{
					logger.warn("Attachment not present in column '" + header + "':");
					badParametersMap.add(parameters);
					continue;
				}
				fileString = fileString.trim();
				if(fileString.length() == 0)
				{
					logger.warn("Attachment not present in column '" + header + "':");
					badParametersMap.add(parameters);
					continue;
				}
				File file = new File(fileString);
				if(file.exists())
					attachments.add(file);
				else
				{
					logger.error("File " + fileString + " not exists");
					if(badParametersMap != null)
						badParametersMap.add(parameters);
				}
			}
		}
		return attachments;
	}

	//--------------------------------------------
	/**
	 * Get recipient list
	 * @param parameters array of parameters
	 * @return recipients addresses list
	 */
	protected Set<InternetAddress> getRecipientsList(Map<String, String> parameters)
	{
		if(parameters == null || parameters.size() == 0)
			return null;
		Set<InternetAddress> recipientsList = new HashSet<InternetAddress>();
		Set<String> keys = parameters.keySet();
		for(String header:keys)
		{
			if(header.toLowerCase().trim().startsWith("email"))
			{
				String emailString = parameters.get(header);
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
		}
		if(recipientsList == null || recipientsList.isEmpty())
		{
			return null;
		}
		return recipientsList;
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
	public List<Map<String, String>> getBadParameters()
	{
		return badParametersMap;
	}
	
	//--------------------------------------------
	/**
	 * Get not sent messages that were in the last send operation.
	 * @return not sent messages
	 */
	public List<Message> getNotSentMessages()
	{
		return notSentMessages;
	}
	
	//--------------------------------------------
	/**
	 * Get not sent messages that were in the last send operation.
	 * @return not sent messages
	 */
	public List<Message> getSentMessages()
	{
		return sentMessages;
	}
}
