package com.pnk.record_management.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.pnk.record_management.dto.response.MedicalRecordResponse;
import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import com.pnk.record_management.entity.MedicalRecord;
import com.pnk.record_management.repository.MedicalRecordRepository;
import lombok.experimental.NonFinal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@SpringBootTest
@ExtendWith(MockitoExtension.class)
@TestPropertySource("/test.properties")
class StorageServiceImplTest {

    @MockBean
    private AmazonS3 s3Client;

    @MockBean
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private StorageService storageService;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private S3Object s3Object;

    @NonFinal
    @Value("${application.bucket.name}")
    private String bucketName;

    ModelMapper modelMapper = new ModelMapper();

    MedicalRecordS3Metadata medicalRecordS3Metadata;
    MedicalRecord medicalRecord;
    MedicalRecordResponse medicalRecordResponse;

    @BeforeEach
    void setUp() {
        medicalRecordS3Metadata = MedicalRecordS3Metadata.builder()
                .bucketName("dummy-bucket-name")
                .key("test-EHR.txt")
                .etag("dummy-eTag")
                .size(6L)
                .lastModified(Date.from(LocalDate.of(2024, 8, 31).atStartOfDay().atZone(ZoneOffset.UTC).toInstant()))
                .storageClass("STANDARD")
                .owner(new Owner("0222", "Phong Vo"))
                .build();

        medicalRecord = MedicalRecord.builder()
                .id("dummyId")
                .medicalRecordName(medicalRecordS3Metadata.getKey())
                .patientId("dummyPatientId")
                .creationDateTime(LocalDate.of(2024, 8, 31).atStartOfDay().atZone(ZoneOffset.UTC).toInstant())
                .latestUpdateDateTime(LocalDate.of(2024, 9, 1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant())
                .s3Availability(true)
                .updatedByUser("dummyUser")
                .medicalRecordS3Metadata(medicalRecordS3Metadata)
                .build();

        medicalRecordResponse = modelMapper.map(medicalRecord, MedicalRecordResponse.class);
    }


    @Test
    void testSearchS3ExactFilename() {
    }


    @Test
    void testSearchS3ContainsFilename() {
    }


    /**
     * Method under test: {@link StorageServiceImpl#downloadFileFromS3(String)}
     */
    @Test
    void testDownloadFileFromS3_Success() {
        // Arrange
        String fileName = "testFile.txt";
        byte[] expectedContent = "test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(expectedContent);

        when(s3Client.getObject(bucketName, fileName)).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));

        // Act
        byte[] result = storageService.downloadFileFromS3(fileName);

        // Assert
        assertArrayEquals(expectedContent, result);
        verify(s3Client, times(1)).getObject(bucketName, fileName);
    }


    /**
     * Method under test: {@link StorageServiceImpl#downloadFileFromS3(String)}
     */
    @Test
    void testDownloadFileFromS3_S3Exception() {
        // Arrange
        String fileName = "testFile.txt";
        AmazonS3Exception s3Exception = new AmazonS3Exception("S3 error");
        when(s3Client.getObject(bucketName, fileName)).thenThrow(s3Exception);

        // Act & Assert
        AmazonS3Exception thrownException = assertThrows(AmazonS3Exception.class, () -> {
            storageService.downloadFileFromS3(fileName);
        });

        assertEquals("Failed to download file from S3 due to an S3 error", thrownException.getErrorMessage());
        verify(s3Client, times(1)).getObject(bucketName, fileName);
    }


    /**
     * Method under test: {@link StorageServiceImpl#downloadFileFromS3(String)}
     */
    @Test
    void testDownloadFileFromS3_IOError() throws IOException {
        // Arrange
        String fileName = "test-file.txt";
        S3Object s3Object = mock(S3Object.class);
        S3ObjectInputStream s3ObjectInputStream = mock(S3ObjectInputStream.class);

        when(s3Client.getObject(bucketName, fileName)).thenReturn(s3Object);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(s3ObjectInputStream.read(any(byte[].class))).thenThrow(IOException.class);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            storageService.downloadFileFromS3(fileName);
        });

        assertEquals("Failed to download file from S3 due to I/O error", thrown.getMessage());
        verify(s3Client, times(1)).getObject(bucketName, fileName);
    }


    /**
     * Method under test: {@link StorageServiceImpl#downloadFileFromS3(String)}
     */
    @Test
    void testDownloadFileFromS3_UnexpectedException() {
        // Arrange
        String fileName = "testFile.txt";
        when(s3Client.getObject(bucketName, fileName)).thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            storageService.downloadFileFromS3(fileName);
        });

        assertTrue(thrownException.getMessage().contains("Failed to download file from S3 due to an unexpected error"));
        verify(s3Client, times(1)).getObject(bucketName, fileName);
    }


    @Test
    void testDeleteFileFromS3() {
    }


    /**
     * Method under test: {@link StorageServiceImpl#uploadFileToS3(MultipartFile)}
     */
