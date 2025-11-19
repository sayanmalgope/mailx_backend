package com.example.startspring.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        try {
            // Load the service account key from the classpath resources.
            InputStream serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("mail-fire-99b18") // Your Project ID from the prompt
                    .setStorageBucket("mail-fire-99b18.firebasestorage.app") // Your Storage Bucket
                    .build();

            // Initialize the app if it hasn't been initialized yet.
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("✅ Firebase Admin SDK has been initialized.");
            }
        } catch (IOException e) {
            logger.error("❌ Failed to initialize Firebase Admin SDK.", e);
        }
    }
}