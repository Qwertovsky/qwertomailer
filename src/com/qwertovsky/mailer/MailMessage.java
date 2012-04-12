package com.qwertovsky.mailer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

/**
 * Message with content, content type, charset
 * , contentTransferEncoding, subject and sender address
 * @author Qwertovsky
 *
 */
public class MailMessage
{
	private Object content = null;
	private String contentType = null;
	private String subject = null;
	private Address addressFrom = null;
	private String charset = "UTF-8";
	private String contentTransferEncoding = "8bit";
	
	/**
	 * Create message from EML file.<br />
	 * Only content, content type and subject will be get from file.<br />
	 * Not allow create message with no subject or with empty subject. 
	 * @param EMLFile - file contains message in EML file format
	 * @throws Exception EML file not exists, EML file is null, Bad subject
	 * @throws IOException can't read file
	 * @throws MessagingException
	 */
	public MailMessage(File EMLFile) throws Exception
	{
		Session mailSession = Session.getDefaultInstance(new Properties(), null);
		try
		{
			InputStream isEML = null;
			if(EMLFile == null)
				throw new Exception("EML file is null");
			isEML = new FileInputStream(EMLFile);
			Message message = new MimeMessage(mailSession, isEML);
			content = message.getContent();
			contentType = message.getContentType();
			subject = message.getSubject();
			if(subject == null || subject.equals(""))
				throw new Exception("Bad subject");
			//get charset
			ContentType ct = new ContentType(contentType);
			charset = ct.getParameter("charset");
			if(charset == null)
				charset = "UTF-8";
			

		} catch (FileNotFoundException e)
		{
			throw new Exception("EML file not exists");
		} catch (MessagingException e)
		{
			throw e;
		} catch (IOException e)
		{
			throw e;
		}
	}
	//--------------------------------------------
	/**
	 * Create message.<br />
	 * Not allow create message with no subject or with empty subject
	 * @param content
	 * @param contentType - Mime type of content
	 * @param charset - encoding of content (default is "utf-8")
	 * @throws Exception if content is null or content is empty text line
	 * 	, rise exception with message "Bad content"
	 *  <br />- if content type is null or content type is empty text line, message "Bad ContentType"
	 *  <br />- if subject is null or subject is empty text line, message "Bad subject"
	 * @throws MessagingException
	 */
	public MailMessage(String content, String contentType, String subject, String charset) throws Exception
	{
		//not allow content with no body parts
		if(content == null || content.equals(""))
			throw new Exception("Bad content");
		this.content = content;
		
		//not allow message with no subject
		if(subject == null || subject.equals(""))
			throw new Exception("Bad subject");
		this.subject = subject;
		
		//set default charset
		if(charset == null || charset.length() == 0)
			charset = "utf-8";
		this.charset = charset;
		
		if(contentType == null || contentType.length() == 0)
			throw new Exception("Bad ContentType");
		try
		{
			ContentType ct = new ContentType(contentType);
			this.contentType = ct.getBaseType() + "; charset=" + charset;
		}catch(ParseException pe)
		{
			throw new Exception("Bad ContentType: "+ pe.getMessage());
		}
		
		
	}
	//--------------------------------------------
	/**
	 * Set subject
	 * <br />Not allow null subject or empty subject
	 * @param subject
	 * @throws Exception "Bad subject" (null subject or empty subject)
	 */
	public void setSubject(String subject) throws Exception
	{
		if(subject == null || subject.equals(""))
			throw new Exception("Bad subject");
		this.subject = subject;
	}
	//--------------------------------------------
	/**
	 * Specify address in field From:
	 * @param person - display name
	 * @param email - email address
	 * @param charset - encoding
	 * @throws Exception "Bad email in FROM" (email is null or email is empty)
	 * @throws UnsupportedEncodingException
	 */
	public void setAddressFrom(String person, String email, String charset) throws Exception
	{
		if(email == null || email.length() == 0)
			throw new Exception ("Bad email in FROM");
		addressFrom = new InternetAddress(email, person, charset);
	}
	//--------------------------------------------
	/**
	 * Add files to message
	 * @param attachments - list of files
	 * @throws MessagingException
	 */
	public void addAttachments(List<File> attachments) throws MessagingException
	{
		if(content instanceof Multipart
				&& ((Multipart)content).getContentType().startsWith("multipart/mixed"))
		{
			//add attachments to end
			MimeBodyPart bodyPart = null;
			for(File attachment:attachments)
			{
				bodyPart = new MimeBodyPart();
				DataSource source = new FileDataSource(attachment);
				bodyPart.setDataHandler(new DataHandler(source));
				bodyPart.setFileName(attachment.getName());
				((Multipart)content).addBodyPart(bodyPart);
			}
		}
		else 
		{
			//create part with type multipart/mixed
			Multipart multipart = new MimeMultipart();
			//add old content
			if(content instanceof Multipart
					&& ((Multipart)content).getContentType().startsWith("multipart/alternative"))
			{
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(content,contentType);
				multipart.addBodyPart(bodyPart);
			}
			else
			{
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(content,contentType);
				bodyPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				multipart.addBodyPart(bodyPart);
			}
			//add attachments to end of message
			MimeBodyPart bodyPart = null;
			for(File attachment:attachments)
			{
				bodyPart = new MimeBodyPart();
				DataSource source = new FileDataSource(attachment);
				bodyPart.setDataHandler(new DataHandler(source));
				bodyPart.setFileName(attachment.getName());
				multipart.addBodyPart(bodyPart);
			}
			content = multipart;
			contentType = ((Multipart)content).getContentType();
		}
	}
	//--------------------------------------------
	/**
	 * Attach file to message
	 * @param attachment
	 * @throws MessagingException
	 */
	public void addAttachment(File attachment) throws MessagingException
	{
		List<File> attachments = new ArrayList<File>(1);
		attachments.add(attachment);
		addAttachments(attachments);
	}
	//--------------------------------------------
	/**
	 * Add alternative text for email clients, that do not support HTML message
	 * @param text - plain text
	 * @param charset - encoding
	 * @throws MessagingException
	 * @throws IOException
	 */
	public void setAlternativeText(String text, String charset) throws MessagingException, IOException
	{
		/*
		 * Message have the following format:
		 * without attachment:
		 * 		(-body (multipart/related or text)
		 * 		or
		 * 		-multipart/alternative
		 * 			-alternative text
		 * 			-body (multipart/related or text)
		 * 		)
		 * with attachment:
		 * 		(-multipart/mixed
		 * 			(-body (multipart/related or text)
		 * 			or
		 * 			-multipart/alternative
		 * 				-alternative text
		 * 				-body (multipart/related or text)
		 * 			)
		 * 			-attachment
		 * 		)
		 * 
		 * If message is mixed, the first is the body then attachments.
		 * If body is with alternative text, the first is alternative text then main body
		 */	
		if(content instanceof Multipart
				&& ((Multipart)content).getContentType().startsWith("multipart/mixed"))
		{
			Multipart mixed = ((Multipart)content);
			//get first part: body or multipart/alternative
			Object body = mixed.getBodyPart(0).getContent();
			if(body instanceof Multipart
					&& ((Multipart)body).getContentType().startsWith("multipart/alternative"))
			{
				//create new alternative
				Multipart multipart = new MimeMultipart("alternative");
				//create new plain part
				MimeBodyPart plain = new MimeBodyPart();
				plain.setText(text, charset);
				plain.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				multipart.addBodyPart(plain);
				//add main body part
				multipart.addBodyPart(((Multipart)body).getBodyPart(1));
				//delete old alternative - the first part of mixed message
				((Multipart)content).removeBodyPart(0);
				//add new alternative in first position before attachments
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(multipart);
				((Multipart)content).addBodyPart(bodyPart, 0);
			}
			else //body with no alternative text
			{
				//create new alternative
				Multipart multipartAlt = new MimeMultipart("alternative");
				//add plain alternative text
				MimeBodyPart plain = new MimeBodyPart();
				plain.setText(text, charset);
				plain.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				multipartAlt.addBodyPart(plain);
				//add main body part
				multipartAlt.addBodyPart(((Multipart)content).getBodyPart(0));
				//replace main body of message to multipart/alternative body
				mixed.removeBodyPart(0);
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(multipartAlt, "multipart/alternative");
				mixed.addBodyPart(bodyPart, 0);
				content = mixed;
			}
		}
		else if (content instanceof Multipart
				&& ((Multipart)content).getContentType().startsWith("multipart/alternative"))
		{
			//replace old alternative text to new
			MimeBodyPart plain = new MimeBodyPart();
			plain.setText(text, charset);
			plain.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
			((Multipart)content).removeBodyPart(0);
			((Multipart)content).addBodyPart(plain, 0);
		}
		else //message with no attachments and body with no alternative text 
		{
			//create new multipart/alternative message
			Multipart multipart = new MimeMultipart("alternative");
			MimeBodyPart plain = new MimeBodyPart();
			plain.setText(text, charset);
			plain.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
			multipart.addBodyPart(plain);
			MimeBodyPart bodyPart = new MimeBodyPart();
			bodyPart.setContent(content, contentType);
			multipart.addBodyPart(bodyPart);
			content = multipart;
			contentType = ((Multipart)content).getContentType();
		}
			
	}
	//--------------------------------------------
	public void setContentTransferEncoding(String contentTransferEncoding)
	{
		if(contentTransferEncoding != null && contentTransferEncoding.length() != 0)
			this.contentTransferEncoding = contentTransferEncoding;
	}
	//--------------------------------------------
	public String getCharset()
	{
		return charset;
	}
	//--------------------------------------------
	public Object getContent()
	{
		return content;
	}
	//--------------------------------------------
	public String getContentType()
	{
		return contentType;
	}
	//--------------------------------------------
	public String getSubject()
	{
		return subject;
	}
	//--------------------------------------------
	public Address getAddressFrom()
	{
		return addressFrom;
	}
	//--------------------------------------------
	public String getContentTransferEncoding()
	{
		return contentTransferEncoding;
	}
}
