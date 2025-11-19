package com.example.startspring.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseTaskProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseTaskProcessor.class);
    private static final String COLLECTION_NAME = "scheduledEmails";

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private EncryptionService encryptionService;

    @Scheduled(fixedRate = 60000)
    public void processScheduledEmails() {
        logger.info("Polling Firestore for due email tasks...");
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "PENDING")
                .whereLessThanOrEqualTo("scheduleTime", new Date())
                .get();
        try {
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            if (documents.isEmpty()) {
                logger.info("No due tasks found.");
                return;
            }
            logger.info("Found {} due tasks to process.", documents.size());
            for (QueryDocumentSnapshot document : documents) {
                processSingleTask(document);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("❌ Error fetching tasks from Firestore.", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processSingleTask(QueryDocumentSnapshot document) {
        String docId = document.getId();
        Firestore db = FirestoreClient.getFirestore();
        Path tempCsvFile = null;
        Path tempAttachmentFile = null;

        try {
            db.collection(COLLECTION_NAME).document(docId).update("status", "PROCESSING").get();
            logger.info("Processing task: {}", docId);

            Map<String, Object> data = document.getData();
            String senderEmail = (String) data.get("senderEmail");
            String encryptedPassword = (String) data.get("senderPassword");

            // --- DIAGNOSTIC LOGGING ---
            // Check if the credentials exist in the Firestore document.
            if (senderEmail == null || senderEmail.isEmpty() || encryptedPassword == null || encryptedPassword.isEmpty()) {
                logger.error("❌ CRITICAL: Task {} is missing 'senderEmail' or 'senderPassword' from Firestore!", docId);
                // Throw an exception to stop processing this task and mark it as an error.
                throw new IllegalStateException("Missing credentials in Firestore task document. Please re-save settings on the frontend.");
            } else {
                logger.info("Found credentials for task {}. Sender: {}", docId, senderEmail);
            }

            String decryptedPassword = encryptionService.decrypt(encryptedPassword);

            String subject = (String) data.get("subject");
            String htmlBody = (String) data.get("htmlBody");
            String csvUrl = (String) data.get("csvUrl");
            String attachmentUrl = (String) data.get("attachmentUrl");

            tempCsvFile = Files.createTempFile("recipients-" + docId + "-", ".csv");
            try (InputStream in = new URL(csvUrl).openStream()) {
                Files.copy(in, tempCsvFile, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Successfully downloaded CSV for task {}", docId);

            if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
                tempAttachmentFile = Files.createTempFile("attachment-" + docId + "-", ".tmp");
                try (InputStream in = new URL(attachmentUrl).openStream()) {
                    Files.copy(in, tempAttachmentFile, StandardCopyOption.REPLACE_EXISTING);
                }
                logger.info("Successfully downloaded attachment for task {}", docId);
            }

            List<String> recipients = Files.readAllLines(tempCsvFile);

            int sentCount = emailSenderService.sendBulkEmailsWithList(
                    senderEmail, decryptedPassword, subject, htmlBody, recipients, tempAttachmentFile
            );

            db.collection(COLLECTION_NAME).document(docId).update(
                    "status", "SENT",
                    "completedAt", new Date(),
                    "sentCount", sentCount
            ).get();
            logger.info("✅ Task {} completed successfully. Emails sent: {}", docId, sentCount);

        } catch (Exception e) {
            logger.error("❌ Error processing task {}: {}", docId, e.getMessage(), e);
            try {
                db.collection(COLLECTION_NAME).document(docId).update(
                        "status", "ERROR",
                        "errorMessage", e.getMessage()
                ).get();
            } catch (InterruptedException | ExecutionException updateEx) {
                logger.error("❌ CRITICAL: Failed to update task {} status to ERROR.", docId, updateEx);
            }
        } finally {
            cleanupTempFiles(tempCsvFile, tempAttachmentFile);
        }
    }

    private void cleanupTempFiles(Path... paths) {
        for (Path path : paths) {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                    logger.info("📁 Deleted temporary file: {}", path.getFileName());
                } catch (IOException e) {
                    logger.warn("⚠️ Failed to delete temporary file: {}", path, e);
                }
            }
        }
    }
}