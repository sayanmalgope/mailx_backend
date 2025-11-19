package com.example.startspring;

import com.example.startspring.service.EncryptionService;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final EncryptionService encryptionService;

    public SettingsController(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @PostMapping("/save")
    public Map<String, String> saveCredentials(
            @RequestParam("userId") String userId,
            @RequestParam("gmail") String gmail,
            @RequestParam("appPassword") String appPassword
    ) throws Exception {

        String encryptedPassword = encryptionService.encrypt(appPassword);

        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> data = new HashMap<>();
        data.put("gmail", gmail);
        data.put("appPassword", encryptedPassword);

        db.collection("settings").document(userId).set(data).get();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");

        return response;
    }
}
