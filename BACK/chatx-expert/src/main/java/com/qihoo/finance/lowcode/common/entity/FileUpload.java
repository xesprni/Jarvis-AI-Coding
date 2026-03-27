package com.qihoo.finance.lowcode.common.entity;

import lombok.Data;

import java.util.Objects;

/**
 * FileUploadDTO
 *
 * @author fengjinfu-jk
 * date 2024/12/3
 * @version 1.0.0
 * @apiNote FileUploadDTO
 */
@Data
public class FileUpload {
    private String name;
    private String url;
    private String fullPath;

    public static final long MAX_FILE_SIZE = 1024 * 1024 * 5;
    public static final long MAX_FILE_COUNT = 10;

    public static FileUpload of(String fileName, String url, String fullPath) {
        FileUpload fileUpload = new FileUpload();
        fileUpload.setName(fileName);
        fileUpload.setUrl(url);
        fileUpload.setFullPath(fullPath);
        return fileUpload;
    }

    @Override
    public String toString() {
        return "FileUpload{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileUpload that = (FileUpload) o;
        return Objects.equals(fullPath, that.fullPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullPath);
    }
}
