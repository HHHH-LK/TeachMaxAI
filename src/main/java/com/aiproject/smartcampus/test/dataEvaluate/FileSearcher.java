package com.aiproject.smartcampus.test.dataEvaluate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileSearcher {
    public static void main(String[] args) {
        // 这里替换为你的父文件夹路径
        String parentFolderPath = "/Users/lk_hhh/Desktop/软件杯第一轮答辩材料/知识库资料/Java";

        // 搜索所有文件
        List<File> allFiles = searchAllFiles(parentFolderPath);

        // 打印搜索结果

        for (File file : allFiles) {
            System.out.println(file.getAbsolutePath());
        }
    }

    /**
     * 从指定的父文件夹中搜索所有文件（包括子文件夹中的文件）
     *
     * @param parentFolderPath 父文件夹路径
     * @return 包含所有文件的List集合
     */
    public static List<File> searchAllFiles(String parentFolderPath) {
        List<File> fileList = new ArrayList<>();
        File parentFolder = new File(parentFolderPath);

        // 检查父文件夹是否存在且是一个目录
        if (!parentFolder.exists() || !parentFolder.isDirectory()) {
            System.out.println("错误：指定的路径不存在或不是一个文件夹 - " + parentFolderPath);
            return fileList;
        }

        // 递归搜索文件
        searchFilesRecursively(parentFolder, fileList);
        return fileList;
    }

    /**
     * 递归搜索文件夹中的所有文件
     *
     * @param folder   要搜索的文件夹
     * @param fileList 存储找到的文件的集合
     */
    private static void searchFilesRecursively(File folder, List<File> fileList) {
        // 获取文件夹中的所有文件和子文件夹
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // 如果是文件，添加到集合中
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    // 如果是文件夹，递归搜索
                    searchFilesRecursively(file, fileList);
                }
            }
        } else {
            // 处理无法访问的文件夹（如权限问题）
            System.out.println("警告：无法访问文件夹 - " + folder.getAbsolutePath());
        }
    }
}