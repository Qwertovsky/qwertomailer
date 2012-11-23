package com.qwertovsky.mailer;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.qwertovsky.mailer.errors.QwertoMailerException;


/**
 * @author Qwertovsky
 *
 */
@RunWith(value = Parameterized.class)
public class MessageContentSSSSTest
{
	String content;
	String contentType;
	String subject;
	String charset;
	
	final static String BAD_CONTENT_TYPE = "test";
	
	@Parameters
	public static Collection<Object[]> parameters() throws AddressException
	{
		String validAddress1 = "address1";
		String validAddress2 = "address2";
		Address address1 = new InternetAddress(validAddress1);
		Address address2 = new InternetAddress(validAddress2);
		ArrayList<Address> to = new ArrayList<Address>();
		to.add(address1);
		to.add(address2);
		
		return Arrays.asList(new Object[][] { 
				{null, null, null, null} //Bad content
				, {"", null, null, null} //Bad content
				, {"text (текст)", null, null, null} //Bad subject
				, {"text (текст)", null, "", null} //Bad subject
				, {"text (текст)", null, "subject (тема)", null} //Bad ContentType
				, {"text (текст)", "", "subject (тема)", null} //Bad ContentType
				, {"text (текст)", BAD_CONTENT_TYPE, "subject (тема)", null} //Bad ContentType
				,
				{"text (текст)", "text/plain", "subject (тема)", null} 
				, {"text (текст)", "text/plain", "subject (тема)", "windows-1251"} 
				, {"<b>text (текст)</b>", "text/html", "subject (тема)", "windows-1251"}
				
				//utf-8 will be replaced by windows-1251
				, {"text (текст)", "text/plain; charset=utf-8", "subject (тема)", "windows-1251"} 
				
				}); 
	}
	
	public MessageContentSSSSTest(String content, String contentType, String subject
			, String charset)
	{
		this.content = content;
		this.contentType = contentType;
		this.subject = subject;
		this.charset = charset;
	}
	
	@Test
	public void testMessageContentStringStringStringString()
	{
		
		try
		{
			MessageContent message = new MessageContent(content, contentType, subject, charset);
			if(!message.getContent().equals(content))
				fail("incorrect content");
			
			//get baseType
			//if "text/plain; charset=utf-8" - result is "text/plain; charset=windows-1251"
			//baseType is "text/plain"
			contentType = contentType.split(";")[0];
			if(!message.getContentType()
					.equalsIgnoreCase(contentType + "; charset=" + (charset==null?"utf-8":charset)))
				fail("incorrect contentType");
			if(!message.getSubject().equals(subject))
				fail("incorrect subject");
			if(!message.getCharset().equalsIgnoreCase((charset==null?"utf-8":charset)))
				fail("incorrect charset");
			
		} catch (QwertoMailerException qme)
		{
			//pass
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
