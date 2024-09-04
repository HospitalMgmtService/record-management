package com.pnk.record_management.entity;

import com.pnk.record_management.dto.response.MedicalRecordS3Metadata;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("medical-record")
public class MedicalRecord {

    @Id
    String id;

    String medicalRecordName;

    String patientId;

    Instant creationDateTime;

    Instant latestUpdateDateTime;

    boolean s3Availability;

    String updatedByUser;

    MedicalRecordS3Metadata medicalRecordS3Metadata;

}
