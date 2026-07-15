package com.davetiye.driveupload;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class UploadController {

    private final GoogleDriveService driveService;
    private final QRCodeService qrCodeService;

    // Constructor: Her iki servisi de burada başlatıyoruz
    public UploadController(GoogleDriveService driveService, QRCodeService qrCodeService) {
        this.driveService = driveService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/yukle")
    public String showUploadPage(@RequestParam("id") String folderId,
                                 @RequestParam(value = "isim", required = false) String coupleName,
                                 Model model) {
        model.addAttribute("folderId", folderId);
        model.addAttribute("coupleName", coupleName);
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("files") List<MultipartFile> files,
                                   @RequestParam("folderId") String folderId,
                                   @RequestParam(value = "isim", required = false) String coupleName,
                                   RedirectAttributes redirectAttributes) {

        // 1. Dosya kontrolü
        if (files == null || files.isEmpty() || (files.size() == 1 && files.get(0).isEmpty())) {
            return "redirect:/yukle?id=" + folderId +
                    (coupleName != null ? "&isim=" + coupleName : "") +
                    "&status=hata";
        }

        int successCount = 0;
        int errorCount = 0;

        // 2. Döngü ile yükleme
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileId = driveService.uploadFileToFolder(file, folderId);
                if (fileId != null) {
                    successCount++;
                } else {
                    errorCount++;
                }
            }
        }

        // 3. Durum koduna göre yönlendirme
        String status = (successCount > 0 && errorCount == 0) ? "basarili" : "hata";

        return "redirect:/yukle?id=" + folderId +
                (coupleName != null ? "&isim=" + coupleName : "") +
                "&status=" + status;
    }

    @GetMapping("/qr-uret-sayfasi")
    public String showQrGenerationPage() {
        return "qr-uret";
    }

    @GetMapping(value = "/qr-uret", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getQRCode(@RequestParam("id") String folderId,
                                            @RequestParam("isim") String coupleName) {

        String encodedName = java.net.URLEncoder.encode(coupleName, java.nio.charset.StandardCharsets.UTF_8);
        String targetUrl = "https://qrproject.up.railway.app/yukle?id=" + folderId + "&isim=" + encodedName;

        byte[] qrCodeBytes = qrCodeService.generateQRCodeImage(targetUrl, 350, 350);

        if (qrCodeBytes != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodeBytes);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/album")
    public String showAlbum(@RequestParam("id") String folderId,
                            Model model) {

        model.addAttribute("photos", driveService.listPhotos(folderId));
        model.addAttribute("folderId", folderId);

        // FolderId'yi okunabilir isme çevir
        String coupleName = folderId
                .replace("-", " & ")
                .replace("_", " & ");

        // Her kelimenin ilk harfini büyüt
        StringBuilder formatted = new StringBuilder();

        for (String part : coupleName.split(" ")) {

            if (part.equals("&")) {
                formatted.append("& ");
                continue;
            }

            formatted.append(
                    part.substring(0,1).toUpperCase()
                            + part.substring(1).toLowerCase()
                            + " "
            );
        }

        model.addAttribute("coupleName", formatted.toString().trim());

        return "album";
    }
    @GetMapping("/album/download")
    public ResponseEntity<byte[]> downloadAlbum(
            @RequestParam("id") String folderId) {

        byte[] zip = driveService.downloadFolderAsZip(folderId);

        if (zip == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + folderId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }
}