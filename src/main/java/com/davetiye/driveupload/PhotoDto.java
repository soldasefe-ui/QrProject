package com.davetiye.driveupload;

public class PhotoDto {

    private String fileName;
    private String imageUrl;

    public PhotoDto(String fileName, String imageUrl) {
        this.fileName = fileName;
        this.imageUrl = imageUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}