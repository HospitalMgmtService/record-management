package com.pnk.record_management.repository;

import com.pnk.record_management.entity.MedicalRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface MedicalRecordRepository extends MongoRepository<MedicalRecord, String> {

    Optional<MedicalRecord> findByMedicalRecordName(String medicalRecordName);

    Optional<MedicalRecord> findByMedicalRecordNameContains(String medicalRecordName);
}
