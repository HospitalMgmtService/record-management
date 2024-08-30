package com.pnk.record_management.service;

import com.pnk.record_management.dto.response.RecordResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface StorageService {

    RecordResponse uploadFile(MultipartFile file);

    List<RecordResponse> searchFilenameExact(String searchingWord);

    List<RecordResponse> searchFilenameContains(String searchingWord);

    byte[] downloadFile(String fileName);

    RecordResponse deleteFile(String fileName);

}
