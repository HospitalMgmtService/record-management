package com.pnk.record_management.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface StorageService {

    String uploadFile(MultipartFile file);

    List<String> searchFilesContains(String searchingWord);

    byte[] downloadFile(String fileName);

    void deleteFile(String fileName);

}
