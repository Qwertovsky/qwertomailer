package com.qwertovsky.mailer;

import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * @author Qwertovsky
 *
 */
class Message extends MimeMessage
{
	private Map<String, String> parameters;
		
	public Message(Session session)
	{
		super(session);
	}

	protected void updateMessageID() throws MessagingException
	{
		String[] messageIdHeaders = getHeader("Message-ID");
		if(messageIdHeaders !=null && messageIdHeaders.length > 0)
			return;
		String host = session.getProperty("mail.smtp.host");
		String messageId = System.currentTimeMillis() + "." + this.hashCode();
		if(host != null)
			messageId = messageId + "@" + host;
		setHeader("Message-ID", "<"	+ messageId	+ ">");
	}
	
	//--------------------------------------------
	public void setParameters(Map<String, String> parametersMap)
	{
		parameters = parametersMap;
	}
	
	//--------------------------------------------
	public void setParameters(String[] headers, String[] parametersArray)
	{
		parameters = new HashMap<String, String>();
		for(int i = 0; i < headers.length; i++)
		{
			parameters.put(headers[i], parametersArray[i]);
		}
	}
	
	//--------------------------------------------
	public Map<String, String> getParameters()
	{
		return parameters;
	}
}
