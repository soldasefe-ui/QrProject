package com.davetiye.driveupload;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleDriveService {

    @Value("${firebase.bucket-name}")
    private String bucketName;

    private Storage storage;

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount;

            // Railway/Sunucu ortamında mıyız diye kontrol et
            String firebaseJson = System.getenv("FIREBASE_KEY_JSON");

            if (firebaseJson != null && !firebaseJson.isEmpty()) {
                // BULUT: Railway'den gelen değişkeni oku
                serviceAccount = new ByteArrayInputStream(firebaseJson.getBytes(StandardCharsets.UTF_8));
            } else {
                // YEREL: resources klasöründeki dosyayı oku
                ClassPathResource resource = new ClassPathResource("firebase-key.json");
                serviceAccount = resource.getInputStream();
            }

            StorageOptions storageOptions = StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            this.storage = storageOptions.getService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String uploadFileToFolder(MultipartFile file, String folderId) {
        try {
            String uniqueFileName = folderId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            BlobId blobId = BlobId.of(bucketName, uniqueFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());
            return uniqueFileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}