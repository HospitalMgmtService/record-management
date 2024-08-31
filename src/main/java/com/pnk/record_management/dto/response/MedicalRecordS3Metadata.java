package com.pnk.record_management.dto.response;

import com.amazonaws.services.s3.model.Owner;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordS3Metadata {

    String bucketName;

    String key;

    String eTag;

    long size;

    Date lastModified;

    String storageClass;

    Owner owner;

}
