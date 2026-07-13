package com.davetiye.driveupload;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UploadController {

    private final GoogleDriveService driveService;
    private final QRCodeService qrCodeService;

    public UploadController(GoogleDriveService driveService, QRCodeService qrCodeService) {
        this.driveService = driveService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/yukle")
    public String showUploadPage(@RequestParam("id") String folderId,
                                 @RequestParam(value = "isim", required = false) String coupleName,
                                 @RequestParam(value = "foto", required = false) String fotoUrl,
                                 Model model) {
        model.addAttribute("folderId", folderId);
        model.addAttribute("coupleName", coupleName);
        model.addAttribute("fotoUrl", fotoUrl);
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("folderId") String folderId,
                                   @RequestParam(value = "isim", required = false) String coupleName,
                                   RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lütfen bir fotoğraf seçin!");
            return "redirect:/yukle?id=" + folderId + (coupleName != null ? "&isim=" + coupleName : "");
        }

        String fileId = driveService.uploadFileToFolder(file, folderId);

        if (fileId != null) {
            redirectAttributes.addFlashAttribute("message", "Harika! Fotoğraf başarıyla yüklendi. ✨");
        } else {
            redirectAttributes.addFlashAttribute("error", "Yükleme hatası! Lütfen tekrar deneyin.");
        }

        return "redirect:/yukle?id=" + folderId + (coupleName != null ? "&isim=" + coupleName : "");
    }

    // YÖNETİM PANELİ İÇİN: QR Kod Üretme
    // Bu sayfa form içerir
    @GetMapping("/qr-uret-sayfasi")
    public String showQrGenerationPage() {
        return "qr-uret"; // resources/templates/qr-uret.html
    }

    // QR Kodun resmini döndüren kısım
    @GetMapping(value = "/qr-uret", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getQRCode(@RequestParam("id") String folderId,
                                            @RequestParam("isim") String coupleName) {

        // URL'deki boşlukları veya özel karakterleri tarayıcının anlayacağı forma çevirelim
        String encodedName = java.net.URLEncoder.encode(coupleName, java.nio.charset.StandardCharsets.UTF_8);

        String targetUrl = "https://qrproject.up.railway.app/yukle?id=" + folderId + "&isim=" + encodedName;

        byte[] qrCodeBytes = qrCodeService.generateQRCodeImage(targetUrl, 350, 350);

        if (qrCodeBytes != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodeBytes);
        }
        return ResponseEntity.badRequest().build();
    }
}