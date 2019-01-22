package com.asset.uploader.initializer;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AwsS3Initializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3Initializer.class);

    @Value("${aws.s3.access.key}")
    private String accessKey;

    @Value("${aws.s3.secret.key}")
    private String secretKey;


    @Value("${aws.s3.bucket}")
    private String bucket;

    AmazonS3 amazonS3Client;

    @PostConstruct
    public void init(){
        LOGGER.info("Initialize aws s3 client");
        BasicAWSCredentials awsCredential = new BasicAWSCredentials(
                accessKey,
                secretKey
        );

        try {
            this.amazonS3Client = AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredential))
                    .withRegion(Regions.US_EAST_2)
                    .build();
        }
        catch (Exception e){
            LOGGER.error("Could not get aws client from credentials");
            this.amazonS3Client=null;
        }

    }

    public void uploadFileToS3Bucket(File asset){
        try {
            this.amazonS3Client.putObject(
                    bucket,
                    "Active/" + asset.getName(),
                    asset);
        }
        catch (Exception e){
            throw new RuntimeException("Could not upload to s3 bucket",e);
        }
    }

    public String getDownloadURL(String asset_id, long timeout){
        if(!this.amazonS3Client.doesObjectExist(bucket,"Active/"+asset_id))
            return null;

        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * timeout;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, asset_id)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);
        URL url = this.amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);

        return url.toString();
    }

    public void updateAssetStatus(String asset_id,String requestBody){
        JSONObject jsonObject = new JSONObject(requestBody);

        if(jsonObject.get("status").equals("ARCHIVED")){
            this.amazonS3Client.copyObject(
                    bucket,
                    "Active/"+asset_id,
                    bucket,
                    "Archived/"+asset_id
            );
            this.amazonS3Client.deleteObject(bucket,"Active/"+asset_id);
        }
    }

    public Map<String,Long> getAllActiveAssets(){
        Map<String,Long> activeAssets = new HashMap<>();
        ObjectListing objectListing = this.amazonS3Client.listObjects(bucket);
        for(S3ObjectSummary os : objectListing.getObjectSummaries()){
            if(os.getKey().startsWith("Active")){
                activeAssets.put(os.getKey().substring("Active/".length()),
                        os.getLastModified().getTime());
            }
        }
        return activeAssets;
    }

}
