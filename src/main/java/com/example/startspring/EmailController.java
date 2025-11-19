package com.example.startspring;

import com.example.startspring.service.FirebaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private FirebaseService firebaseService;

    @GetMapping("/")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Backend is running!");
    }

    @PostMapping("/send-emails-instant")
    public ResponseEntity<String> createEmailTask(
            @RequestParam("senderEmail") String senderEmail,
            @RequestParam("senderPassword") String senderPassword,
            @RequestParam("subject") String subject,
            @RequestParam("body") String htmlBody,
            @RequestParam("recipients") List<String> recipients,
            @RequestParam(value = "attachmentFile", required = false) MultipartFile attachmentFile) {

        logger.info("Received request for /send-emails-instant with {} recipients.", recipients.size());

        if (recipients == null || recipients.isEmpty()) {
            logger.error("Request failed: Recipient list is empty.");
            return ResponseEntity.badRequest().body("Recipient list cannot be empty.");
        }

        try {
            Date scheduleTime = new Date();

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("senderEmail", senderEmail);
            emailData.put("senderPassword", senderPassword);
            emailData.put("subject", subject);
            emailData.put("htmlBody", htmlBody);
            emailData.put("scheduleTime", scheduleTime);

            // --- THIS IS THE FIX ---
            // Pass the recipients list directly to the service.
            firebaseService.createEmailTask(emailData, recipients, attachmentFile);

            logger.info("Email task created successfully.");
            return ResponseEntity.ok("Email task created successfully. It will be processed shortly.");

        } catch (Exception e) {
            logger.error("❌ Internal server error while creating email task: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error creating email task: " + e.getMessage());
        }
    }
}