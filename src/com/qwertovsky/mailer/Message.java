package com.qwertovsky.mailer;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class Message extends MimeMessage
{
	public Message(Session session)
	{
		super(session);
	}

	protected void updateMessageID() throws MessagingException
	{
		String host = session.getProperty("mail.smtp.host");
		String messageId = this.hashCode() + "." + System.currentTimeMillis();
		if(host != null)
			messageId = messageId + "@" + host;
		setHeader("Message-ID", "<"	+ messageId	+ ">");
	}
}
