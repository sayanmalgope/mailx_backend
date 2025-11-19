package com.example.startspring.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Uploads file content (as a byte array) to a specific folder in Cloudinary,
     * preserving a given filename.
     *
     * @param fileBytes The raw bytes of the file to upload.
     * @param folderName The name of the folder in Cloudinary.
     * @param originalFilename The desired filename to save in Cloudinary.
     * @return The secure URL of the uploaded file.
     * @throws IOException If the upload fails.
     */
    public String uploadFile(byte[] fileBytes, String folderName, String originalFilename) throws IOException {

        Map<String, Object> options = ObjectUtils.asMap(
                "resource_type", "raw",
                "folder", folderName,
                "public_id", originalFilename, // Use the provided filename as the public_id
                "overwrite", true
        );

        // Upload the byte array with the specified options.
        Map<?, ?> uploadResult = cloudinary.uploader().upload(fileBytes, options);

        return (String) uploadResult.get("secure_url");
    }
}