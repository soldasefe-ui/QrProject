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
    public String handleFileUpload(@RequestParam("files") List<MultipartFile> files, // Listeye çevirdik
                                   @RequestParam("folderId") String folderId,
                                   @RequestParam(value = "isim", required = false) String coupleName,
                                   RedirectAttributes redirectAttributes) {

        if (files == null || files.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lütfen en az bir fotoğraf seçin!");
            return "redirect:/yukle?id=" + folderId + (coupleName != null ? "&isim=" + coupleName : "");
        }

        // Döngüye alıp hepsini tek tek yüklüyoruz
        for (MultipartFile file : files) {
            driveService.uploadFileToFolder(file, folderId);
        }

        redirectAttributes.addFlashAttribute("message", "Tüm fotoğraflar başarıyla yüklendi! ✨");

        return "redirect:/yukle?id=" + folderId + (coupleName != null ? "&isim=" + coupleName : "");
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
}