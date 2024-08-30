package com.pnk.record_management.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.pnk.record_management.dto.response.ApiResponse;
import com.pnk.record_management.dto.response.RecordResponse;
import com.pnk.record_management.service.StorageServiceImpl;
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

    StorageServiceImpl storageService;


    @PostMapping("/upload")
    public ApiResponse<RecordResponse> uploadFile(@RequestParam(value = "file") MultipartFile file) {
        log.info(">> uploadFile >> {}", file.getOriginalFilename());

        return ApiResponse.<RecordResponse>builder()
                .result(storageService.uploadFile(file))
                .build();
    }


    @GetMapping("/search/{searchingWord}")
    public ApiResponse<List<RecordResponse>> searchFile(@PathVariable String searchingWord) {
        log.info(">> searchFile::searchingWord: {}", searchingWord);

        return ApiResponse.<List<RecordResponse>>builder()
                .result(storageService.searchFilenameContains(searchingWord))
                .build();
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) {
        log.info(">> downloadFile::downloadFile: {}", fileName);

        try {
            byte[] data = storageService.downloadFile(fileName);
            ByteArrayResource resource = new ByteArrayResource(data);

            log.info(">> downloadFile >> {}", fileName);

            return ResponseEntity
                    .ok()
                    .contentLength(data.length)
                    .header("Content-type", "application/octet-stream")
                    .header("Content-disposition", "attachment; file=\"" + fileName + "\"")
                    .body(resource);
        } catch (AmazonS3Exception amazonS3Exception) {
            log.info(">> downloadFile >> Filename {} not found on storage", fileName);

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }


    @DeleteMapping("/delete/{fileName}")
    public ApiResponse<RecordResponse> deleteFile(@PathVariable String fileName) {
        log.info(">> deleteFile::fileName: {}", fileName);

        RecordResponse deletionResult = storageService.deleteFile(fileName);

        return ApiResponse.<RecordResponse>builder()
                .result(deletionResult)
                .build();
    }
}
