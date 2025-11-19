package com.example.startspring.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {

    private static final String COLLECTION_NAME = "scheduledEmails";

    @Autowired
    private CloudinaryService cloudinaryService;

    public void createEmailTask(Map<String, Object> emailData, List<String> recipients, MultipartFile attachmentFile)
            throws IOException, ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();

        // --- THIS IS THE FIX ---
        // Convert the recipients list into a byte array representing the CSV content.
        String csvContent = String.join("\n", recipients);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        // Upload the byte arrays to Cloudinary
        String csvUrl = cloudinaryService.uploadFile(csvBytes, "csv_files", "recipients.csv");
        String attachmentUrl = (attachmentFile != null && !attachmentFile.isEmpty())
                ? cloudinaryService.uploadFile(attachmentFile.getBytes(), "attachments", attachmentFile.getOriginalFilename())
                : null;

        Map<String, Object> taskData = new HashMap<>(emailData);
        taskData.put("userId", "backend-user");
        taskData.put("csvUrl", csvUrl);
        taskData.put("attachmentUrl", attachmentUrl);
        taskData.put("status", "PENDING");
        taskData.put("createdAt", new Date());

        db.collection(COLLECTION_NAME).document().set(taskData).get();
    }
}