# 0.9.2
## Features
Add personal part to email address. You may set option
``` bash
-emailTo "Personal <Email@host.com>, Personal2 <Email2@host.com>"
```
Or add new column to csv file - `PERSON`. It is a reserved column name like `EMAIL` and `ATTACH`.

# 0.9.1
## Fixes
- `sentMessages` is null
- Class-Path of Manifest is wrong

# 0.9
## Fixes
- MIME type of attachment now detect more correctly
- Remove from public methods exceptions that depend from third libraries
- `NullPointerException` when use as console program (one of `badEmails` or `badParameters` is null)
- `NullPointerException` when use as console program (if `sentMessages` is null)
- fix `SenderParametersTest`

## Changes
- `Sender#getErrorSendMessages` was renamed to `#getNotSentMessages`

## Features
- Print error messages and settings to log
- Add option `-haltOnFailure`. Stop send if bad parameters exist.
- `Sender#sentMessages()` returns sent messages
- `MessageContent#toByteArray()` returns byte array of message in EML format
- Add Gradle build file 

	
# 0.8.14
## Fixes
- Name of attachment with no latin symbols
- File with attachments list read in specified tncoding (utf-8 as default)
- Fix conflict `EMLFile` and `subject` options. If `EMLFile` is specified, `subject` is not requared. Subject will be get from EML file.
- If specified more than one same emails, will be send one message.

## Features
- Print in log full path of processed files
- `Sender#getErrorSendMessages` returns not sent messages
- Set parameters to `MessageContent` by `Map` 
- `Sender#send` with `Map` parameters
- Create message content from EML file input stream 
	
