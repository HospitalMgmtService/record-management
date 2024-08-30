package com.pnk.record_management.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.pnk.record_management.dto.response.RecordResponse;
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
import java.util.ArrayList;
import java.util.List;
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
    public RecordResponse uploadFile(MultipartFile file) {
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss_SSS");
        String timePrefix = utcNow.format(formatter);
        String fileName = timePrefix + "_" + utcNow.getZone() + "_" + file.getOriginalFilename();

        File fileObject = null;
        try {
            fileObject = convertMultiPartFileToFile(file);
            s3Client.putObject(new PutObjectRequest(bucketName, fileName, fileObject));

            // add uploaded file name to a database (NoSQL) for keeping management

            log.info(">> uploadFile >> File uploaded: {}", fileName);

            return RecordResponse.builder()
                    .bucketName(bucketName)
                    .message("File uploaded: " + fileName)
                    .build();
        } catch (Exception e) {
            log.error(">> uploadFile >> Error uploading file to S3", e);
            throw new RuntimeException("File upload failed");
        } finally {
            if (fileObject != null) {
                try {
                    Files.delete(fileObject.toPath());
                    log.info(">> uploadFile >> Temporary file deleted successfully");
                } catch (Exception e) {
                    log.warn(">> uploadFile >> Failed to delete temporary file", e);
                }
            }
        }
    }


    /*
     * key: is the saved file's filename
     * */
    @Override
    public List<RecordResponse> searchFilenameExact(String searchingWord) {
        // Create a request to list objects in the S3 bucket
        ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;

        // StringBuilder to collect matching filenames
        List<RecordResponse> matchingFiles = new ArrayList<>();

        do {
            // Get the next batch of objects from the bucket
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary summary : result.getObjectSummaries()) {
                String key = summary.getKey();
                if (key.equals(searchingWord))
                    matchingFiles.add(
                            RecordResponse.builder()
                                    .bucketName(bucketName)
                                    .message(key)
                                    .build()
                    );
            }
            // If there are more objects, get the next batch
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return matchingFiles;
    }


    @Override
    public List<RecordResponse> searchFilenameContains(String searchingWord) {
        // Create a request to list objects in the S3 bucket
        ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;

        // StringBuilder to collect matching filenames
        List<RecordResponse> matchingFiles = new ArrayList<>();

        do {
            // Get the next batch of objects from the bucket
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary summary : result.getObjectSummaries()) {
                String key = summary.getKey();
                if (key.contains(searchingWord)) {
                    matchingFiles.add(
                            RecordResponse.builder()
                                    .bucketName(bucketName)
                                    .message(key)
                                    .build()
                    );
                }
            }
            // If there are more objects, get the next batch
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return matchingFiles;
    }


    @Override
    public byte[] downloadFile(String fileName) {
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        try (S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent()) {
            byte[] content = IOUtils.toByteArray(s3ObjectInputStream);
            log.info(">> downloadFile >> File downloaded: {}", fileName);
            return content;
        } catch (AmazonS3Exception e) {
            log.error(">> downloadFile >> S3 error while downloading file: {}", fileName, e);
            throw new RuntimeException("Failed to download file from S3 due to S3 error", e);
        } catch (IOException e) {
            log.error(">> downloadFile >> I/O error while processing the file: {}", fileName, e);
            throw new RuntimeException("Failed to download file from S3 due to I/O error", e);
        } catch (Exception e) {
            log.error(">> downloadFile >> Unexpected error while downloading file: {}", fileName, e);
            throw new RuntimeException("Failed to download file from S3 due to an unexpected error", e);
        }
    }


    @Override
    public RecordResponse deleteFile(String fileName) {
        log.info(">> deleteFile >> File deleted: {}", fileName);

        List<RecordResponse> searchResultBeforeDeletion = searchFilenameExact(fileName);

        s3Client.deleteObject(bucketName, fileName);

        List<RecordResponse> searchResultAfterDeletion = searchFilenameExact(fileName);

        // remove uploaded file name from database (NoSQL) for keeping management

        if (!searchResultBeforeDeletion.isEmpty() && searchResultAfterDeletion.isEmpty()) {
            return RecordResponse.builder()
                    .message("File " + fileName + " was deleted successfully")
                    .build();
        }
        return RecordResponse.builder()
                .message("File " + fileName + " was failed to delete or not found.")
                .build();
    }


    private File convertMultiPartFileToFile(MultipartFile file) {
        log.info(">> convertMultiPartFileToFile::file: {}", file);

        File convertedFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(convertedFile)) {
            fileOutputStream.write(file.getBytes());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error(">> convertMultiPartFileToFile >> Error converting MultipartFile to File", e);
            throw new RuntimeException(e);
        }

        return convertedFile;
    }


    // add uploaded file name to a database (NoSQL) for keeping management

    // remove uploaded file name from database (NoSQL) for keeping management

}
