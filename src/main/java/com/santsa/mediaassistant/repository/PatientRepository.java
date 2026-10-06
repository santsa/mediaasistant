package com.santsa.mediaassistant.repository;

import com.santsa.mediaassistant.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

}
