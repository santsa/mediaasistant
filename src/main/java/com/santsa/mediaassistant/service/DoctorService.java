package com.santsa.mediaassistant.service;

import java.util.List;

import com.santsa.mediaassistant.dto.DoctorInfo;

public interface DoctorService {

    public List<DoctorInfo> searchDoctors(String query);

}
