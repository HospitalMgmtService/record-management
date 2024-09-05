package com.pnk.record_management.service;

import com.pnk.record_management.dto.response.MedicalRecordResponse;
import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface StorageService {

    MedicalRecordResponse uploadFileToS3(MultipartFile file);

    List<MedicalRecordS3Metadata> searchS3ExactFilename(String fileName);

    List<MedicalRecordS3Metadata> searchS3ContainsFilename(String fileName);

    List<MedicalRecordResponse> searchDatabaseExactFilename(String fileName);

    List<MedicalRecordResponse> searchDatabaseContainingFilename(String fileName);

    byte[] downloadFileFromS3(String fileName);

    MedicalRecordResponse deleteFileFromS3(String fileName);

}
