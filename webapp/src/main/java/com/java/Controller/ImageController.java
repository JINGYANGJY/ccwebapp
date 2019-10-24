package com.java.Controller;

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
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

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Controller
public class ImageController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    ImageRepository imageRepository;

    Regions clientRegion = Regions.US_EAST_1;
    String bucketName = "webapp.shujiefan.me";
    String profile = "dev";

    @RequestMapping(value = "/v1/recipe/{id}/image", method = RequestMethod.POST, consumes = "multipart/form-data", produces = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    uploadImage(@RequestHeader(value = "Authorization") String auth, @RequestParam(value = "recipeImage", required = true) MultipartFile file, @PathVariable("id") String id) {
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        User user = userRepository.findUserByEmail(email);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        Recipe recipe = recipeRepository.findRecipeById(id);

        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + id + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        if (!file.getContentType().equals("image/png") &&  !file.getContentType().equals("image/jpg") && !file.getContentType().equals("image/jpeg")) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "File type not valid");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        if (recipe.getImage() != null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "There is already an image associated with this recipe");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        Image image = new Image();
        String imageId = UUID.randomUUID().toString();
        image.setId(imageId);
        image = uploadToS3(image, file);
        recipe.setImage(image);
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipe.setUpdatedTs(ft.format(dNow));
        recipeRepository.save(recipe);
        JSONObject jObject = new JSONObject();
        jObject.put("id", image.getId());
        jObject.put("url", image.getUrl());

        return ResponseEntity.status(HttpStatus.CREATED).
                body(jObject.toString());
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.GET, produces = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    getImage(@PathVariable("recipeId") String recipeId, @PathVariable("imageId") String imageId) {
        Recipe recipe = recipeRepository.findRecipeById(recipeId);

        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + recipeId + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }

        Image image = imageRepository.findImageById(imageId);
        if (image == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Image with id " + imageId + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }

        JSONObject jObject = new JSONObject();
        jObject.put("id", image.getId());
        jObject.put("url", image.getUrl());

        return ResponseEntity.status(HttpStatus.OK).
                body(jObject.toString());
    }

    @RequestMapping(value = "/v1/recipe/{recipeId}/image/{imageId}", method = RequestMethod.DELETE, produces = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    deleteImage(@RequestHeader(value = "Authorization") String auth, @PathVariable("recipeId") String recipeId, @PathVariable("imageId") String imageId) {
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        User user = userRepository.findUserByEmail(email);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        Recipe recipe = recipeRepository.findRecipeById(recipeId);
        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + recipeId + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }

        Image image = imageRepository.findImageById(imageId);
        if (image == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Image with id " + imageId + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        deleteImage(image);
        recipe.setImage(null);
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipe.setUpdatedTs(ft.format(dNow));
        recipeRepository.save(recipe);
        imageRepository.delete(image);

        JSONObject jObject = new JSONObject();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).
                body(jObject.toString());
    }

    private Image uploadToS3(Image image, MultipartFile file) {
        String fileObjKeyName = image.getId();

        try {
            ProfileCredentialsProvider sys = new ProfileCredentialsProvider(profile);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion).withCredentials(new AWSStaticCredentialsProvider(sys.getCredentials()))
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

    public void deleteImage(Image image) {
        String[] temp = image.getUrl().split("/");
        String keyName = image.getId();
        try {
            ProfileCredentialsProvider sys = new ProfileCredentialsProvider(profile);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion).withCredentials(new AWSStaticCredentialsProvider(sys.getCredentials()))
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
}
