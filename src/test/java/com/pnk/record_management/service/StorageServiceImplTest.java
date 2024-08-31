package com.pnk.record_management.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.pnk.record_management.dto.response.MedicalRecordResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@SpringBootTest
@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @MockBean
    private AmazonS3 s3Client;

    @Autowired
    private StorageServiceImpl storageService;

    @Value("${application.bucket.name}")
    private String bucketName;


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