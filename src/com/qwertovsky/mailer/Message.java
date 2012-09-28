package com.qwertovsky.mailer;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * @author Qwertovsky
 *
 */
class Message extends MimeMessage
{
	private String[] parameters;
	
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
	public void setParameters(String[] parameters)
	{
		this.parameters = parameters;
	}

	public String[] getParameters()
	{
		return parameters;
	}
}
