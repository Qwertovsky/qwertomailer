package com.qwertovsky.mailer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
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
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

/**
 * Message with content, content type, charset
 * , contentTransferEncoding, subject and sender address
 * @author Qwertovsky
 *
 */
public class MessageContent
{
	private Object content = null;
	private String contentType = null;
	private String subject = null;
	private Address addressFrom = null;
	private String charset = "UTF-8";
	private String contentTransferEncoding = "8bit";
	
	//--------------------------------------------
	/**
	 * Create copy of MessageContent object
	 * @param messageContent source object
	 * @throws MessagingException
	 */
	public MessageContent (MessageContent messageContent) throws MessagingException
	{
		Object newContent = messageContent.getContent();
		if(newContent instanceof String)
			this.content = new String((String) newContent);
		else if(newContent instanceof Multipart)
		{
			ContentType ct = new ContentType(((Multipart)newContent).getContentType());
			String subType = ct.getSubType();
			Multipart multipart = new MimeMultipart(subType);
			//add parts
			for(int i=0; i < ((Multipart)newContent).getCount();i++)
			{
				multipart.addBodyPart(((Multipart)newContent).getBodyPart(i));
			}
			this.content = multipart;
		}
		InternetAddress address = (InternetAddress) messageContent.getAddressFrom();
		try
		{
			this.addressFrom = new InternetAddress(address.getAddress(), address.getPersonal());
		} catch (UnsupportedEncodingException e)
		{
			// never
		}
		this.charset = new String(messageContent.getCharset());
		this.contentType = new String(messageContent.getContentType());
		this.contentTransferEncoding = new String(messageContent.getContentTransferEncoding());
		
	}
	
