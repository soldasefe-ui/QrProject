package com.davetiye.driveupload;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UploadController {

    private final GoogleDriveService driveService;
    private final QRCodeService qrCodeService; // Yeni ekledik

    // Constructor'a qrCodeService'i de dahil ettik
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
        model.addAttribute("fotoUrl", fotoUrl); // Yeni ekledik
        return "upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("folderId") String folderId,
                                   RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lütfen bir fotoğraf seçin!");
            return "redirect:/yukle?id=" + folderId;
        }

        String fileId = driveService.uploadFileToFolder(file, folderId);

        if (fileId != null) {
            redirectAttributes.addFlashAttribute("message", "Harika! Fotoğraf başarıyla yüklendi. ✨");
        } else {
            redirectAttributes.addFlashAttribute("error", "Yükleme hatası! Lütfen tekrar deneyin.");
        }

        return "redirect:/yukle?id=" + folderId;
    }
    @org.springframework.web.bind.annotation.GetMapping("/qr-uret")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> getQRCode(@RequestParam("id") String folderId,
                                                                     @RequestParam("isim") String coupleName) {
        // İleride buraya gerçek sitenin linki gelecek. Şimdilik yönlendirmeyi ayarlıyoruz.
        // Linkin sonuna hem klasör ID'sini hem de Türkçe karakter riski taşımayan ismi ekliyoruz
        String targetUrl = "http://localhost:8080/yukle?id=" + folderId + "&isim=" + coupleName;

        byte[] qrCodeBytes = qrCodeService.generateQRCodeImage(targetUrl, 350, 350);

        if (qrCodeBytes != null) {
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                    .body(qrCodeBytes);
        }
        return org.springframework.http.ResponseEntity.badRequest().build();
    }
}