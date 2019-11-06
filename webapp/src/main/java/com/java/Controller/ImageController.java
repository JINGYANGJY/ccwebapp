package com.java.Controller;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.Md5Utils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java.DAO.ImageRepository;
import com.java.DAO.RecipeRepository;
import com.java.DAO.UserRepository;
import com.java.POJO.Image;
import com.java.POJO.Recipe;
import com.java.POJO.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.boot.Metadata;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import static com.java.JavaApplication.statsDClient;
import static com.java.JavaApplication.LOGGER;

@Controller
public class ImageController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    ImageRepository imageRepository;

    public static Regions clientRegion = Regions.fromName(System.getenv("AWS_REGION"));
    public static String bucketName = System.getenv("S3_IMAGE_BUCKET_NAME");
    public static String awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
    public static String awsSecretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY");

    @RequestMapping(value = "/v1/recipe/{id}/image", method = RequestMethod.POST, consumes = "multipart/form-data", produces = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    uploadImage(@RequestHeader(value = "Authorization") String auth, @RequestParam(value = "recipeImage", required = true) MultipartFile file, @PathVariable("id") String id) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.image.http.post");
        LOGGER.info("image.post: Upload Image");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        long findUserStart = System.currentTimeMillis();
        User user = userRepository.findUserByEmail(email);
        recordTime("endpoint.image.http.post.query.findUserByEmail", findUserStart);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            recordTime("endpoint.image.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        long findRecipeStart = System.currentTimeMillis();
        Recipe recipe = recipeRepository.findRecipeById(id);
        recordTime("endpoint.image.http.post.query.findRecipeById", findRecipeStart);

        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + id + " does not exist");
            recordTime("endpoint.image.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        if (!file.getContentType().equals("image/png") &&  !file.getContentType().equals("image/jpg") && !file.getContentType().equals("image/jpeg")) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "File type not valid");
            recordTime("endpoint.image.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        if (recipe.getImage() != null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "There is already an image associated with this recipe");
            recordTime("endpoint.image.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        Image image = new Image();
        String imageId = UUID.randomUUID().toString();
        image.setId(imageId);
        long s3Start = System.currentTimeMillis();
        image = uploadToS3(image, file);
        recordTime("endpoint.image.http.post.s3.upload", s3Start);
        recipe.setImage(image);
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipe.setUpdatedTs(ft.format(dNow));
        long saveRecipeStart = System.currentTimeMillis();
        recipeRepository.save(recipe);
        recordTime("endpoint.image.http.post.query.save.recipe", saveRecipeStart);
        JSONObject jObject = new JSONObject();
        jObject.put("id", image.getId());
        jObject.put("url", image.getUrl());

        recordTime("endpoint.image.http.post", startTime);
        return ResponseEntity.status(HttpStatus.CREATED).
                body(jObject.toString());
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    getImage(@PathVariable("recipeId") String recipeId, @PathVariable("imageId") String imageId) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.image.http.get");
        LOGGER.info("image.get: Get Image info");

        long findRecipeStart = System.currentTimeMillis();
        Recipe recipe = recipeRepository.findRecipeById(recipeId);
        recordTime("endpoint.image.http.get.query.findRecipeById", findRecipeStart);

        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + recipeId + " does not exist");
            recordTime("endpoint.image.http.get", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }

        long findImageStart = System.currentTimeMillis();
        Image image = imageRepository.findImageById(imageId);
        recordTime("endpoint.image.http.get.query.findImageById", findImageStart);
        if (image == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Image with id " + imageId + " does not exist");
            recordTime("endpoint.image.http.get", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }

        JSONObject jObject = new JSONObject();
        jObject.put("id", image.getId());
        jObject.put("url", image.getUrl());

        recordTime("endpoint.image.http.get", startTime);
        return ResponseEntity.status(HttpStatus.OK).
                body(jObject.toString());
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.DELETE, produces = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    deleteImage(@RequestHeader(value = "Authorization") String auth, @PathVariable("recipeId") String recipeId, @PathVariable("imageId") String imageId) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.image.http.delete");
        LOGGER.info("image.delete: Delete Image");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        long findUserStart = System.currentTimeMillis();
        User user = userRepository.findUserByEmail(email);
        recordTime("endpoint.image.http.delete.query.findUserByEmail", findUserStart);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            recordTime("endpoint.image.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        long findRecipeStart = System.currentTimeMillis();
        Recipe recipe = recipeRepository.findRecipeById(recipeId);
        recordTime("endpoint.image.http.delete.query.findRecipeById", findRecipeStart);
        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + recipeId + " does not exist");
            recordTime("endpoint.image.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }

        long findImageStart = System.currentTimeMillis();
        Image image = imageRepository.findImageById(imageId);
        recordTime("endpoint.image.http.delete.query.findImageById", findImageStart);
        if (image == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Image with id " + imageId + " does not exist");
            recordTime("endpoint.image.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        long s3Start = System.currentTimeMillis();
        deleteImage(image);
        recordTime("endpoint.image.http.delete.s3.delete", s3Start);
        recipe.setImage(null);
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipe.setUpdatedTs(ft.format(dNow));
        long saveRecipeStart = System.currentTimeMillis();
        recipeRepository.save(recipe);
        recordTime("endpoint.image.http.delete.query.save.recipe", saveRecipeStart);
        long deleteImageStart = System.currentTimeMillis();
        imageRepository.delete(image);
        recordTime("endpoint.image.http.delete.query.delete.image", deleteImageStart);

        JSONObject jObject = new JSONObject();
        recordTime("endpoint.image.http.delete", startTime);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).
                body(jObject.toString());
    }

    private Image uploadToS3(Image image, MultipartFile file) {
        String fileObjKeyName = image.getId();

        try {
            AWSCredentials creds = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion).withCredentials(new AWSStaticCredentialsProvider(creds))
                    .build();

            // Upload a file as a new object with ContentType and title specified.
            File convFile = new File(System.getProperty("java.io.tmpdir")+"/" + file.getOriginalFilename());
            file.transferTo(convFile);
            ObjectMetadata metadata = new ObjectMetadata();
            FileInputStream fis = new FileInputStream(convFile);
            byte[] content_bytes = IOUtils.toByteArray(fis);
            String md5 = new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(content_bytes)));

            metadata.setContentMD5(md5);
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(convFile.length());
            PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, convFile).withMetadata(metadata);
            s3Client.putObject(request);
            String url = s3Client.getUrl(bucketName, fileObjKeyName).toString();

            ObjectMetadata mdata = s3Client.getObjectMetadata(bucketName, fileObjKeyName);
//            image.setMd5(mdata.getContentMD5());
            image.setMd5(md5);
            image.setLastModifiedTime(mdata.getLastModified().toString());
            image.setContentLength(mdata.getContentLength());
            image.seteTag(mdata.getETag());
            image.setUrl(url);

            convFile.deleteOnExit();
            convFile.delete();
            return image;
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void deleteImage(Image image) {
        String[] temp = image.getUrl().split("/");
        String keyName = image.getId();
        try {
            AWSCredentials creds = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion).withCredentials(new AWSStaticCredentialsProvider(creds))
                    .build();

            s3Client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
    }

    public boolean Authentication(User user, String password) {
        if (user == null) return false;
        String stored_hash = user.getPassword();
        if (BCrypt.checkpw(password, stored_hash)) {
            return true;
        } else {
            return false;
        }
    }

    public void recordTime(String name, Long startTime) {
        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime(name, endTime - startTime);
    }
}
