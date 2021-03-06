package com.qwertovsky.mailer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import com.qwertovsky.mailer.errors.QwertoMailerException;



/**
 * @author Qwertovsky
 *
 */
public class MessageContentTest
{
		
	@SuppressWarnings("unused")
	@Test
	public void testMessageContentFile()
	{
		File file1 = null;
		File file2 = new File("");
		File file3 = new File("test_files/test_notexists.eml");
		File file4 = new File("test_files/test_bad_subject.eml");
		File file5 = new File("test_files/test.eml");
		
		//not allow null file
		try
		{
			MessageContent message = new MessageContent(file1);
		} catch (NullPointerException npe)
		{
			//pass
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		//----------
		//not exists file
		try
		{
			MessageContent message = new MessageContent(file2);
		} catch (FileNotFoundException e)
		{
			//pass
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		//----------
		//not exists file
		try
		{
			MessageContent message = new MessageContent(file2);
		} catch (FileNotFoundException e)
		{
			//pass
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		//----------
		//bad subject
		try
		{
			MessageContent message = new MessageContent(file4);
		} catch(QwertoMailerException qme)
		{
			//pass
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		//----------
		try
		{
			MessageContent message = new MessageContent(file5);
			//data from file
			if(!((String)message.getContent()).startsWith("<html>"))
				fail("incorrect content");
			if(!message.getCharset().equals("windows-1251"))
				fail("incorrect charset");
			if(!message.getContentType().equals("text/html; charset=windows-1251"))
				fail("incorrect contentType");
			
			//Content-Transfer-Encoding will be not got from file (default 8bit)
			if(!message.getContentTransferEncoding().equals("8bit"))
				fail("incorrect contentType");
			//address will be not got from file (default null)
			if(message.getAddressFrom() != null)
				fail("incorrect FROM");
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	//--------------------------------------------
	@Test
	public void testSetSubject()
	{
		//not allow null or empty subject
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			
			try
			{
				message.setSubject(null);
				fail("incorrect setSubject");
			} catch (QwertoMailerException qme)
			{
				//pass
			}
			catch(Exception se)
			{
				fail(se.getMessage());
				se.printStackTrace();
			}
			//----------------
			try
			{
				message.setSubject("");
				fail("incorrect setSubject");
			} catch (QwertoMailerException qme)
			{
				//pass
			}
			catch(Exception se)
			{
				fail(se.getMessage());
				se.printStackTrace();
			}
			//----------------
			try
			{
				message.setSubject("subject (тема)");
				if(!"subject (тема)".equals(message.getSubject()))
					fail("incorrect setSubject");
			}catch(Exception se)
			{
				fail(se.getMessage());
				se.printStackTrace();
			}
			
			
		} catch (Exception e)
		{
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	//--------------------------------------------
	@Test
	public void testSetAddressFrom()
	{
		//not allow null or empty email
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			try
			{
				message.setAddressFrom(null, null, null);
				fail("incorrect setAddressFrom");
			} catch (QwertoMailerException qme)
			{
				//pass
			}catch(Exception ae)
			{
				ae.printStackTrace();
				fail(ae.getMessage());
			}
			//----------------
			try
			{
				message.setAddressFrom(null, "", null);
				fail("incorrect setAddressFrom");
			} catch (QwertoMailerException qme)
			{
				//pass
			}catch(Exception ae)
			{
				ae.printStackTrace();
				fail(ae.getMessage());
			}
			//----------------
			try
			{
				message.setAddressFrom(null, "from", null);
				fail("incorrect setAddressFrom");
			} catch (AddressException ae)
			{
				//pass
			}catch(Exception ae)
			{
				ae.printStackTrace();
				fail(ae.getMessage());
			}
			//----------------
			try
			{
				message.setAddressFrom("person", "from@host.ru", null);
				InternetAddress address = (InternetAddress) message.getAddressFrom();
				if(address != null
						&& address.getAddress().equals("from@host.ru")
						&& address.getPersonal().equals("person"))
					;
				else
					fail("incorrect setAddressFrom");
			}catch(Exception ae)
			{
				ae.printStackTrace();
				fail(ae.getMessage());
				
			}
			
		} catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	//--------------------------------------------
	@Test
	public void testAddAttachment()
	{
		/*
		 * -mixed
		 * \-html (index 0)
		 * |-attachment (index 1)
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			message.addAttachment(new File("test_files/test.eml"));
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				BodyPart file = ((Multipart)mixed).getBodyPart(1);
				if(!file.getFileName().equals("test.eml"))
						fail("incorrect addAttachments");
			}
			else
				fail("incorrect addAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addAttachments");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative (index 0)
		 * 		\-palin
		 * 		|-html
		 * |-attachment (index 1)
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt.eml"));
			message.addAttachment(new File("test_files/test.eml"));
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				BodyPart file = ((Multipart)mixed).getBodyPart(1);
				if(!file.getFileName().equals("test.eml"))
						fail("incorrect addAttachments");
			}
			else
				fail("incorrect addAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addAttachments");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative (index 0)
		 * 		\-palin
		 * 		|-html
		 * |-attachment (index 1) old
		 * |-attachment (index 2) new
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed.eml"));
			message.addAttachment(new File("test_files/test.eml"));
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				BodyPart file = ((Multipart)mixed).getBodyPart(2);
				if(!file.getFileName().equals("test.eml"))
						fail("incorrect addAttachments");
			}
			else
				fail("incorrect addAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addAttachments");
		}
	}
	//--------------------------------------------
	@Test
	public void testSetAlternativeText()
	{
		/*
		 *  -alternative 
		 *  \-plain 
		 *  |-html
		 *  
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			message.setAlternativeText("alternative text (алтернативный текст)", "utf-8");
			if(!message.getContentType().startsWith("multipart/alternative"))
				fail("incorrect setAlternativeText");
			Object alternative = message.getContent();
			if(alternative instanceof Multipart
					&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
			{
				BodyPart plain = ((Multipart)alternative).getBodyPart(0);
				if(!plain.getContent().equals("alternative text (алтернативный текст)")
						|| !plain.getContentType().equals("text/plain"))
					fail("incorrect setAlternativeText");
			}
			else
				fail("incorrect setAlternativeText");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setAlternativeText");
		}
		//--------------------
		/*
		 *  -alternative 
		 *  \-plain (old will be replaced by new)
		 *  |-html
		 *  
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt.eml"));
			message.setAlternativeText("alternative text (алтернативный текст)", "utf-8");
			if(!message.getContentType().startsWith("multipart/alternative"))
				fail("incorrect setAlternativeText");
			Object alternative = message.getContent();
			if(alternative instanceof Multipart
					&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
			{
				BodyPart plain = ((Multipart)alternative).getBodyPart(0);
				if(!plain.getContent().equals("alternative text (алтернативный текст)")
						|| !plain.getContentType().equals("text/plain"))
					fail("incorrect setAlternativeText");
			}
			else
				fail("incorrect setAlternativeText");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setAlternativeText");
		}
		//--------------------
		/*
		 *  -mixed
		 *  \-alternative 
		 *  	\-plain (old will be replaced by new)
		 *  	|-html
		 *  |-attachment
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed.eml"));
			message.setAlternativeText("alternative text (алтернативный текст)", "utf-8");
			if(!message.getContentType().startsWith("multipart/mixed"))
				fail("incorrect setAlternativeText");
			Object mixed = message.getContent();
			if(mixed instanceof Multipart 
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed")) 
			{	
				Object alternative =((Multipart)mixed).getBodyPart(0).getContent();
				if(alternative instanceof Multipart
						&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
				{
					BodyPart plain = ((Multipart)alternative).getBodyPart(0);
					if(!plain.getContent().equals("alternative text (алтернативный текст)")
							|| !plain.getContentType().equals("text/plain"))
						fail("incorrect setAlternativeText");
				}
				else
					fail("incorrect setAlternativeText");
			}
			else
				fail("incorrect setAlternativeText");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setAlternativeText");
		}
	}
	//--------------------------------------------
	@Test
	public void testSetContentTransferEncoding()
	{
		//default is 8bit
		//not allow null and empty
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			if(!"8bit".equalsIgnoreCase(message.getContentTransferEncoding()))
				fail("incorrect ContentTransferEncoding");
			
			message.setContentTransferEncoding(null);
			if(!"8bit".equalsIgnoreCase(message.getContentTransferEncoding()))
					fail("incorrect ContentTransferEncoding");
			
			message.setContentTransferEncoding("");
			if(!"8bit".equalsIgnoreCase(message.getContentTransferEncoding()))
					fail("incorrect ContentTransferEncoding");
			
			message.setContentTransferEncoding("base64");
			if(!"base64".equalsIgnoreCase(message.getContentTransferEncoding()))
					fail("incorrect ContentTransferEncoding");
		} catch (Exception e)
		{
			fail(e.getMessage());
			e.printStackTrace();
		}
	}
	//--------------------------------------------
	@Test
	public void testAddInlineAttachment()
	{
		
		/*
		 * -related
		 * \-html (index 0)
		 * |-attachment (index 1)
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			String cid = message.addInlineAttachment("../qwertomailer/test_files/test.png");
			Object related = message.getContent();
			if(related instanceof Multipart
					&& ((Multipart)related).getContentType().startsWith("multipart/related"))
			{
				BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
				if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect addInlineAttachments");
				String html = (String) htmlPart.getContent();
				if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))	
					fail("incorrect addInlineAttachments");
				BodyPart file = ((Multipart)related).getBodyPart(1);
				if(!file.getHeader("Content-ID")[0].equals(cid))
						fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");;
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -alternative 
		 * 	\-plain
		 * 	|-related
		 * 			\-html
		 * 			|-attachment
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object alternative = message.getContent();
			if(alternative instanceof Multipart
					&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
			{
				Object related = ((Multipart)alternative).getBodyPart(1).getContent();
				if(related instanceof Multipart
						&& ((Multipart)related).getContentType().startsWith("multipart/related"))
				{
					BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
					if(!htmlPart.getContentType().startsWith("text/html"))
							fail("incorrect addInlineAttachments");
					String html = (String) htmlPart.getContent();
					if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
						fail("incorrect addInlineAttachments");
					BodyPart file = ((Multipart)related).getBodyPart(1);
					if(!file.getFileName().equals("test.png"))
						fail("incorrect addInlineAttachments");
					if(!file.getHeader("Content-ID")[0].equals(cid))
						fail("incorrect addInlineAttachments");
				}
				else
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -related
		 * 	\-html
		 * 	|-attachment
		 * 	|-attachment new
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_related.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object related = message.getContent();
			if(related instanceof Multipart
					&& ((Multipart)related).getContentType().startsWith("multipart/related"))
			{
				BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
				if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect addInlineAttachments");
				String html = (String) htmlPart.getContent();
				if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
					fail("incorrect addInlineAttachments");
				BodyPart file = ((Multipart)related).getBodyPart(2);
				if(!file.getFileName().equals("test.png"))
					fail("incorrect addInlineAttachments");
				if(!file.getHeader("Content-ID")[0].equals(cid))
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -alternative 
		 * 	\-plain
		 * 	|-related
		 * 			\-html
		 * 			|-attachment
		 * 			|-attachment new
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt_related.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object alternative = message.getContent();
			if(alternative instanceof Multipart
					&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
			{
				Object related = ((Multipart)alternative).getBodyPart(1).getContent();
				if(related instanceof Multipart
						&& ((Multipart)related).getContentType().startsWith("multipart/related"))
				{
					BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
					if(!htmlPart.getContentType().startsWith("text/html"))
							fail("incorrect addInlineAttachments");
					String html = (String) htmlPart.getContent();
					if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
						fail("incorrect addInlineAttachments");
					BodyPart file = ((Multipart)related).getBodyPart(2);
					if(!file.getFileName().equals("test.png"))
						fail("incorrect addInlineAttachments");
					if(!file.getHeader("Content-ID")[0].equals(cid))
						fail("incorrect addInlineAttachments");
				}
				else
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -mixed
		 * \-related
		 * 		\-html
		 * 		|-attachment
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object related = ((Multipart)mixed).getBodyPart(0).getContent();
				if(related instanceof Multipart
						&& ((Multipart)related).getContentType().startsWith("multipart/related"))
				{
					BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
					if(!htmlPart.getContentType().startsWith("text/html"))
							fail("incorrect addInlineAttachments");
					String html = (String) htmlPart.getContent();
					if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
						fail("incorrect addInlineAttachments");
					BodyPart file = ((Multipart)related).getBodyPart(1);
					if(!file.getFileName().equals("test.png"))
						fail("incorrect addInlineAttachments");
					if(!file.getHeader("Content-ID")[0].equals(cid))
						fail("incorrect addInlineAttachments");
				}
				else
					fail("incorrect addInlineAttachments");
				
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative 
		 * 		\-plain
		 * 		|-related
		 * 			\-html
		 * 			|-attachment
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_alt.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object alternative = ((Multipart)mixed).getBodyPart(0).getContent();
				if(alternative instanceof Multipart
						&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
				{
					Object related = ((Multipart)alternative).getBodyPart(1).getContent();
					if(related instanceof Multipart
							&& ((Multipart)related).getContentType().startsWith("multipart/related"))
					{
						BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
						if(!htmlPart.getContentType().startsWith("text/html"))
								fail("incorrect addInlineAttachments");
						String html = (String) htmlPart.getContent();
						if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
							fail("incorrect addInlineAttachments");
						BodyPart file = ((Multipart)related).getBodyPart(1);
						if(!file.getFileName().equals("test.png"))
							fail("incorrect addInlineAttachments");
						if(!file.getHeader("Content-ID")[0].equals(cid))
							fail("incorrect addInlineAttachments");
					}
					else
						fail("incorrect addInlineAttachments");
				}
				else
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -mixed
		 * \-related
		 * 		\-html
		 * 		|-attachment
		 * 		|-attachment new
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_related.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object related = ((Multipart)mixed).getBodyPart(0).getContent();
				if(related instanceof Multipart
						&& ((Multipart)related).getContentType().startsWith("multipart/related"))
				{
					BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
					if(!htmlPart.getContentType().startsWith("text/html"))
							fail("incorrect addInlineAttachments");
					String html = (String) htmlPart.getContent();
					if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
						fail("incorrect addInlineAttachments");
					BodyPart file = ((Multipart)related).getBodyPart(2);
					if(!file.getFileName().equals("test.png"))
						fail("incorrect addInlineAttachments");
					if(!file.getHeader("Content-ID")[0].equals(cid))
						fail("incorrect addInlineAttachments");
				}
				else
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative 
		 * 		\-plain
		 * 		|-related
		 * 			\-html
		 * 			|-attachment
		 * 			|-attachment new
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_alt_related.eml"));
			String cid = message.addInlineAttachment("test_files/test.png");
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object alternative = ((Multipart)mixed).getBodyPart(0).getContent();
				if(alternative instanceof Multipart
						&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
				{
					Object related = ((Multipart)alternative).getBodyPart(1).getContent();
					if(related instanceof Multipart
							&& ((Multipart)related).getContentType().startsWith("multipart/related"))
					{
						BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
						if(!htmlPart.getContentType().startsWith("text/html"))
								fail("incorrect addInlineAttachments");
						String html = (String) htmlPart.getContent();
						if(!html.contains("\"cid:" + cid.substring(1, cid.length()-1) +"\""))
							fail("incorrect addInlineAttachments");
						BodyPart file = ((Multipart)related).getBodyPart(2);
						if(!file.getFileName().equals("test.png"))
							fail("incorrect addInlineAttachments");
						if(!file.getHeader("Content-ID")[0].equals(cid))
							fail("incorrect addInlineAttachments");
					}
					else
						fail("incorrect addInlineAttachments");
				}
				else
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect addInlineAttachments");
		}
	}
	//--------------------------------------------
	@Test
	public void testSetRelated()
	{
		/*
		 * -related
		 * \-html (index 0)
		 * |-attachment (index 1)
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("../qwertomailer/test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -alternative 
		 * 	\-plain
		 * 	|-related
		 * 			\-html
		 * 			|-attachment
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -related
		 * 	\-html
		 * 	|-attachment
		 * 	|-attachment new
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_related.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -alternative 
		 * 	\-plain
		 * 	|-related
		 * 			\-html
		 * 			|-attachment
		 * 			|-attachment new
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt_related.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -mixed
		 * \-related
		 * 		\-html
		 * 		|-attachment
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative 
		 * 		\-plain
		 * 		|-related
		 * 			\-html
		 * 			|-attachment
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_alt.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -mixed
		 * \-related
		 * 		\-html
		 * 		|-attachment
		 * 		|-attachment new
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_related.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative 
		 * 		\-plain
		 * 		|-related
		 * 			\-html
		 * 			|-attachment
		 * 			|-attachment new
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_alt_related.eml"));
			List<String> paths = message.setRelated();
			if(paths.size() != 1)
				fail("incorrect setRelated");
			if(!paths.get(0).equalsIgnoreCase("test_files/test.png"))
				fail("incorrect setRelated");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setRelated");
		}
	}

	//--------------------------------------------
	@Test
	public void testSetParameters() throws Exception
	{
		/*
		 * error
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			message.setParameters(new String[]{}
				, new String[]{"message parameter","subject parameter"});
			String html = (String) message.getContent();
			if(!message.getContentType().startsWith("text/html"))
				fail("incorrect setParameters");
			String subject = message.getSubject();
			if(!html.contains("<br />message parameter"))
				fail("not correct message");
			if(!subject.contains("subject parameter"))
				fail("not correct subject");
		} catch (QwertoMailerException qme)
		{
			//pass
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * error
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter"});
			String html = (String) message.getContent();
			if(!message.getContentType().startsWith("text/html"))
				fail("incorrect setParameters");
			String subject = message.getSubject();
			if(!html.contains("<br />message parameter"))
				fail("not correct message");
			if(!subject.contains("subject parameter"))
				fail("not correct subject");
		} catch (QwertoMailerException qme)
		{
			//pass
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * replace null to empty string
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter",null});
			String html = (String) message.getContent();
			if(!message.getContentType().startsWith("text/html"))
				fail("incorrect setParameters");
			String subject = message.getSubject();
			if(!html.contains("<br />message parameter"))
				fail("not correct message");
			//$subject must be replaced to empty string
			System.out.println(subject);
			if(subject.contains("$subject") || subject.contains("ull"))
				fail("not correct subject");
		} catch (QwertoMailerException qme)
		{
			//pass
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		
		/*
		 * -html (content)
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			String html = (String) message.getContent();
			if(!message.getContentType().startsWith("text/html"))
				fail("incorrect setParameters");
			String subject = message.getSubject();
			if(!html.contains("<br />message parameter"))
				fail("not correct message");
			if(!subject.contains("subject parameter"))
				fail("not correct subject");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -alternative 
		 * 	\-plain
		 * 	|-html
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object alternative = message.getContent();
			if(alternative instanceof Multipart
					&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
			{
				BodyPart textPart = ((Multipart)alternative).getBodyPart(0);
				BodyPart htmlPart = ((Multipart)alternative).getBodyPart(1);
				if(!htmlPart.getContentType().startsWith("text/html"))
					fail("incorrect setParameters");
				String text = (String) textPart.getContent();
				String html = (String) htmlPart.getContent();
				String subject = message.getSubject();
				if(!text.contains("message parameter"))
					fail("not correct alternative text");
				if(!html.contains("<br />message parameter"))
					fail("not correct message");
				if(!subject.contains("subject parameter"))
					fail("not correct subject");
			}
			else
				fail("must be alternative");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -related
		 * 	\-html
		 * 	|-attachment
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_related.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object related = message.getContent();
			if(related instanceof Multipart
					&& ((Multipart)related).getContentType().startsWith("multipart/related"))
			{
				BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
				if(!htmlPart.getContentType().startsWith("text/html"))
					fail("incorrect setParameters");
				String html = (String) htmlPart.getContent();
				String subject = message.getSubject();
				if(!html.contains("<br />message parameter"))
					fail("not correct message");
				if(!subject.contains("subject parameter"))
					fail("not correct subject");
			}
			else
				fail("must be related");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -alternative 
		 * 	\-plain
		 * 	|-related
		 * 			\-html
		 * 			|-attachment
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_alt_related.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object alternative = message.getContent();
			if(alternative instanceof Multipart
					&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
			{
				Object related = ((Multipart)alternative).getBodyPart(1).getContent();
				if(related instanceof Multipart
						&& ((Multipart)related).getContentType().startsWith("multipart/related"))
				{
					BodyPart textPart = ((Multipart)alternative).getBodyPart(0);
					BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
					if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect setParameters");
					String text = (String) textPart.getContent();
					String html = (String) htmlPart.getContent();
					String subject = message.getSubject();
					if(!text.contains("message parameter"))
						fail("not correct alternative text");
					if(!html.contains("<br />message parameter"))
						fail("not correct message");
					if(!subject.contains("subject parameter"))
						fail("not correct subject");
				}
				else
					fail("must be related");
			}
			else
				fail("must be alternative");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -mixed
		 * \-html
		 * |-attachment (index 1) 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				BodyPart htmlPart = ((Multipart)mixed).getBodyPart(0);
				if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect setParameters");
				String html = (String) htmlPart.getContent();
				String subject = message.getSubject();
				if(!html.contains("<br />message parameter"))
					fail("not correct message");
				if(!subject.contains("subject parameter"))
					fail("not correct subject");
			}
			else
				fail("must be mixed");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative 
		 * 		\-plain
		 * 		|-html
		 * |-attachment (index 1) 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_alt.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object alternative = ((Multipart)mixed).getBodyPart(0).getContent();
				if(alternative instanceof Multipart
						&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
				{
					BodyPart textPart = ((Multipart)alternative).getBodyPart(0);
					BodyPart htmlPart = ((Multipart)alternative).getBodyPart(1);
					if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect setParameters");
					String text = (String) textPart.getContent();
					String html = (String) htmlPart.getContent();
					String subject = message.getSubject();
					if(!text.contains("message parameter"))
						fail("not correct alternative text");
					if(!html.contains("<br />message parameter"))
						fail("not correct message");
					if(!subject.contains("subject parameter"))
						fail("not correct subject");
						
				}
				else
					fail("must be alternative");
			}
			else
				fail("must be mixed");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -mixed
		 * \-related
		 * 		\-html
		 * 		|-attachment
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_related.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object related = ((Multipart)mixed).getBodyPart(0).getContent();
				if(related instanceof Multipart
						&& ((Multipart)related).getContentType().startsWith("multipart/related"))
				{
					BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
					if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect setParameters");
					String html = (String) htmlPart.getContent();
					String subject = message.getSubject();
					if(!html.contains("<br />message parameter"))
						fail("not correct message");
					if(!subject.contains("subject parameter"))
						fail("not correct subject");
				}
				else
					fail("incorrect addInlineAttachments");
			}
			else
				fail("incorrect addInlineAttachments");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		//--------------------
		/*
		 * -mixed
		 * \-alternative 
		 * 		\-plain
		 * 		|-related
		 * 			\-html
		 * 			|-attachment
		 * |-attachment (index 1) 
		 * 
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test_multipart_mixed_alt_related.eml"));
			message.setParameters(new String[]{"message", "subject"}
				, new String[]{"message parameter","subject parameter"});
			Object mixed = message.getContent();
			if(mixed instanceof Multipart
					&& ((Multipart)mixed).getContentType().startsWith("multipart/mixed"))
			{
				Object alternative = ((Multipart)mixed).getBodyPart(0).getContent();
				if(alternative instanceof Multipart
						&& ((Multipart)alternative).getContentType().startsWith("multipart/alternative"))
				{
					Object related = ((Multipart)alternative).getBodyPart(1).getContent();
					if(related instanceof Multipart
							&& ((Multipart)related).getContentType().startsWith("multipart/related"))
					{
						BodyPart textPart = ((Multipart)alternative).getBodyPart(0);
						BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
						if(!htmlPart.getContentType().startsWith("text/html"))
							fail("incorrect setParameters");
						String text = (String) textPart.getContent();
						String html = (String) htmlPart.getContent();
						String subject = message.getSubject();
						if(!text.contains("message parameter"))
							fail("not correct alternative text");
						if(!html.contains("<br />message parameter"))
							fail("not correct message");
						if(!subject.contains("subject parameter"))
							fail("not correct subject");
					}
					else
						fail("must be related");
				}
				else
					fail("must be alternative");
			}
			else
				fail("must be mixed");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
	}
	
	//--------------------------------------------
	@Test
	public void testSetMapParameters() throws Exception
	{
		/*
		 * -html (content)
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("message","message parameter");
			parameters.put("subject","subject parameter");
			message.setParameters(parameters);
			String html = (String) message.getContent();
			if(!message.getContentType().startsWith("text/html"))
					fail("incorrect setParameters");
			String subject = message.getSubject();
			if(!html.contains("<br />message parameter"))
				fail("not correct message");
			if(!subject.contains("subject parameter"))
				fail("not correct subject");
		}catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
		
		//----------------------------------------
		/*
		 * error
		 */
		try
		{
			MessageContent message = new MessageContent(new File("test_files/test.eml"));
			Map<String, String> parameters = new HashMap<String, String>();
			message.setParameters(parameters);
			String html = (String) message.getContent();
			if(!message.getContentType().startsWith("text/html"))
				fail("incorrect setParameters");
			String subject = message.getSubject();
			if(!html.contains("<br />message parameter"))
				fail("not correct message");
			if(!subject.contains("subject parameter"))
				fail("not correct subject");
		} catch (QwertoMailerException qme)
		{
			//pass
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail("incorrect setParameters");
		}
	}
	
	//--------------------------------------------
	@Test
	public void testToByteArray() throws Exception
	{
		MessageContent messageContent = new MessageContent("test message", "text/html", "subject", "utf-8");
		byte[] messageByteArray = messageContent.toByteArray();
		
		ByteArrayInputStream messageIS = new ByteArrayInputStream(messageByteArray);
		Session session = Session.getDefaultInstance(new Properties(), null);
		MimeMessage mimeMessage = new MimeMessage(session, messageIS);
		
		String contentType = mimeMessage.getContentType();
		Object contentObject = mimeMessage.getContent();
		String subject = mimeMessage.getSubject();
		
		assertTrue(contentType.startsWith("text/html"));
		assertEquals("test message", contentObject);
		assertEquals("subject", subject);
	}
}
