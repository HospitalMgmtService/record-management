package com.pnk.record_management.service;

import com.pnk.record_management.dto.response.MedicalRecordResponse;
import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;


@Repository
public interface StorageService {

    MedicalRecordResponse uploadFileToS3(MultipartFile file);

    List<MedicalRecordS3Metadata> searchS3ExactFilename(String searchingWord);

    List<MedicalRecordS3Metadata> searchS3ContainsFilename(String searchingWord);

    byte[] downloadFileFromS3(String fileName);

    MedicalRecordResponse deleteFileFromS3(String fileName);

}
