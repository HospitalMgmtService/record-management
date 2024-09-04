package com.pnk.record_management.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.pnk.record_management.dto.response.MedicalRecordResponse;
import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import com.pnk.record_management.entity.MedicalRecord;
import com.pnk.record_management.exception.AppException;
import com.pnk.record_management.exception.ErrorCode;
import com.pnk.record_management.repository.MedicalRecordRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.pnk.record_management.utils.JwtUtils.extractDataFromJWT;


@Service
@RequiredArgsConstructor // injected by Constructor, no longer need of @Autowire
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StorageServiceImpl implements StorageService {

    com.pnk.record_management.service.DateTimeFormatter dateTimeFormatter;

    MedicalRecordRepository medicalRecordRepository;

    private final AmazonS3 s3Client;

    @NonFinal
    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @NonFinal
    @Value("${application.bucket.name}")
    private String bucketName;

    ModelMapper modelMapper = new ModelMapper();


    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public MedicalRecordResponse uploadFileToS3(MultipartFile file) {
        Instant utcNow = ZonedDateTime.now(ZoneId.of("UTC")).toInstant();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss_SSS");
        String timePrefix = utcNow.atZone(ZoneId.of("UTC")).format(formatter);
        String fileName = timePrefix + "_" + utcNow.getEpochSecond() + "_" + file.getOriginalFilename();

        File fileObject = null;
        try {
            log.info(">> uploadFileToS3 >> Uploading file: {} on S3", fileName);

            fileObject = convertMultiPartFileToFile(file);
            s3Client.putObject(new PutObjectRequest(bucketName, fileName, fileObject));

            // after uploading, search with exact fileName to get metadata of the file in S3
            MedicalRecordS3Metadata s3Metadata = searchS3ExactFilename(fileName).getFirst();

            // add uploaded file name to a database (MongoDB) for management purpose
            MedicalRecordResponse medicalRecordResponse = insertMedicalRecordInDB(s3Metadata);
            medicalRecordResponse.setMedicalRecordS3Metadata(s3Metadata);
            medicalRecordResponse.setElapsedCreationTime(
                    dateTimeFormatter.format(medicalRecordResponse.getCreationDateTime()));
            medicalRecordResponse.setElapsedUpdateTime(
                    dateTimeFormatter.format(medicalRecordResponse.getLatestUpdateDateTime()));

            return medicalRecordResponse;
        } catch (MultipartException e) {
            log.error(">> uploadFileToS3 >> Multipart request error: {}", e.getMessage());
            throw new IllegalArgumentException("Current request is not a multipart request", e);
        } catch (Exception e) {
            log.error(">> uploadFileToS3 >> Error uploading file to S3", e);
            throw new RuntimeException("File upload failed");
        } finally {
            if (fileObject != null) {
                try {
                    Files.delete(fileObject.toPath());
                    log.info(">> uploadFileToS3 >> Temporary file deleted successfully");
                } catch (Exception e) {
                    log.warn(">> uploadFileToS3 >> Failed to delete temporary file", e);
                }
            }
        }
    }


    /*
     * key: is the file's filename existing in S3
     * */
    @Override
    public List<MedicalRecordS3Metadata> searchS3ExactFilename(String searchingWord) {
        log.info(">> searchS3ExactFilename ....");

        // Create a request to list objects in the S3 bucket
        ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;

        List<MedicalRecordS3Metadata> matchingFiles = new ArrayList<>();

        do {
            // Get the next batch of objects from the bucket
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary summary : result.getObjectSummaries()) {
//                log.info(">> searchS3ExactFilename::summary: {}", summary);
                String key = summary.getKey();
                if (key.equals(searchingWord)) {
//                    log.info(">> searchS3ExactFilename::key {}", key);
                    matchingFiles.add(
                            MedicalRecordS3Metadata.builder()
                                    .bucketName(summary.getBucketName())
                                    .key(summary.getKey())
                                    .etag(summary.getETag())
                                    .size(summary.getSize())
                                    .lastModified(summary.getLastModified())
                                    .storageClass(summary.getStorageClass())
                                    .owner(summary.getOwner())
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
    public List<MedicalRecordS3Metadata> searchS3ContainsFilename(String searchingWord) {
        log.info(">> searchS3ContainsFilename ....");

        // Create a request to list objects in the S3 bucket
        ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;

        List<MedicalRecordS3Metadata> matchingFiles = new ArrayList<>();

        do {
            // Get the next batch of objects from the bucket
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary summary : result.getObjectSummaries()) {
//                log.info(">> searchS3ContainsFilename::summary: {}", summary);
                String key = summary.getKey();
                if (key.contains(searchingWord)) {
//                    log.info(">> searchS3ContainsFilename::key {}", key);
                    matchingFiles.add(
                            MedicalRecordS3Metadata.builder()
                                    .bucketName(summary.getBucketName())
                                    .key(summary.getKey())
                                    .etag(summary.getETag())
                                    .size(summary.getSize())
                                    .lastModified(summary.getLastModified())
                                    .storageClass(summary.getStorageClass())
                                    .owner(summary.getOwner())
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
    public List<MedicalRecordResponse> searchDatabaseExactFilename(String searchingWord) {
        var retrievedFile = medicalRecordRepository.findByMedicalRecordName(searchingWord)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        MedicalRecordResponse medicalRecordResponse = modelMapper.map(retrievedFile, MedicalRecordResponse.class);
        medicalRecordResponse.setElapsedCreationTime(
                dateTimeFormatter.format(medicalRecordResponse.getCreationDateTime()));
        medicalRecordResponse.setElapsedUpdateTime(
                dateTimeFormatter.format(medicalRecordResponse.getLatestUpdateDateTime()));

        log.info(">> searchDatabaseExactFilename::medicalRecordResponse {}", medicalRecordResponse);

        return List.of(medicalRecordResponse);
    }


    @Override
    public List<MedicalRecordResponse> searchDatabaseContainsFilename(String searchingWord) {
        var retrievedFile = medicalRecordRepository.findByMedicalRecordNameContains(searchingWord)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        MedicalRecordResponse medicalRecordResponse = modelMapper.map(retrievedFile, MedicalRecordResponse.class);
        medicalRecordResponse.setElapsedCreationTime(
                dateTimeFormatter.format(medicalRecordResponse.getCreationDateTime()));

        log.info(">> searchDatabaseContainsFilename::medicalRecordResponse {}", medicalRecordResponse);

        return List.of(medicalRecordResponse);
    }


    @Override
    public byte[] downloadFileFromS3(String fileName) {
        try (S3Object s3Object = s3Client.getObject(bucketName, fileName);
             S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent()) {
            byte[] content = IOUtils.toByteArray(s3ObjectInputStream);
            log.info(">> downloadFileFromS3 >> File downloaded: {}", fileName);
            return content;
        } catch (AmazonS3Exception e) {
            log.error(">> downloadFileFromS3 >> S3 error while downloading file: {}", fileName, e);
            throw new AmazonS3Exception("Failed to download file from S3 due to an S3 error", e);
        } catch (IOException e) {
            log.error(">> downloadFileFromS3 >> I/O error while processing the file: {}", fileName, e);
            throw new RuntimeException("Failed to download file from S3 due to I/O error", e);
        } catch (Exception e) {
            log.error(">> downloadFileFromS3 >> Unexpected error while downloading file: {}", fileName, e);
            throw new RuntimeException("Failed to download file from S3 due to an unexpected error", e);
        }
    }


    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public MedicalRecordResponse deleteFileFromS3(String fileName) {
        log.info(">> deleteFile >> Deleting file: {} on S3", fileName);

        // Check if the file exists in S3 before deletion
        List<MedicalRecordS3Metadata> searchResultBeforeDeletion = searchS3ExactFilename(fileName);
        if (searchResultBeforeDeletion.isEmpty())
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_EXISTING);

        // Delete the file from the S3 bucket
        s3Client.deleteObject(bucketName, fileName);

        // Check if the file was successfully deleted from S3
        List<MedicalRecordS3Metadata> searchResultAfterDeletion = searchS3ExactFilename(fileName);

        if (!searchResultAfterDeletion.isEmpty())
            throw new AppException(ErrorCode.FILE_DELETION_FAILED);

        // update existence status in MongoDB for management purposes
        MedicalRecordResponse savedMedicalRecordResponse = updateMedicalRecordExistenceStatusInDB(fileName);
        savedMedicalRecordResponse.setElapsedCreationTime(
                dateTimeFormatter.format(savedMedicalRecordResponse.getCreationDateTime()));
        savedMedicalRecordResponse.setElapsedUpdateTime(
                dateTimeFormatter.format(savedMedicalRecordResponse.getLatestUpdateDateTime()));

        log.info(">> deleteFileFromS3 >> File {} deleted successfully from S3 and its correspondent database entry updated.", fileName);

        // Return a response indicating successful deletion
        return savedMedicalRecordResponse;
    }


    private File convertMultiPartFileToFile(MultipartFile file) {
        log.info(">> convertMultiPartFileToFile::file: {}", file);

        File convertedFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(convertedFile)) {
            fileOutputStream.write(file.getBytes());
        } catch (IOException e) {
            log.error(">> convertMultiPartFileToFile >> Error converting MultipartFile to File", e);
            throw new RuntimeException(e);
        }

        return convertedFile;
    }


    // add uploaded file name to a database (MongoDB) for management
    private MedicalRecordResponse insertMedicalRecordInDB(MedicalRecordS3Metadata s3Metadata) {
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .id(UUID.randomUUID().toString())
                .medicalRecordName(s3Metadata.getKey())
                .patientId("get patientId sent from the producer")
                .creationDateTime(s3Metadata.getLastModified().toInstant())
                .latestUpdateDateTime(s3Metadata.getLastModified().toInstant())
                .s3Availability(true)
                .updatedByUser(extractDataFromJWT().get("name").toString())
                .build();

        log.info(">> insertMedicalRecordInDB::medicalRecord in DB: {}", medicalRecord);

        medicalRecord = medicalRecordRepository.save(medicalRecord);

        return modelMapper.map(medicalRecord, MedicalRecordResponse.class);
    }


    private MedicalRecordResponse updateMedicalRecordExistenceStatusInDB(String fileName) {
        MedicalRecord savedMedicalRecord = medicalRecordRepository
                .findByMedicalRecordName(fileName)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_EXISTING));

        savedMedicalRecord.setLatestUpdateDateTime(Instant.now());
        savedMedicalRecord.setS3Availability(false);
        savedMedicalRecord.setUpdatedByUser(extractDataFromJWT().get("name").toString());

        log.info(">> updateMedicalRecordExistenceStatusInDB >> savedMedicalRecord in DB: {}", savedMedicalRecord);

        savedMedicalRecord = medicalRecordRepository.save(savedMedicalRecord);

        return modelMapper.map(savedMedicalRecord, MedicalRecordResponse.class);
    }
}
