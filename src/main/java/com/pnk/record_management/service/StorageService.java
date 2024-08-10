package com.pnk.record_management.service;

import org.springframework.web.multipart.MultipartFile;


public interface StorageService {

    String uploadFile(MultipartFile file);

    byte[] downloadFile(String fileName);

    public void deleteFile(String fileName);

}
