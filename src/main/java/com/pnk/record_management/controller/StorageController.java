package com.pnk.record_management.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.pnk.record_management.service.StorageServiceImpl;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/files")
@RequiredArgsConstructor // injected by Constructor, no longer need of @Autowire
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StorageController {

    StorageServiceImpl storageService;


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam(value = "file") MultipartFile file) {
        log.info("StorageController >> uploadFile >> {}", file.getOriginalFilename());
        return new ResponseEntity<>(storageService.uploadFile(file), HttpStatus.OK);
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) {
        try {
            byte[] data = storageService.downloadFile(fileName);
            ByteArrayResource resource = new ByteArrayResource(data);

            log.info("StorageController >> downloadFile >> {}", fileName);

            return ResponseEntity
                    .ok()
                    .contentLength(data.length)
                    .header("Content-type", "application/octet-stream")
                    .header("Content-disposition", "attachment; file=\"" + fileName + "\"")
                    .body(resource);
        } catch (AmazonS3Exception amazonS3Exception) {
            log.warn("StorageController >> downloadFile >> Filename {} not found on storage", fileName);

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }


    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        storageService.deleteFile(fileName);

        log.info("StorageController >> deleteFile >> Filename {}", fileName);

        return ResponseEntity
                .ok()
                .body("File was deleted successfully.");
    }
}
