package com.example.startspring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

@Service
public class EmailSchedulingService {

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private EmailSenderService emailSenderService;

    public void scheduleEmail(String senderEmail, String senderPassword, String subject, String htmlBody, MultipartFile csvFile, MultipartFile resumeFile, Instant scheduleTime) {

        // Since the task runs later, we must save the files to a temporary location.
        // We cannot hold MultipartFile objects in memory.
        try {
            Path tempCsvFile = Files.createTempFile("recipients-", ".csv");
            Path tempResumeFile = Files.createTempFile("resume-", ".pdf");

            Files.copy(csvFile.getInputStream(), tempCsvFile, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(resumeFile.getInputStream(), tempResumeFile, StandardCopyOption.REPLACE_EXISTING);

            // Create a Runnable task
            Runnable emailTask = () -> emailSenderService.sendBulkEmails(
                    senderEmail, senderPassword, subject, htmlBody, tempCsvFile, tempResumeFile
            );

            // Schedule the task
            taskScheduler.schedule(emailTask, scheduleTime);
            System.out.println("✅ Task scheduled successfully for: " + scheduleTime.toString());

        } catch (IOException e) {
            System.err.println("❌ Could not create temporary files for scheduling. Error: " + e.getMessage());
            // In a real app, you'd throw an exception here to be handled by the controller.
        }
    }
}