package com.pnk.record_management.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface StorageService {

    String uploadFile(MultipartFile file);

    List<String> searchFilenameExact(String searchingWord);

    List<String> searchFilenameContains(String searchingWord);

    byte[] downloadFile(String fileName);

    boolean deleteFile(String fileName);

}
