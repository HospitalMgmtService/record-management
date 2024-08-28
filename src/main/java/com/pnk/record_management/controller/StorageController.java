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

import java.util.List;


@RestController
@RequestMapping("/files")
@RequiredArgsConstructor // injected by Constructor, no longer need of @Autowire
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StorageController {

    StorageServiceImpl storageService;


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam(value = "file") MultipartFile file) {
        log.info(">> uploadFile >> {}", file.getOriginalFilename());
        return new ResponseEntity<>(storageService.uploadFile(file), HttpStatus.OK);
    }


    @GetMapping("/search/{searchingWord}")
    public ResponseEntity<List<String>> searchFile(@PathVariable String searchingWord) {
        log.info(">> searchFile::searchingWord: {}", searchingWord);

        return ResponseEntity
                .ok()
                .body(storageService.searchFilesContains(searchingWord));
    }


    @GetMapping("/download/{fileName}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) {
        log.info(">> searchFile::downloadFile: {}", fileName);

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
            log.warn(">> downloadFile >> Filename {} not found on storage", fileName);

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }


    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        log.info(">> deleteFile::fileName: {}", fileName);

        storageService.deleteFile(fileName);

        log.info(">> deleteFile >> Filename {}", fileName);

        return ResponseEntity
                .ok()
                .body("File was deleted successfully.");
    }
}
