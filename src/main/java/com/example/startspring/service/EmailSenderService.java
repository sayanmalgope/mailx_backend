package com.example.startspring.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailSenderService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);

    /**
     * Sends emails to a provided list of recipient email addresses with an optional attachment.
     * This is the method used by the FirebaseTaskProcessor.
     *
     * @param senderEmail The email address to send from.
     * @param senderPassword The 16-digit app password for the sender's account.
     * @param subject The subject of the email.
     * @param htmlBody The HTML content of the email body.
     * @param recipients A list of recipient email addresses.
     * @param attachmentFilePath The path to a temporary attachment file (can be null).
     * @return The number of successfully sent emails.
     */
    public int sendBulkEmailsWithList(String senderEmail, String senderPassword, String subject, String htmlBody, List<String> recipients, Path attachmentFilePath) {
        final AtomicInteger successCount = new AtomicInteger(0);

        // 1. Configure Gmail SMTP server properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // 2. Create a mail session with an authenticator
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        logger.info("Starting bulk email send process for {} recipients.", recipients.size());

        // 3. Iterate through each recipient and send an email
        for (String recipientEmail : recipients) {
            // Skip any invalid or empty email addresses
            if (recipientEmail == null || recipientEmail.trim().isEmpty() || !recipientEmail.contains("@")) {
                logger.warn("Skipping invalid recipient: {}", recipientEmail);
                continue;
            }
            try {
                // Create a new MimeMessage for each recipient
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail.trim()));
                message.setSubject(subject);

                // Create a multipart message to hold text and attachments
                Multipart multipart = new MimeMultipart();

                // Create the HTML body part
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setContent(htmlBody, "text/html; charset=utf-8");
                multipart.addBodyPart(textPart);

                // Add the attachment if a path is provided and the file exists
                if (attachmentFilePath != null && Files.exists(attachmentFilePath)) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    File file = attachmentFilePath.toFile();

                    // Set file name explicitly (Gmail will show this name)
                    attachmentPart.setFileName(file.getName());

                    // Set correct MIME type (important for Gmail preview)
                    attachmentPart.attachFile(file);
                    attachmentPart.setHeader("Content-Type", "application/pdf; name=\"" + file.getName() + "\"");
                    attachmentPart.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

                    multipart.addBodyPart(attachmentPart);
                }


                // Set the final content of the message and send it
                message.setContent(multipart);
                Transport.send(message);

                logger.info("✅ Email sent to: {}", recipientEmail);
                successCount.incrementAndGet();

            } catch (MessagingException | IOException e) {
                // Log errors for individual emails but continue the process
                logger.error("❌ Failed to send email to: {} - {}", recipientEmail, e.getMessage());
            }
        }

        logger.info("Bulk email process finished. Successfully sent {} out of {} emails.", successCount.get(), recipients.size());

        // The cleanup of the temporary files is handled by the calling method (FirebaseTaskProcessor)
        // to ensure they are deleted even if this service throws an exception.

        return successCount.get();
    }

    /**
     * This is the old method that reads recipients from a CSV file.
     * It is no longer used by the FirebaseTaskProcessor but is kept for reference.
     */
    public int sendBulkEmails(String senderEmail, String senderPassword, String subject, String htmlBody, Path csvFilePath, Path resumeFilePath) {
        logger.warn("The method 'sendBulkEmails' is deprecated and should not be used in the new workflow.");
        try {
            List<String> recipients = Files.readAllLines(csvFilePath);
            return sendBulkEmailsWithList(senderEmail, senderPassword, subject, htmlBody, recipients, resumeFilePath);
        } catch (IOException e) {
            logger.error("❌ Error reading deprecated CSV file path: {}", e.getMessage(), e);
            return 0;
        }
    }
}