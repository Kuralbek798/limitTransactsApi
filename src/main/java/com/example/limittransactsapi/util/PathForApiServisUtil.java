package com.example.limittransactsapi.util;

import com.example.limittransactsapi.Entity.PathForApi;
import com.example.limittransactsapi.repository.PathForApiRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PathForApiServisUtil {


    private final PathForApiRepository pathForApiRepository;
    private final EncripionDecriptionUtil encripionDecriptionUtil;

    @Autowired
    public PathForApiServisUtil(PathForApiRepository pathForApiRepository, EncripionDecriptionUtil encripionDecriptionUtil) {
        this.pathForApiRepository = pathForApiRepository;
        this.encripionDecriptionUtil = encripionDecriptionUtil;
    }


    public String getDecryptedApiKey(String servisIdentity, String uuidKey) throws Exception {
        try {
            PathForApi encryptedPathword = pathForApiRepository.findByDescription(servisIdentity);
            return decryptPath(uuidKey, encryptedPathword.getEncryptedApiPath());

        } catch (Exception e) {
            log.error("an error occurred while in a temp to get data from repository in method getEncryptedPathForApi{}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    private String decryptPath(String uuidKey, String encryptedPathword) throws Exception {
        return encripionDecriptionUtil.getDecryptedAPIKey(uuidKey, encryptedPathword);
    }

}