//    @Test
//    void testConvertMultiPartFileToFile_Success() throws IOException {
//        // Mock MultipartFile
//        String fileName = "testFile.txt";
//        String fileContent = "This is a test file content.";
//        multipartFile = new MockMultipartFile(
//                fileName, fileName,
//                "text/plain", fileContent.getBytes());
//
//        // Call the method to test
//        File resultFile = storageService.convertMultiPartFileToFile(multipartFile);
//
//        // Validate the results
//        assertNotNull(resultFile);
//        assertEquals(fileName, resultFile.getName());
//        assertTrue(resultFile.exists());
//        assertEquals(fileContent, new String(java.nio.file.Files.readAllBytes(resultFile.toPath())));
//
//        // Clean up
//        assertTrue(resultFile.delete());
//    }


    /**
     * Method under test: {@link StorageServiceImpl#convertMultiPartFileToFile(MultipartFile)}
     */
//    @Test
//    void testConvertMultiPartFileToFile_IOException() throws IOException {
//        // Mock MultipartFile
//        MultipartFile multipartFile = mock(MultipartFile.class);
//        when(multipartFile.getOriginalFilename())
//                .thenReturn("testFile.txt");
//        when(multipartFile.getBytes())
//                .thenThrow(new IOException("Simulated IOException"));
//
//        // Validate the IOException handling
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> storageService.convertMultiPartFileToFile(multipartFile));
//        assertInstanceOf(IOException.class, exception.getCause());
//    }


    /**
     * Method under test: {@link StorageServiceImpl#insertMedicalRecordInDB(MedicalRecordS3Metadata)}
     */
//    @Test
//    void testInsertMedicalRecordInDB() {
//        when(medicalRecordRepository.save(any(MedicalRecord.class)))
//                .thenReturn(medicalRecord);
//
//        assertEquals(medicalRecordResponse, storageService.insertMedicalRecordInDB(medicalRecordS3Metadata));
//    }


    /**
     * Method under test: {@link StorageServiceImpl#updateMedicalRecordExistenceStatusInDB(String)}
     */
//    @Test
//    void testUpdateMedicalRecordInDB() {
//        when(medicalRecordRepository.findByMedicalRecordName(anyString()))
//                .thenReturn(Optional.ofNullable(medicalRecord));
//        when(medicalRecordRepository.save(any(MedicalRecord.class)))
//                .thenReturn(medicalRecord);
//
//        MedicalRecordResponse updatedRecordResponse = storageService
//                .updateMedicalRecordExistenceStatusInDB(medicalRecord.getMedicalRecordName());
//
//        assertFalse(updatedRecordResponse.isS3Availability());
//        assertNull(updatedRecordResponse.getMedicalRecordS3Metadata());
//    }


//    @Test
//    void testUploadFile_Success() {
//        // Arrange
//        MultipartFile multipartFile = new MockMultipartFile(
//                "file", "test.txt", "text/plain", "Test Content".getBytes());
//
//        // Act
//        MedicalRecordResponse medicalRecordResponse = storageService.uploadFile(multipartFile);
//
//        // Assert
//        assertThat(medicalRecordResponse.getBucketName()).isEqualTo(bucketName);
//        assertThat(medicalRecordResponse.getMessage()).contains("File uploaded: ");
//        assertThat(medicalRecordResponse.getMessage()).contains("UTC_test.txt");
//        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class));
//    }
}