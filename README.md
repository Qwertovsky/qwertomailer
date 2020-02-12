Qwertomailer - Java library and CLI program that serves for sending e-mails.
It works over `JavaMail API` and provide an easier way to create and send emails.

QwertoMailer is similar to such libraries as [Simple Java Mail](https://code.google.com/p/simple-java-mail/)
 and [Apache Commons Email](http://commons.apache.org/email/index.html).\
Unlike them QwertoMailer may be used like CLI program to send emails from terminal.

With QwertoMailer you can include in email personal data. The text of the latter can be created as a [Velocity](http://velocity.apache.org/engine/devel/user-guide.html) template. Data will be placed in template from file with parameters.

QwertoMailer works with EML files. So it is possible to create template with Thunderbird or other email clients.

# Download

[Releases](https://github.com/Qwertovsky/qwertomailer/releases)

# CLI

On call without parameters the manual will be printed.
``` bash
java -jar qwertomailer.jar
usage: java -jar qwertomailer.jar [-alttext <text> | -alttextFile <file>]
	[-attach <files> | -attachFile <file>]
	-body <message> | -bodyEML <file> | -bodyFile <file>
	[-charset <charset>] [-contentType <type>]
	-emailFrom <email> -emailTo <recipients> | -emailToFile <file>
	[-hostname <hostname>] [-mimeTransport <transport>]
	[-personFrom <person>] [-related] -smtpHost <host>
	[-smtpPassword <password>] [-smtpPort <port>] [-smtpUser <username>]
	-subject <subject> | -subjectFile <file>
	[-trace]
 -alttext <text>              Add alternative plain text
 -alttextFile <file>          Add alternative plain text from file
 -attach <files>              attach files (comma separated)
 -attachFile <file>           file with list of attach file
 -body <message>              message body
 -EMLFile <file>              get content from EML file
 -bodyFile <file>             message body file
 -charset <charset>           specify message encoding (default utf-8)
 -contentType <type>          specify content type (default text/plain)
 -emailFrom <email>           specify sender email
 -emailTo <recipients>        specify recipients (comma separated)
 -emailToFile <file>          specify file with recipients list
 -hostname <hostname>         replace your local machine name
 -mimeTransport <transport>   specify MIME Transport (default 8bit)
 -personFrom <person>         specify sender name
 -related                     Create message with inline images
 -smtpHost <host>             specify SMTP server
 -smtpPassword <password>     specify SMTP password
 -smtpPort <port>             specify SMTP port (default 25)
 -smtpUser <username>         specify SMTP user
 -subject <subject>           subject
 -subjectFile <file>          file with subject line
 -trace                       Set trace log level. Send messages will be
							  saved on disk
 -haltOnFailure               Stop program if exists bad emails or wrong
                              parameters
```
The bold ones is required. You can use just one from group of them.

 `-alttext <text>` alternative text for html email. If email client does not work with html, it shows this text.\
 `-alttextFile <file>` alternative text from file.

 `-attach <files>` Attach files. Comma separated.\
 `-attachFile <file>` Paths to files are in text file. One path on a row.

 __`-body <message>`__ Text.\
 __`-EMLFile <file>`__ Get message body from EML file. Message will include subject, text, alternative text, and attachments from EML file.\
 __`-bodyFile <file>`__ Only text from text file.\
 You can choose one of the three.

`-charset <charset>` Encoding. Default - utf-8.

`-contentType <type>` Message format (text/plain or text/html).

__`-emailFrom <email>`__ Address from.
 
__`-emailTo <recipients>`__ Comma separated addresses TO. Example: `Person <address1@host.com>, Person2 <address2@host.com>`\
__`-emailToFile <file>`__ CSV file with addresses and parameters.
 The first row is headers. Header that starts with `email` is necessary.  `attach` and `person` are special too. `attach*` columns are paths to files. `person` column is address name (only one allowed).
 Each field can hold more than one value, comma separated, escaped by `"`.\
 Choose one of `emailTo` or `emailToFile`.
 
 `-hostname <hostname>` Change computer name
 
 `-mimeTransport <transport>` MIME transport. Default - 8bit.
 
 `-personFrom <person>` Sender name.</p>
 
 `-related` Make HTML message with images.
 If text contains `<IMG>` with links to files, the message will be with images.
 
 __`-smtpHost <host>`__ SMTP server
 
 `-smtpPassword <password>` SMTP server password
 
 `-smtpPort <port>` SMTP server port. Default - 25.
 
 `-smtpUser <username>` SMTP server login

 __`-subject <subject>`__  Subject\
 __`-subjectFile <file>`__ Subject from file (one line).\
 Choose one of them. Will not work if `-EMLFile` are specified. EML file should be with subject.
 
 `-trace` Print information for each message. Messages will be stored as EML to `./messages`.

`-haltOnFailure` Stop sending if error are thowed (wrong address, parameters)

## Examples
### HTML message
``` bash
java -jar qwertomailer.jar \
-smtpHost smtp.company.ru \
-smtpUser mylogin \
-smtpPassword mypassword \
-emailFrom mylogin@company.ru \
-emailToFile clients_emails.csv \
-subject "Письмо" \
-body "<html>Текст письма</html>" \
-contentType text/html
```

### HTML with images and attachments
``` bash
java -jar qwertomailer.jar \
-smtpHost smtp.company.ru \
-smtpUser mylogin \
-smtpPassword mypassword \
-emailFrom mylogin@company.ru \
-emailToFile clients_emails.csv \
-subject "Subject" \
-body "<html><img src="file:///d:/inline_image.png" /><br />Message text</html>" \
-contentType text/html \
-related \
-attach "d:/presentation.pdf"
```

### Text message with parameters
``` bash
java -jar qwertomailer.jar \
-smtpHost smtp.company.ru \
-smtpUser mylogin \
-smtpPassword mypassword \
-emailFrom mylogin@company.ru \
-emailToFile clients.csv \
-subject "New tarif $tarif" \
-bodyFile body.txt
```

_clients.csv_
``` csv 
email1,email2,fio,tarif,attach1,organization
abon1@gmail.com,,Mr. Dow,Super 146,super146.pdf,
abon2@gmail.com,,Мr. Smith,Super 146,super146.pdf,
,"emp1@company.ru, emp2@company.ru",,Corporate 46,corp46.pdf,CompanyCorp
```

_body.txt_
``` txt
Hello, #if(!($fio=="")) $fio#else $organization#end.
Your new tarif is $tarif.
```

Three messages will be sended. One is for each row. Parameters will ne inserted in subject and body. Attachments will be added. Files from columns that started with `attach` (attach , attach1, attach2, ...) are used as attacments. \
The third message has two addressee. In field `TO` will be added all address from columns that started with `email` (email1, email2, ...).
 
## Encoding
Files with text, subject, parameters should be encoded with the same charset (option`-charset`). UTF-8 is default.

# Library usage

### Text message

``` java
MessageContent message = new MessageContent("Message text", "text/plain", "Subject", "utf-8");
message.setAddressFrom("Company", "sender@company.com", "utf-8");
Sender sender = new Sender("smtp.company.com", 25, "login", "password", "Company");
List<InternetAddress> emailsTo = new ArrayList<InternetAddress>();
emailsTo.add(new InternetAddress("john.doe@server.com"));
sender.send(message, emailsTo);
```

### HTML with alternative text

``` java
MessageContent message = new MessageContent(
	"<html>HTML message</html>", "text/html", "Subject", "utf-8");
message.setAlternativeText("Text message", "utf-8");
message.setAddressFrom("Company", "sender@company.com", "utf-8");
```

### Message with attachments

``` java
MessageContent message = new MessageContent(
	"<html>HTML message</html>", "text/html", "Subject", "utf-8");
message.setAddressFrom("Company", "sender@company.com", "utf-8");
List<File> attachFiles = new ArrayList<File>();
attachFiles.add(new File("file.pdf"));
message.addAttachments(attachFiles);
message.addAttachment(new File("another_file.zip"));
```
					
### Message with images

``` java
MessageContent message = new MessageContent(
	"<html><img src=\"file:///d:/inline_image.png\" /><br />Текст письма</html>"
	, "text/html", "Subject", "utf-8");
message.setAddressFrom("Company", "sender@company.com", "utf-8");
message.setRelated();
```

### Message from EML file

``` java
MessageContent message = new MessageContent(new File("message.eml"));
message.setAddressFrom("Company", "sender@company.com", "utf-8");
```
Text, attachments and subject will be taken from file.

### Message with parameters
_message.eml_
```
Hello, #if(!($fio=="")) $fio#else $organization#end.
Your tarif was changed to $tarif.
```

``` java
MessageContent message = new MessageContent(new File("message.eml"));
message.setAddressFrom("Company", "sender@company.com", "utf-8");
message.setSubject("New tarif $tarif");
String[] parametersHeaders = new String[]
	{"email1", "fio", "tarif", "attach", "organization"};

List<String[]> parameters = new ArrayList<String[]>();
parameters.add(new String[]{"abon1@gmail.com", "Mr. Dow", "Super 146", "super146.pdf", ""});
parameters.add(new String[]{"abon2@gmail.com", "Mr. Smith", "Super 146", "super146.pdf", ""});
parameters.add(new String[]{"emp1@company.ru, emp2@company.ru", "", "Corporative 46", "corp46.pdf", "CompanyCorp"});
Sender sender = new Sender("smtp.company.com", 25, "login", "password", "Company");
sender.send(message, parametersHeaders, parameters);
```

Three messages will be sended The last one will be sended to two addressee (method TO).

There is another way to use parameters. If need to send one message to all addresses. And template has parameters.

Текст сообщения:

_message.eml_
```
Hello, $fio.
Your tarif was changed to $tarif.
```

``` java
MessageContent message = new MessageContent(new File("message.eml"));
message.setAddressFrom("Company", "sender@company.com", "utf-8");
message.setSubject("New tarif $tarif");
String[] parametersHeaders = new String[]{"tarif"};
String[] parameters = new String[]{"Super 146"};
message.setParameters(parametersHeaders, parameters);
message.addAttachment(new File("super146.pdf"));

parametersHeaders = new String[]
	{"email1", "fio"};
List<String[]> personParameters = new ArrayList<String[]>();
personParameters.add(new String[]{"abon1@gmail.com", "Mr. Dow"});
personParameters.add(new String[]{"abon2@gmail.com", "Mr. Smith"});
Sender sender = new Sender("smtp.company.com", 25, "login", "password", "Company");
sender.send(message, parametersHeaders, personParameters);
```
There are common and personalized parameters.
					
# API
More methods you can find in [Javadoc](https://qwertovsky.com/mailer/docs/)

# Dependencies

- JavaMail API
- SLF4j API
- Apache Velocity
- Apache Commons Сollections - for Velocity
- Apache Commons Lang - for Velocity
- Apache Commons CLI - for CLI
- OpenCSV - for csv
- SLF4j-log4j - for CLI
- Log4J - for CLI
- JUnit - for tests
- SubethaSMTP - for tests

# License
OK - using for your company, if you send messages to your registered customers and FROM is your company address.

DON'T use as service to send messages (from your customers to their customers).

GPL - for other cases.