	//--------------------------------------------
	/**
	 * Create message from EML file.<br />
	 * Only content, content type and subject will be get from file.<br />
	 * Not allow create message with no subject or with empty subject. 
	 * @param EMLFile file contains message in EML file format
	 * @throws Exception EML file not exists, EML file is null, Bad subject
	 * @throws IOException can't read file
	 * @throws MessagingException
	 */
	public MessageContent(File EMLFile) throws Exception
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
	 * @param contentType Mime type of content
	 * @param charset encoding of content (default is "utf-8")
	 * @throws Exception if content is null or content is empty text line
	 * 	, rise exception with message "Bad content"
	 *  <br />- if content type is null or content type is empty text line, message "Bad ContentType"
	 *  <br />- if subject is null or subject is empty text line, message "Bad subject"
	 * @throws MessagingException
	 */
	public MessageContent(String content, String contentType, String subject, String charset) throws Exception
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
	 * @param attachments list of files
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
				bodyPart.setHeader("Content-Type", contentType);
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
	 * Add inline attachment to message<br />
	 * Change html content to multipart/related.<br />
	 * Replace path in 'src'-attribute to "cid:content-id"<br />
	 * Return &lt;content-id&gt; 
	 * @param path file path from image attribute 'src' (&lt;img src="<b>path</b>" /&gt;) 
	 * @return Content-ID of attachment with &lt; &gt;
	 * @throws MessagingException
	 * @throws IOException
	 * @throws IOException Inline image url is null or empty
	 */
	protected String addInlineAttachment(String path) throws MessagingException, IOException, Exception
	{
		if(path == null || path.length() == 0)
			throw new Exception("Inline image url is incorrect");
		if(content instanceof Multipart
				&& ((Multipart)content).getContentType().startsWith("multipart/mixed"))
		{
			//get and replace html part of message
			Object body = ((Multipart)content).getBodyPart(0).getContent();
			if(body instanceof Multipart
					&& ((Multipart)body).getContentType().startsWith("multipart/alternative"))
			{
				//get main part (html or related)
				Object mainPart = ((Multipart)body).getBodyPart(1).getContent();
				if(mainPart instanceof Multipart)
				{
					/*
					 * -mixed
					 * \-alternative (body)
					 * 		\-text
					 * 		|-related (mainPart)
					 * 			\-html
					 * 			|-attachment
					 * |-attachments
					 */
					
					//add attachment
					MimeBodyPart attachPart = new MimeBodyPart();
					File attachment = new File(path);
					DataSource source = new FileDataSource(attachment);
					attachPart.setDataHandler(new DataHandler(source));
					attachPart.setFileName(attachment.getName());
					attachPart.setDisposition(MimeBodyPart.INLINE);
					InputStream is = new BufferedInputStream(new FileInputStream(attachment));
					String mimeType = URLConnection.guessContentTypeFromStream(is);
					attachPart.setHeader("Content-Type",mimeType);
					attachPart.setContentID("<" + attachment.getName()
							+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
					((Multipart)mainPart).addBodyPart(attachPart);
					//replace html
					String html = (String)  ((Multipart)mainPart).getBodyPart(0).getContent();
					String cid = attachPart.getContentID();
					cid = cid.substring(1, cid.length() - 1);
					html = html.replace(path, "cid:" + cid);
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)mainPart).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)mainPart).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					((Multipart)mainPart).removeBodyPart(0);
					((Multipart)mainPart).addBodyPart(htmlPart, 0);
					//replace related
					MimeBodyPart bodyPart = new MimeBodyPart();
					bodyPart.setContent(((Multipart)mainPart));
					((Multipart)body).removeBodyPart(1);
					((Multipart)body).addBodyPart(bodyPart);
					
					//replace body
					((Multipart)content).removeBodyPart(0);
					bodyPart = new MimeBodyPart();
					bodyPart.setContent(((Multipart)body));
					((Multipart)content).addBodyPart(bodyPart, 0);
					
					return attachPart.getContentID();
				}
				else
				{
					/*
					 * -mixed
					 * \-alternative (body)
					 * 		\-text
					 * 		|-html (will be changed to related)
					 * |-attachments
					 */
					//create new alternative
					Multipart alternative = new MimeMultipart("alternative");
					//add palin part
					alternative.addBodyPart(((Multipart)body).getBodyPart(0));
					
					//get and replace html part to multipart/related
					//create multipart/related
					Multipart related = new MimeMultipart("related");
					//add attachment
					MimeBodyPart attachPart = new MimeBodyPart();
					File attachment = new File(path);
					DataSource source = new FileDataSource(attachment);
					attachPart.setDataHandler(new DataHandler(source));
					attachPart.setFileName(attachment.getName());
					attachPart.setDisposition(MimeBodyPart.INLINE);
					InputStream is = new BufferedInputStream(new FileInputStream(attachment));
					String mimeType = URLConnection.guessContentTypeFromStream(is);
					attachPart.setHeader("Content-Type",mimeType);
					attachPart.setContentID("<" + attachment.getName()
							+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
					related.addBodyPart(attachPart);
					//add html
					String html = (String)  ((Multipart)body).getBodyPart(1).getContent();
					String cid = attachPart.getContentID();
					cid = cid.substring(1, cid.length() - 1);
					html = html.replace(path, "cid:" + cid);
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)body).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)body).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					related.addBodyPart(htmlPart, 0);
					//add related to alternative
					MimeBodyPart bodyPart = new MimeBodyPart();
					bodyPart.setContent(related);
					alternative.addBodyPart(bodyPart);
					
					//replace body
					((Multipart)content).removeBodyPart(0);
					bodyPart = new MimeBodyPart();
					bodyPart.setContent(alternative);
					((Multipart)content).addBodyPart(bodyPart, 0);
					
					return attachPart.getContentID();
				}
			}
			else if(body instanceof Multipart
					&& ((Multipart)body).getContentType().startsWith("multipart/related"))
			{
				/*
				 * -mixed
				 * \-related (body)
				 * 		\-html
				 * 		|-attachment 
				 * |-attachments
				 */
				//add attachment
				MimeBodyPart attachPart = new MimeBodyPart();
				File attachment = new File(path);
				DataSource source = new FileDataSource(attachment);
				attachPart.setDataHandler(new DataHandler(source));
				attachPart.setFileName(attachment.getName());
				attachPart.setDisposition(MimeBodyPart.INLINE);
				InputStream is = new BufferedInputStream(new FileInputStream(attachment));
				String mimeType = URLConnection.guessContentTypeFromStream(is);
				attachPart.setHeader("Content-Type",mimeType);
				attachPart.setContentID("<" + attachment.getName()
						+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
				((Multipart)body).addBodyPart(attachPart);
				//replace html
				String html = (String) ((Multipart)body).getBodyPart(0).getContent();;
				String cid = attachPart.getContentID();
				cid = cid.substring(1, cid.length() - 1);
				html = html.replace(path, "cid:" + cid);
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, ((Multipart)body).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Type", ((Multipart)body).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				((Multipart)body).removeBodyPart(0);
				((Multipart)body).addBodyPart(htmlPart, 0);
				
				//replace body
				((Multipart)content).removeBodyPart(0);
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(((Multipart)body));
				((Multipart)content).addBodyPart(bodyPart, 0);
				
				return attachPart.getContentID();
			}
			else
			{
				/*
				 * -mixed
				 * \-html (body)(will be changed to related)
				 * |-attachments
				 */
				//create multipart/related
				Multipart related = new MimeMultipart("related");
				//add attachment
				MimeBodyPart attachPart = new MimeBodyPart();
				File attachment = new File(path);
				DataSource source = new FileDataSource(attachment);
				attachPart.setDataHandler(new DataHandler(source));
				attachPart.setFileName(attachment.getName());
				attachPart.setDisposition(MimeBodyPart.INLINE);
				InputStream is = new BufferedInputStream(new FileInputStream(attachment));
				String mimeType = URLConnection.guessContentTypeFromStream(is);
				attachPart.setHeader("Content-Type",mimeType);
				attachPart.setContentID("<" + attachment.getName()
						+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
				related.addBodyPart(attachPart);
				//add html
				String html = (String) ((Multipart)content).getBodyPart(0).getContent();
				String cid = attachPart.getContentID();
				cid = cid.substring(1, cid.length() - 1);
				html = html.replace(path, "cid:" + cid);
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Type", ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				related.addBodyPart(htmlPart, 0);
				
				//replace body
				((Multipart)content).removeBodyPart(0);
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(related);
				((Multipart)content).addBodyPart(bodyPart, 0);
				
				return attachPart.getContentID();
			}
		}
		else 
		{
			if(content instanceof Multipart
					&& ((Multipart)content).getContentType().startsWith("multipart/alternative"))
			{
				//get and replace html part of message
				Object body = ((Multipart)content).getBodyPart(1).getContent();
				if(body instanceof Multipart)
				{
					/*
					 * -alternative (content)
					 * \-text
					 * |-related (body)
					 * 		\-html
					 * 		|-attachment
					 */
					//add attachment
					MimeBodyPart attachPart = new MimeBodyPart();
					File attachment = new File(path);
					DataSource source = new FileDataSource(attachment);
					attachPart.setDataHandler(new DataHandler(source));
					attachPart.setFileName(attachment.getName());
					attachPart.setDisposition(MimeBodyPart.INLINE);
					InputStream is = new BufferedInputStream(new FileInputStream(attachment));
					String mimeType = URLConnection.guessContentTypeFromStream(is);
					attachPart.setHeader("Content-Type",mimeType);
					attachPart.setContentID("<" + attachment.getName()
							+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
					((Multipart)body).addBodyPart(attachPart);
					//replace html
					String html = (String) ((Multipart)body).getBodyPart(0).getContent();
					String cid = attachPart.getContentID();
					cid = cid.substring(1, cid.length() - 1);
					html = html.replace(path, "cid:" + cid);
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)body).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)body).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					((Multipart)body).removeBodyPart(0);
					((Multipart)body).addBodyPart(htmlPart, 0);
					//replace related
					((Multipart)content).removeBodyPart(1);
					MimeBodyPart relatedPart = new MimeBodyPart();
					relatedPart.setContent((Multipart) body);
					((Multipart)content).addBodyPart(relatedPart, 1);
					
					return attachPart.getContentID();
				}
				else
				{
					/*
					 * -alternative (content)
					 * \-text
					 * |-html
					 */
					//get and replace html part to multipart/related
					//create multipart/related
					Multipart related = new MimeMultipart("related");
					//add attachment
					MimeBodyPart attachPart = new MimeBodyPart();
					File attachment = new File(path);
					DataSource source = new FileDataSource(attachment);
					attachPart.setDataHandler(new DataHandler(source));
					attachPart.setFileName(attachment.getName());
					attachPart.setDisposition(MimeBodyPart.INLINE);
					InputStream is = new BufferedInputStream(new FileInputStream(attachment));
					String mimeType = URLConnection.guessContentTypeFromStream(is);
					attachPart.setHeader("Content-Type",mimeType);
					attachPart.setContentID("<" + attachment.getName()
							+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
					related.addBodyPart(attachPart);
					//add html
					String html = (String) ((Multipart)content).getBodyPart(1).getContent();
					String cid = attachPart.getContentID();
					cid = cid.substring(1, cid.length() - 1);
					html = html.replace(path, "cid:" + cid);
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)content).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)content).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					related.addBodyPart(htmlPart, 0);
					
					//replace html part
					((Multipart)content).removeBodyPart(1);
					MimeBodyPart bodyPart = new MimeBodyPart();
					bodyPart.setContent(related);
					((Multipart)content).addBodyPart(bodyPart);
					
					return attachPart.getContentID();
				}
			}
			else if(content instanceof Multipart
					&& ((Multipart)content).getContentType().startsWith("multipart/related"))
			{
				/*
				 * -related (content)
				 * \-html
				 * |-attachment
				 */
				//add attachment
				MimeBodyPart attachPart = new MimeBodyPart();
				File attachment = new File(path);
				DataSource source = new FileDataSource(attachment);
				attachPart.setDataHandler(new DataHandler(source));
				attachPart.setFileName(attachment.getName());
				attachPart.setDisposition(MimeBodyPart.INLINE);
				InputStream is = new BufferedInputStream(new FileInputStream(attachment));
				String mimeType = URLConnection.guessContentTypeFromStream(is);
				attachPart.setHeader("Content-Type",mimeType);
				attachPart.setContentID("<" + attachment.getName()
						+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
				((Multipart)content).addBodyPart(attachPart);
				//replace html
				String html = (String) ((Multipart)content).getBodyPart(0).getContent();
				String cid = attachPart.getContentID();
				cid = cid.substring(1, cid.length() - 1);
				html = html.replace(path, "cid:" + cid);
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Type", ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				((Multipart)content).removeBodyPart(0);
				((Multipart)content).addBodyPart(htmlPart, 0);
				
				return attachPart.getContentID();
			}
			else 
			{
				/*
				 * -html (content)
				 */
				//create multipart/related
				Multipart related = new MimeMultipart("related");
				//add attachment
				MimeBodyPart attachPart = new MimeBodyPart();
				File attachment = new File(path);
				DataSource source = new FileDataSource(attachment);
				attachPart.setDataHandler(new DataHandler(source));
				attachPart.setFileName(attachment.getName());
				attachPart.setDisposition(MimeBodyPart.INLINE);
				InputStream is = new BufferedInputStream(new FileInputStream(attachment));
				String mimeType = URLConnection.guessContentTypeFromStream(is);
				attachPart.setHeader("Content-Type",mimeType);
				attachPart.setContentID("<" + attachment.getName()
						+ "." + attachPart.hashCode() +"."+ System.currentTimeMillis() +">");
				related.addBodyPart(attachPart);
				//add html
				String html = (String) content;
				String cid = attachPart.getContentID();
				cid = cid.substring(1, cid.length() - 1);
				html = html.replace(path, "cid:" + cid);
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, contentType);
				htmlPart.setHeader("Content-Type", contentType);
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				related.addBodyPart(htmlPart, 0);
				
				content = related;
				contentType = related.getContentType();
				return attachPart.getContentID();
			}
		}
			
	}
	//--------------------------------------------
	/**
	 * Change html part of message to multipart/related part.
	 * <br />Files must be available by path, that is specified in attribute SRC in tag IMG.
	 * @return list of file path, that was specified in html part
	 * @throws Exception 
	 * @throws IOException 
	 * @throws MessagingException 
	 */
	public List<String> setRelated() throws MessagingException, IOException, Exception
	{
		List<String> paths = null;
		//get html part content
		String html = null;
		if(content instanceof Multipart
				&& ((Multipart)content).getContentType().startsWith("multipart/mixed"))
		{
			Object body = ((Multipart)content).getBodyPart(0).getContent();
			if(body instanceof Multipart
					&& ((Multipart)body).getContentType().startsWith("multipart/alternative"))
			{
				/*
				 * -alternative (body)
				 * 	\-text
				 * 	|
				 */
				Object mainPart = ((Multipart)body).getBodyPart(1).getContent();
				if(mainPart instanceof Multipart
						&& ((Multipart)mainPart).getContentType().startsWith("multipart/related"))
				{
					/*
					 * -alternative (body)
					 * 	\-text
					 * 	|-related (mainPart)
					 * 		\-html
					 * 		|-attachments
					 */
					html = (String) ((Multipart)mainPart).getBodyPart(0).getContent();
				}
				else
				{
					/*
					 * -alternative (content)
					 * 	\-text
					 * 	|-html (mainPart)
					 */
					html = (String) mainPart;
				}
			}
			else
			{
				if(body instanceof Multipart
						&& ((Multipart)body).getContentType().startsWith("multipart/related"))
				{
					/*
					 * -related (body)
					 * 	\-html
					 * 	|-attachments
					 */
					html = (String) ((Multipart)body).getBodyPart(0).getContent();
				}
				else
				{
					/*
					 * -html (body)
					 */
					html = (String) body;
				}
			}
		}
		else
		{
			if(content instanceof Multipart
					&& ((Multipart)content).getContentType().startsWith("multipart/alternative"))
			{
				/*
				 * -alternative (content)
				 * 	\-text
				 * 	|
				 */
				Object mainPart = ((Multipart)content).getBodyPart(1).getContent();
				if(mainPart instanceof Multipart
						&& ((Multipart)mainPart).getContentType().startsWith("multipart/related"))
				{
					/*
					 * -alternative (content)
					 * 	\-text
					 * 	|-related (mainPart)
					 * 		\-html
					 * 		|-attachments
					 */
					html = (String) ((Multipart)mainPart).getBodyPart(0).getContent();
				}
				else
				{
					/*
					 * -alternative (content)
					 * 	\-text
					 * 	|-html
					 */
					html = (String) mainPart;
				}
			}
			else
			{
				if(content instanceof Multipart
						&& ((Multipart)content).getContentType().startsWith("multipart/related"))
				{
					/*
					 * -related (content)
					 * 	\-html
					 * 	|-attachments
					 */
					html = (String) ((Multipart)content).getBodyPart(0).getContent();
				}
				else
				{
					/*
					 * -html (content)
					 */
					html = (String) content;
				}
			}
		}
		
		//get all file paths from html part
		ParserDelegator htmlParser = new ParserDelegator();
		ImageCallback callback = new ImageCallback();
		StringReader reader = new StringReader(html);
		htmlParser.parse(reader, callback, true);
		paths = callback.getPathList();

		//create multipart/related part
		for(String path:paths)
		{
			addInlineAttachment(path);
		}
		return paths;
	}
	//--------------------------------------------
	/**
	 * Add alternative text for email clients, that do not support HTML message
	 * @param text plain text
	 * @param charset encoding
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

	//--------------------------------------------
	/**
	 * Put inline parameters in message
	 * @param headers array of parameter's headers
	 * @param parameters array of parameters
	 * @throws IOException
	 * @throws MessagingException
	 * @throws org.apache.velocity.runtime.parser.ParseException
	 */
	public void setParameters(String[] headers, String[] parameters)
		throws IOException, MessagingException, org.apache.velocity.runtime.parser.ParseException
	{
		VelocityContext context = new VelocityContext();
		for(int i=0; i < headers.length; i++)
		{	
			if(headers[i] == null || headers[i].length() == 0)
				continue;
			context.put(headers[i], parameters[i]);
		}
		
		if(content instanceof Multipart
				&& ((Multipart)content).getContentType().startsWith("multipart/mixed"))
		{
			//get and replace html part of message
			Object body = ((Multipart)content).getBodyPart(0).getContent();
			if(body instanceof Multipart
					&& ((Multipart)body).getContentType().startsWith("multipart/alternative"))
			{
				//get alternative text
				BodyPart altPart = ((Multipart)body).getBodyPart(0);
				String alterText = (String) altPart.getContent();
				StringWriter alterTextWriter = new StringWriter();
				Velocity.evaluate(context, alterTextWriter, "message body", alterText);
				alterTextWriter.flush();
				alterTextWriter.close();
				alterText = alterTextWriter.toString();
				ContentType ct = new ContentType(altPart.getContentType());
				String alterTextCharset = ct.getParameter("charset");
				
				//get main part (html or related)
				Object mainPart = ((Multipart)body).getBodyPart(1).getContent();
				if(mainPart instanceof Multipart)
				{
					/*
					 * -mixed
					 * \-alternative (body)
					 * 		\-text
					 * 		|-related (mainPart)
					 * 			\-html
					 * 			|-attachment
					 * |-attachments
					 */
					
					//replace html
					String html = (String)  ((Multipart)mainPart).getBodyPart(0).getContent();
					StringWriter mailBody = new StringWriter();
					Velocity.evaluate(context, mailBody, "message body", html);
					mailBody.flush();
					mailBody.close();
					html = mailBody.toString();
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)mainPart).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)mainPart).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					((Multipart)mainPart).removeBodyPart(0);
					((Multipart)mainPart).addBodyPart(htmlPart, 0);
					//replace related
					MimeBodyPart bodyPart = new MimeBodyPart();
					bodyPart.setContent(((Multipart)mainPart));
					((Multipart)body).removeBodyPart(1);
					((Multipart)body).addBodyPart(bodyPart);
					
					//replace body
					((Multipart)content).removeBodyPart(0);
					bodyPart = new MimeBodyPart();
					bodyPart.setContent(((Multipart)body));
					((Multipart)content).addBodyPart(bodyPart, 0);
				}
				else
				{
					/*
					 * -mixed
					 * \-alternative (body)
					 * 		\-text
					 * 		|-html 
					 * |-attachments
					 */
					//create new alternative
					Multipart alternative = new MimeMultipart("alternative");
					//add palin part
					alternative.addBodyPart(((Multipart)body).getBodyPart(0));
					
					//add html
					String html = (String)  ((Multipart)body).getBodyPart(1).getContent();
					StringWriter mailBody = new StringWriter();
					Velocity.evaluate(context, mailBody, "message body", html);
					mailBody.flush();
					mailBody.close();
					html = mailBody.toString();
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)body).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)body).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					
					//replace html
					((Multipart)body).removeBodyPart(1);
					((Multipart)body).addBodyPart(htmlPart, 1);
					
					//replace body
					((Multipart)content).removeBodyPart(0);
					MimeBodyPart bodyPart = new MimeBodyPart();
					bodyPart.setContent(alternative);
					((Multipart)content).addBodyPart(bodyPart, 0);
				}
				
				if(alterTextCharset != null)
					setAlternativeText(alterText, alterTextCharset);
				else
					setAlternativeText(alterText, charset);
			}
			else if(body instanceof Multipart
					&& ((Multipart)body).getContentType().startsWith("multipart/related"))
			{
				/*
				 * -mixed
				 * \-related (body)
				 * 		\-html
				 * 		|-attachment 
				 * |-attachments
				 */
				
				//replace html
				String html = (String) ((Multipart)body).getBodyPart(0).getContent();;
				StringWriter mailBody = new StringWriter();
				Velocity.evaluate(context, mailBody, "message body", html);
				mailBody.flush();
				mailBody.close();
				html = mailBody.toString();
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, ((Multipart)body).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Type", ((Multipart)body).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				((Multipart)body).removeBodyPart(0);
				((Multipart)body).addBodyPart(htmlPart, 0);
				
				//replace body
				((Multipart)content).removeBodyPart(0);
				MimeBodyPart bodyPart = new MimeBodyPart();
				bodyPart.setContent(((Multipart)body));
				((Multipart)content).addBodyPart(bodyPart, 0);
			}
			else
			{
				/*
				 * -mixed
				 * \-html (body)
				 * |-attachments
				 */
				
				//replace html
				String html = (String) ((Multipart)content).getBodyPart(0).getContent();
				StringWriter mailBody = new StringWriter();
				Velocity.evaluate(context, mailBody, "message body", html);
				mailBody.flush();
				mailBody.close();
				html = mailBody.toString();
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Type", ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				
				((Multipart)content).removeBodyPart(0);
				((Multipart)content).addBodyPart(htmlPart, 0);
			}
		}
		else 
		{
			if(content instanceof Multipart
					&& ((Multipart)content).getContentType().startsWith("multipart/alternative"))
			{
				//get alternative text
				BodyPart altPart = ((Multipart)content).getBodyPart(0);
				String alterText = (String) altPart.getContent();
				StringWriter alterTextWriter = new StringWriter();
				Velocity.evaluate(context, alterTextWriter, "message body", alterText);
				alterTextWriter.flush();
				alterTextWriter.close();
				alterText = alterTextWriter.toString();
				ContentType ct = new ContentType(altPart.getContentType());
				String alterTextCharset = ct.getParameter("charset");
				
				//get and replace html part of message
				Object body = ((Multipart)content).getBodyPart(1).getContent();
				if(body instanceof Multipart)
				{
					/*
					 * -alternative (content)
					 * \-text
					 * |-related (body)
					 * 		\-html
					 * 		|-attachment
					 */
					
					//replace html
					String html = (String) ((Multipart)body).getBodyPart(0).getContent();
					StringWriter mailBody = new StringWriter();
					Velocity.evaluate(context, mailBody, "message body", html);
					mailBody.flush();
					mailBody.close();
					html = mailBody.toString();
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)body).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)body).getBodyPart(0).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					((Multipart)body).removeBodyPart(0);
					((Multipart)body).addBodyPart(htmlPart, 0);
					//replace related
					((Multipart)content).removeBodyPart(1);
					MimeBodyPart relatedPart = new MimeBodyPart();
					relatedPart.setContent((Multipart) body);
					((Multipart)content).addBodyPart(relatedPart, 1);
					
				}
				else
				{
					/*
					 * -alternative (content)
					 * \-text
					 * |-html
					 */
					
					//replace html
					String html = (String) ((Multipart)content).getBodyPart(1).getContent();
					StringWriter mailBody = new StringWriter();
					Velocity.evaluate(context, mailBody, "message body", html);
					mailBody.flush();
					mailBody.close();
					html = mailBody.toString();
					MimeBodyPart htmlPart = new MimeBodyPart();
					htmlPart.setContent(html, ((Multipart)content).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Type", ((Multipart)content).getBodyPart(1).getContentType());
					htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
					
					((Multipart)content).removeBodyPart(1);
					((Multipart)content).addBodyPart(htmlPart);
				}
				
				if(alterTextCharset != null)
					setAlternativeText(alterText, alterTextCharset);
				else
					setAlternativeText(alterText, charset);
			}
			else if(content instanceof Multipart
					&& ((Multipart)content).getContentType().startsWith("multipart/related"))
			{
				/*
				 * -related (content)
				 * \-html
				 * |-attachment
				 */
				
				//replace html
				String html = (String) ((Multipart)content).getBodyPart(0).getContent();
				StringWriter mailBody = new StringWriter();
				Velocity.evaluate(context, mailBody, "message body", html);
				mailBody.flush();
				mailBody.close();
				html = mailBody.toString();
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(html, ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Type", ((Multipart)content).getBodyPart(0).getContentType());
				htmlPart.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
				((Multipart)content).removeBodyPart(0);
				((Multipart)content).addBodyPart(htmlPart, 0);
			}
			else 
			{
				/*
				 * -html (content)
				 */
				
				//replace html html
				String html = (String) content;
				StringWriter mailBody = new StringWriter();
				Velocity.evaluate(context, mailBody, "message body", html);
				mailBody.flush();
				mailBody.close();
				html = mailBody.toString();
				content = html;
			}
		}
		
		StringWriter subjectWriter = new StringWriter();
		Velocity.evaluate(context, subjectWriter, "message body", subject);
		subjectWriter.flush();
		subjectWriter.close();
		subject = subjectWriter.toString();
		
	}
	
	
}
