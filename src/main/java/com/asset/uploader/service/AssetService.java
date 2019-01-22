package com.asset.uploader.service;

import com.asset.uploader.initializer.AwsS3Initializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@Component
public class AssetService extends AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetService.class);

    @Autowired
    @Qualifier("awsS3Initializer")
    private AwsS3Initializer awsS3Initializer;


    @RequestMapping(value = "/asset",method = RequestMethod.POST)
    public ResponseEntity<?> uploadAsset(
            @RequestParam("file") MultipartFile asset
            )
    {
        String asset_id;
        if(asset.isEmpty()){
            LOGGER.error("Invalid form data");
            return ResponseEntity.status(400).body("Invalid form data");
        }
        try{

            asset_id=saveAndUpload(asset);
        }
        catch (Exception e){
            LOGGER.error("Can not upload file on s3 bucket",e);
            return ResponseEntity.status(500).body("Can not upload file on s3 bucket");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id",asset_id);
        jsonObject.put("status","UPLOADED");
        jsonObject.put("message","OK");

        return ResponseEntity.ok().body(jsonObject.toString());
    }

    @RequestMapping(value = "/asset/{asset_id}",method = RequestMethod.GET)
    public ResponseEntity<?> getDownloadURL(
            @PathVariable String asset_id,
            @RequestParam(required = false) String timeout
    ) {
        long default_timeout=60;
        long t = timeout.isEmpty()?default_timeout:Long.parseLong(timeout);
        try {
            String downloadURL = awsS3Initializer.getDownloadURL(asset_id, t);
            return ResponseEntity.ok().body("downloadURL: " + downloadURL);
        }
        catch (Exception e){
            LOGGER.error("Could not connect to s3 bucket",e);
            return ResponseEntity.status(500).body("");
        }
    }

    @RequestMapping(value = "/asset/{asset_id}",method = RequestMethod.PUT)
    public ResponseEntity<?> archiveAsset(
            @PathVariable String asset_id,
            @RequestBody String requestBody
            )
    {
     try{
         awsS3Initializer.updateAssetStatus(asset_id, requestBody);
     }
     catch (Exception e){
         LOGGER.error("Could not update status of asset",e);
         return ResponseEntity.status(500).body("Could not update the status of the asset");
     }

     JSONObject jsonObject = new JSONObject();
     jsonObject.put("message","Ok");
     return ResponseEntity.ok().body(jsonObject.toString());
    }

    @RequestMapping(value = "/assetlist",method = RequestMethod.GET)
    public ResponseEntity<?> getAllAssets() {
        Map<String,Long> activeAssets=new HashMap<>();
        JSONArray jsonArray = new JSONArray();

        try {
            activeAssets = awsS3Initializer.getAllActiveAssets();
        }
        catch (Exception e){
            LOGGER.error("Could not query s3 bucket");
            ResponseEntity.status(500).body("Could not query s3 bucket for assets");
        }
        if(!activeAssets.isEmpty()){
            for(String key:activeAssets.keySet()){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(key,activeAssets.get(key));
                jsonArray.put(jsonObject);
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("assets",jsonArray);
        jsonObject.put("message","OK");
        return ResponseEntity.ok().body(jsonObject.toString());
    }

    private String saveAndUpload(MultipartFile asset) {
        try {
            byte[] bytes = asset.getBytes();
            String rootPath = System.getProperty("catalina.home");
            File dir = new File(rootPath + File.separator + "tmpFiles");
            if (!dir.exists())
                dir.mkdirs();
            String name = Long.toString(new Date().toInstant().getEpochSecond());

            // Create the file on server
            File serverFile = new File(dir.getAbsolutePath()
                    + File.separator + name);
            BufferedOutputStream stream = new BufferedOutputStream(
                    new FileOutputStream(serverFile));
            stream.write(bytes);
            stream.close();

            LOGGER.info("file stored at:"+serverFile.getAbsolutePath());
            awsS3Initializer.uploadFileToS3Bucket(serverFile);
            serverFile.delete();
            return serverFile.getName();
        }
        catch (Exception e){
            LOGGER.error("Could not save file on server.",e);
            throw new RuntimeException("Server error",e);
        }
    }

}
