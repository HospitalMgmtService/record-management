package com.pnk.record_management.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Owner;
import com.pnk.record_management.dto.response.MedicalRecordResponse;
import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import com.pnk.record_management.entity.MedicalRecord;
import com.pnk.record_management.exception.AppException;
import com.pnk.record_management.exception.ErrorCode;
import com.pnk.record_management.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
class StorageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Mock
    private MultipartFile multipartFile;

    ModelMapper modelMapper = new ModelMapper();

    MedicalRecordS3Metadata medicalRecordS3Metadata;

    MedicalRecord medicalRecord;

    MedicalRecordResponse medicalRecordResponse;

    private String jwt;

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

        // access http://localhost:8888/api/v1/identity/auth/token   to re-generate auth token
        jwt = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJwaG9uZ3ZvLmNvbSIsInN1YiI6ImFkbWluIiwiZXhwIjoxNzI1MTQ2NDA2LCJpYXQiOjE3MjUxMzkyMDYsImp0aSI6ImIzMDRkMTg4LWJkODYtNDhhNC1hMGIwLWZiMDIzNGJkZTQzZCIsInNjb3BlIjoiUk9MRV9BRE1JTiBDUkVBVEVfUE9TVCBSRUpFQ1RfUE9TVCBBUFBST1ZFX1BPU1QifQ.jDQ_0hgdqsWIcydjeOBndjS7y9yGgQM6GpKZAYD0EBc";
    }


    /**
     * Method under test: {@link StorageController#uploadFile(MultipartFile)}
     */
    @Test
    void testUploadFileSuccess() throws Exception {
        when(storageService.uploadFileToS3(any()))
                .thenReturn(medicalRecordResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "testfile.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "sample content".getBytes()
        );

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("dummyId"))
                .andExpect(jsonPath("$.result.medicalRecordName").value("test-EHR.txt"))
                .andExpect(jsonPath("$.result.patientId").value("dummyPatientId"))
                .andExpect(jsonPath("$.result.s3Availability").value(true))
                .andExpect(jsonPath("$.result.updatedByUser").value("dummyUser"))
                .andExpect(jsonPath("$.result.medicalRecordS3Metadata.key").value("test-EHR.txt"))
                .andExpect(jsonPath("$.result.medicalRecordS3Metadata.owner.id").value("0222"))
                .andExpect(jsonPath("$.result.medicalRecordS3Metadata.owner.displayName").value("Phong Vo"))
        ;
    }


    /**
     * Method under test: {@link StorageController#uploadFile(MultipartFile)}
     */
    @Test
    void testUploadFileMultipartException() throws Exception {
        when(storageService.uploadFileToS3(any()))
                .thenThrow(new MultipartException("Multipart error"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "testfile.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "sample content".getBytes()
        );

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest()); // or another appropriate status
    }


    /**
     * Method under test: {@link StorageController#searchFileInS3(String)}
     */
    @Test
    void testSearchFileInS3() throws Exception {
        String searchingWord = "testfile.txt";
        when(storageService.searchS3ContainsFilename(searchingWord))
                .thenReturn(List.of(medicalRecordS3Metadata));

        mockMvc.perform(get("/files/search/" + searchingWord)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].bucketName").value("dummy-bucket-name"))
                .andExpect(jsonPath("$.result[0].key").value("test-EHR.txt"))
                .andExpect(jsonPath("$.result[0].etag").value("dummy-eTag"))
                .andExpect(jsonPath("$.result[0].size").value(6L))
                .andExpect(jsonPath("$.result[0].storageClass").value("STANDARD"))
                .andExpect(jsonPath("$.result[0].owner.id").value("0222"))
                .andExpect(jsonPath("$.result[0].owner.displayName").value("Phong Vo"))
        ;
    }


    /**
     * Method under test: {@link StorageController#downloadFile(String)}
     */
    @Test
    void testDownloadFileSuccess() throws Exception {
        // Prepare mock data
        String fileName = "testfile.txt";
        byte[] fileContent = "Sample file content".getBytes();

        // Mock the service call
        when(storageService.downloadFileFromS3(anyString())).thenReturn(fileContent);

        // Perform the request
        mockMvc.perform(get("/files/download/" + fileName)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-type", "application/octet-stream"))
                .andExpect(header().string("Content-disposition", "attachment; file=\"" + fileName + "\""))
                .andExpect(content().bytes(fileContent));
    }


    /**
     * Method under test: {@link StorageController#downloadFile(String)}
     */
    @Test
    void testDownloadFileNotFound() throws Exception {
        String fileName = "nonexistentfile.txt";

        // Mock the service call to throw an AmazonS3Exception for a file not found
        when(storageService.downloadFileFromS3(anyString()))
                .thenThrow(new AmazonS3Exception("File not found"));

        // Perform the request
        mockMvc.perform(get("/files/download/" + fileName)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    /**
     * Method under test: {@link StorageController#downloadFile(String)}
     */
    @Test
    void testDownloadFileUnexpectedError() throws Exception {
        String fileName = "unexpectedfile.txt";

        // Mock the service call to throw a RuntimeException for an unexpected error
        when(storageService.downloadFileFromS3(anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Perform the request
        mockMvc.perform(get("/files/download/" + fileName)
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }


    /**
     * Method under test: {@link StorageController#deleteFile(String)}
     */
    @Test
    void testDeleteFileSuccess() throws Exception {
        String targetFileName = "testfile.txt";

        medicalRecordResponse.setMedicalRecordS3Metadata(null);
        medicalRecordResponse.setS3Availability(false);

        when(storageService.deleteFileFromS3(anyString()))
                .thenReturn(medicalRecordResponse);

        mockMvc.perform(delete("/files/delete/" + targetFileName)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("dummyId"))
                .andExpect(jsonPath("$.result.medicalRecordName").value("test-EHR.txt"))
                .andExpect(jsonPath("$.result.patientId").value("dummyPatientId"))
                .andExpect(jsonPath("$.result.s3Availability").value(false))
                .andExpect(jsonPath("$.result.updatedByUser").value("dummyUser"))
        ;
    }


    /**
     * Method under test: {@link StorageController#deleteFile(String)}
     */
    @Test
    void testDeleteFileNotFound() throws Exception {
        // Mock the service call to throw an exception for not found
        when(storageService.deleteFileFromS3("nonexistentfile.txt"))
                .thenThrow(new AppException(ErrorCode.MEDICAL_RECORD_NOT_EXISTING));

        // Perform the request
        mockMvc.perform(delete("/files/delete/nonexistentfile.txt")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    /**
     * Method under test: {@link StorageController#deleteFile(String)}
     */
    @Test
    void testDeleteFileFailed() throws Exception {
        // Mock the service call to throw an exception for deletion failure
        when(storageService.deleteFileFromS3("errorfile.txt"))
                .thenThrow(new AppException(ErrorCode.FILE_DELETION_FAILED));

        // Perform the request
        mockMvc.perform(delete("/files/delete/errorfile.txt")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}