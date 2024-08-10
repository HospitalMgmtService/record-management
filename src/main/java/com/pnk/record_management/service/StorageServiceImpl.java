package com.pnk.record_management.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


@Service
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final AmazonS3 s3Client;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${application.bucket.name}")
    private String bucketName;

    public StorageServiceImpl(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }


    @Override
    public String uploadFile(MultipartFile file) {
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss_SSS");
        String timePrefix = utcNow.format(formatter);
        String fileName = timePrefix + "_" + utcNow.getZone() + "_" + file.getOriginalFilename();

        File fileObject = convertMultiPartFileToFile(file);
        try {
            s3Client.putObject(new PutObjectRequest(bucketName, fileName, fileObject));
            return "File uploaded: " + fileName;
        } catch (Exception e) {
            log.error("Error uploading file to S3", e);
            throw new RuntimeException("File upload failed");
        } finally {
            try {
                Files.delete(fileObject.toPath());
                log.info("Temporary file deleted successfully");
            } catch (Exception e) {
                log.warn("Failed to delete temporary file", e);
            }
        }
    }


    @Override
    public byte[] downloadFile(String fileName) {
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        try (S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent()) {
            byte[] content = IOUtils.toByteArray(s3ObjectInputStream);
            return content;
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }


    @Override
    public void deleteFile(String fileName) {
        s3Client.deleteObject(bucketName, fileName);
        log.info("File uploaded: {}", fileName);
    }


    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(convertedFile)) {
            fileOutputStream.write(file.getBytes());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Error converting MultipartFile to File", e);
            throw new RuntimeException(e);
        }

        return convertedFile;
    }

}
