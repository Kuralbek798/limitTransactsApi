package com.example.limittransactsapi.util;

import jakarta.xml.bind.DatatypeConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
@Slf4j
@Service
public  class EncripionDecriptionUtil {


    public String getDecryptedAPIKey(String uuidKey, String encryptedPathword)throws Exception{
       try{
           SecretKey masterKey = generateKeyFromUUID(uuidKey);
           return decrypt(encryptedPathword, masterKey);

       }catch (Exception e){
           log.error("An arror ocured in the method getaPiKey {} ",e.getMessage());
           throw new ServiceException("An Error occurred in method getAPIKey: " + e);
       }
    }


    public static SecretKey generateKeyFromUUID(String uuid) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(uuid.getBytes("UTF-8")); // Генерация хеша SHA-256 из UUID
        return new SecretKeySpec(key, 0, 16, "AES"); // Создание ключа AES длиной 128 бит
    }

    public  String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return DatatypeConverter.printBase64Binary(encryptedData);
    }

    public  String decrypt(String encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedData = DatatypeConverter.parseBase64Binary(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData);
    }


}
