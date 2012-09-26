package com.qwertovsky.mailer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;
import org.subethamail.wiser.Wiser;

public class MailerTest
{
	static Wiser wiser;
	String[] args;
	public static Logger loggerOrg = Logger.getLogger("org");
	public static Logger loggerCom = Logger.getLogger("com");
	
	
	
	public MailerTest()
	{
		String pattern = "[%d{yyyy-MM-dd HH:mm:ss} %-4r][%-5p] %m%n";
	    PatternLayout layout = new PatternLayout(pattern);
		ConsoleAppender appender = new ConsoleAppender(layout);
		loggerOrg.addAppender(appender);
		loggerCom.addAppender(appender);
		loggerOrg.setLevel(Level.WARN);
		loggerCom.setLevel(Level.WARN);
		
		wiser = new Wiser(2500);
		wiser.start();
	}
	
	@Test
	public void testMain() throws MessagingException, IOException
	{
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "test message"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		MimeMessage message = wiser.getMessages().get(0).getMimeMessage();
		String content = (String) message.getContent();
		String contentType;
		assertEquals("test message", content.trim());
		
		//--------------------
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test message</html>"
				,"-contentType", "text/html"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		content = (String) message.getContent();
		contentType = message.getContentType();
		assertEquals("<html>test message</html>", content.trim());
		assertEquals("text/html; charset=UTF-8", contentType);
		
		//--------------------
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test message</html>"
				,"-contentType", "text/html"
				,"-charset", "windows-1251"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		content = (String) message.getContent();
		contentType = message.getContentType();
		assertEquals("<html>test message</html>", content.trim());
		assertEquals("text/html; charset=windows-1251", contentType);
		
		//--------------------
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test message</html>"
				,"-contentType", "text/html"
				,"-alttext", "text message"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		contentType = message.getContentType();
		if(!contentType.startsWith("multipart/alternative"))
			fail("incorrect Mailer (-alttext)");
		
		//--------------------
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test message</html>"
				,"-contentType", "text/html"
				,"-attach", "test.png"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		contentType = message.getContentType();
		if(!contentType.startsWith("multipart/mixed"))
			fail("incorrect Mailer (-attach)");
		
		//--------------------
		//test attach file with cyrillic name
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test message</html>"
				,"-contentType", "text/html"
				,"-attach", "тест.png"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com","-trace"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		contentType = message.getContentType();
		if(!contentType.startsWith("multipart/mixed"))
			fail("incorrect Mailer (-attach)");
		Multipart body = (Multipart) message.getContent();
		BodyPart attach = body.getBodyPart(1);
		if(!MimeUtility.decodeText(attach.getFileName()).equalsIgnoreCase("тест.png"))
			fail("incorrect attach file name");
		
		//--------------------
		//test attach files from list
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test message</html>"
				,"-contentType", "text/html"
				,"-attachFile", "attach_list"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com","-trace"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		contentType = message.getContentType();
		if(!contentType.startsWith("multipart/mixed"))
			fail("incorrect Mailer (-attach)");
		body = (Multipart) message.getContent();
		attach = body.getBodyPart(1);
		if(!MimeUtility.decodeText(attach.getFileName()).equalsIgnoreCase("test.png"))
			fail("incorrect attach file name");
		attach = body.getBodyPart(2);
		if(!MimeUtility.decodeText(attach.getFileName()).equalsIgnoreCase("тест.png"))
			fail("incorrect attach file name");
		
		//--------------------
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "<html>test $parameter<img src='test.png' /> <br>related message</html>"
				,"-contentType", "text/html"
				,"-related"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 1)
			fail("incorrect Mailer");
		message = wiser.getMessages().get(0).getMimeMessage();
		contentType = message.getContentType();
		if(!contentType.startsWith("multipart/related"))
			fail("incorrect Mailer (-related)");
		
		//--------------------
		wiser.getMessages().clear();
		Mailer.main(new String[]{"-smtpHost", "localhost", "-smtpPort", "2500" 
				,"-subject", "test subject" 
				,"-body", "test message"
				,"-emailFrom", "qwertovsky@gmail.com"
				,"-emailTo", "qwertovsky@gmail.com,qwertovsky@gmail.com"
				});
		if(wiser.getMessages().size() != 2)
			fail("incorrect Mailer");
		
		
	}

}
