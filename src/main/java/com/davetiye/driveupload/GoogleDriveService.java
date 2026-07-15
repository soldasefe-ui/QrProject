package com.davetiye.driveupload;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleDriveService {

    @Value("${firebase.bucket-name}")
    private String bucketName;

    private Storage storage;

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount;

            // Railway ortamında mıyız diye kontrol et
            String firebaseJson = System.getenv("FIREBASE_KEY_JSON");

            if (firebaseJson != null && !firebaseJson.trim().isEmpty()) {
                // BULUT: Railway ortamı için değişkeni oku
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
            System.err.println("Firebase başlatılamadı: " + e.getMessage());
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
    public List<String> listFileNames(String folderId) {
        List<String> fileNames = new ArrayList<>();
        String prefix = folderId + "/";
        for (Blob blob : storage.list(bucketName, Storage.BlobListOption.prefix(prefix)).iterateAll()) {
            if (!blob.getName().equals(prefix)) {
                fileNames.add(blob.getName());
            }
        }
        return fileNames;
    }
    public List<PhotoDto> listPhotos(String folderId) {

        List<PhotoDto> photos = new ArrayList<>();

        String prefix = folderId + "/";

        for (Blob blob : storage.list(bucketName,
                Storage.BlobListOption.prefix(prefix)).iterateAll()) {

            if (blob.getName().equals(prefix)) {
                continue;
            }

            String encodedName = URLEncoder.encode(blob.getName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");

            String imageUrl =
                    "https://firebasestorage.googleapis.com/v0/b/"
                            + bucketName
                            + "/o/"
                            + encodedName
                            + "?alt=media";

            photos.add(new PhotoDto(
                    blob.getName().replace(prefix, ""),
                    imageUrl
            ));
        }

        return photos;
    }
    public byte[] downloadFolderAsZip(String folderId) {

        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ZipOutputStream zipOut = new ZipOutputStream(baos);

            String prefix = folderId + "/";

            for (Blob blob : storage.list(bucketName,
                    Storage.BlobListOption.prefix(prefix)).iterateAll()) {

                if (blob.getName().equals(prefix)) {
                    continue;
                }

                String fileName = blob.getName().replace(prefix, "");

                zipOut.putNextEntry(new ZipEntry(fileName));

                zipOut.write(blob.getContent());

                zipOut.closeEntry();

            }

            zipOut.finish();

            zipOut.close();

            return baos.toByteArray();

        }

        catch (Exception e){

            e.printStackTrace();

            return null;

        }

    }
}