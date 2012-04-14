package com.qwertovsky.mailer;
import static org.junit.Assert.fail;

import java.io.File;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;

import org.junit.Test;


public class MailMessageTest
{
		
	@SuppressWarnings("unused")
	@Test
	public void testMailMessageFile()
	{
		File file1 = null;
		File file2 = new File("");
		File file3 = new File("test_notexists.eml");
		File file4 = new File("test.eml");
		
		//not allow null file
		try
		{
			MailMessage message = new MailMessage(file1);
		} catch (Exception e)
		{
			if(e.getMessage().equals("EML file is null"))
				;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
		//----------
		//not exists file
		try
		{
			MailMessage message = new MailMessage(file2);
		} catch (Exception e)
		{
			if(e.getMessage().equals("EML file not exists"))
				;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
		//----------
		//not exists file
		try
		{
			MailMessage message = new MailMessage(file3);
		} catch (Exception e)
		{
			if(e.getMessage().equals("EML file not exists"))
				;
			else
			{
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
		//----------
		try
		{
			MailMessage message = new MailMessage(file4);
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

	@Test
	public void testSetSubject()
	{
		//not allow null or empty subject
		try
		{
			MailMessage message = new MailMessage(new File("test.eml"));
			
			try
			{
				message.setSubject(null);
				fail("incorrect setSubject");
			}catch(Exception se)
			{
				if(se.getMessage().equals("Bad subject"))
					;
				else
				{
					fail(se.getMessage());
					se.printStackTrace();
				}
			}
			//----------------
			try
			{
				message.setSubject("");
				fail("incorrect setSubject");
			}catch(Exception se)
			{
				if(se.getMessage().equals("Bad subject"))
					;
				else
				{
					fail(se.getMessage());
					se.printStackTrace();
				}
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

	@Test
	public void testSetAddressFrom()
	{
		//not allow null or empty email
		try
		{
			MailMessage message = new MailMessage(new File("test.eml"));
			try
			{
				message.setAddressFrom(null, null, null);
				fail("incorrect setAddressFrom");
			}catch(Exception ae)
			{
				if("Bad email in FROM".equals(ae.getMessage()))
					;
				else
				{
					ae.printStackTrace();
					fail(ae.getMessage());
				}
			}
			//----------------
			try
			{
				message.setAddressFrom(null, "", null);
				fail("incorrect setAddressFrom");
			}catch(Exception ae)
			{
				if("Bad email in FROM".equals(ae.getMessage()))
					;
				else
				{
					ae.printStackTrace();
					fail(ae.getMessage());
				}
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
			MailMessage message = new MailMessage(new File("test.eml"));
			message.addAttachment(new File("test.eml"));
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
			MailMessage message = new MailMessage(new File("test_multipart_alt.eml"));
			message.addAttachment(new File("test.eml"));
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
			MailMessage message = new MailMessage(new File("test_multipart_mixed.eml"));
			message.addAttachment(new File("test.eml"));
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
			MailMessage message = new MailMessage(new File("test.eml"));
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
			MailMessage message = new MailMessage(new File("test_multipart_alt.eml"));
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
			MailMessage message = new MailMessage(new File("test_multipart_mixed.eml"));
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

	@Test
	public void testSetContentTransferEncoding()
	{
		//default is 8bit
		//not allow null and empty
		try
		{
			MailMessage message = new MailMessage(new File("test.eml"));
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
			MailMessage message = new MailMessage(new File("test.eml"));
			String cid = message.addInlineAttachment("../qwertomailer/test.png");
			Object related = message.getContent();
			if(related instanceof Multipart
					&& ((Multipart)related).getContentType().startsWith("multipart/related"))
			{
				BodyPart htmlPart = ((Multipart)related).getBodyPart(0);
				if(!htmlPart.getContentType().startsWith("text/html"))
						fail("incorrect addInlineAttachments");
				String html = (String) htmlPart.getContent();
				if(!html.contains("\"cid:" + cid +"\""))
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
			MailMessage message = new MailMessage(new File("test_multipart_alt.eml"));
			String cid = message.addInlineAttachment("test.png");
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
					if(!html.contains("\"cid:" + cid +"\""))
						fail("incorrect addInlineAttachments");
					BodyPart file = ((Multipart)related).getBodyPart(1);
					if(!file.getFileName().equals("test.png"))
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
			MailMessage message = new MailMessage(new File("test_multipart_mixed.eml"));
			String cid = message.addInlineAttachment("test.png");
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
						if(!html.contains("\"cid:" + cid +"\""))
							fail("incorrect addInlineAttachments");
						BodyPart file = ((Multipart)related).getBodyPart(1);
						if(!file.getFileName().equals("test.png"))
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
}
