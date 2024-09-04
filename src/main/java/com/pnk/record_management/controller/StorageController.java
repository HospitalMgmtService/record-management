package com.pnk.record_management.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.pnk.record_management.dto.response.ApiResponse;
import com.pnk.record_management.dto.response.MedicalRecordResponse;
import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import com.pnk.record_management.service.StorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/files")
@RequiredArgsConstructor // injected by Constructor, no longer need of @Autowire
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StorageController {

    StorageService storageService;


    @PostMapping("/upload")
    public ApiResponse<MedicalRecordResponse> uploadFile(@RequestParam(value = "file") MultipartFile file) {
        log.info(">> uploadFile >> {}", file.getOriginalFilename());

        return ApiResponse.<MedicalRecordResponse>builder()
                .result(storageService.uploadFileToS3(file))
                .build();
    }


    @GetMapping("/search-s3/{searchingWord}")
    public ApiResponse<List<MedicalRecordS3Metadata>> searchFileInS3(@PathVariable String searchingWord) {
        log.info(">> searchFile::searchingWord: {}", searchingWord);

        return ApiResponse.<List<MedicalRecordS3Metadata>>builder()
                .result(storageService.searchS3ContainsFilename(searchingWord))
                .build();
    }


    @GetMapping("/search-db/{searchingWord}")
    public ApiResponse<List<MedicalRecordResponse>> searchFileInDB(@PathVariable String searchingWord) {
        log.info(">> searchFile::searchFileInDB: {}", searchingWord);

        return ApiResponse.<List<MedicalRecordResponse>>builder()
                .result(storageService.searchDatabaseExactFilename(searchingWord))
                .build();
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) {
        log.info(">> downloadFile::downloadFile: {}", fileName);

        try {
            byte[] data = storageService.downloadFileFromS3(fileName);
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity
                    .ok()
                    .contentLength(data.length)
                    .header("Content-type", "application/octet-stream")
                    .header("Content-disposition", "attachment; file=\"" + fileName + "\"")
                    .body(resource);
        } catch (AmazonS3Exception amazonS3Exception) {
            log.info(">> downloadFile >> Filename {} not found on S3", fileName);

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }


    @DeleteMapping("/delete/{fileName}")
    public ApiResponse<MedicalRecordResponse> deleteFile(@PathVariable String fileName) {
        log.info(">> deleteFile::fileName: {}", fileName);

        MedicalRecordResponse deletionResult = storageService.deleteFileFromS3(fileName);

        return ApiResponse.<MedicalRecordResponse>builder()
                .result(deletionResult)
                .build();
    }
}
