package com.pnk.record_management.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordResponse {

    String id;

    String medicalRecordName;

    String patientId;

    Instant creationDateTime;

    Instant latestUpdateDateTime;

    boolean s3Availability;

    String updatedByUser;

    MedicalRecordS3Metadata medicalRecordS3Metadata;

}
