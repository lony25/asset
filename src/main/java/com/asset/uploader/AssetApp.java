package com.asset.uploader;

import com.asset.uploader.initializer.AwsS3Initializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.asset.uploader"})
public class AssetApp{
    private static final Logger LOGGER = LoggerFactory.getLogger(AssetApp.class);

    public static void main(String[] args) {
        LOGGER.info("Starting app");
        SpringApplication.run(AssetApp.class, args);
    }

    @Bean(name = "awsS3Initializer")
    public AwsS3Initializer getAwsS3Initializer() {
        LOGGER.info("Initializing aws s3 client");
        return new AwsS3Initializer();
    }

}
