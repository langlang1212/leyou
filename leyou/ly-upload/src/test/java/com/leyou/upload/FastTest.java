package com.leyou.upload;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.domain.ThumbImageConfig;
import com.github.tobato.fastdfs.service.DefaultFastFileStorageClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FastTest {

    @Autowired
    private DefaultFastFileStorageClient storageClient;

    @Autowired
    private ThumbImageConfig thumbImageConfig;

    @Test
    public void testUpload() throws FileNotFoundException {
        File file = new File("G:\\JAVA\\timg.jpg");

        StorePath storePath = this.storageClient.uploadFile(
                new FileInputStream(file),file.length(),"jpg",null);

        System.out.println(storePath.getFullPath());

        System.out.println(storePath.getPath());
    }

    @Test
    public void  testUploadAndCreateThumb() throws FileNotFoundException {
        File file = new File("G:\\JAVA\\timg.jpg");

        StorePath storePath = this.storageClient.uploadImageAndCrtThumbImage(
                new FileInputStream(file),file.length(),"jpg",null);

        System.out.println(storePath.getFullPath());

        System.out.println(storePath.getPath());
    }
}
