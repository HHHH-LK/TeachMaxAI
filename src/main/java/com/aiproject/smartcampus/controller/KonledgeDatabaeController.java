package com.aiproject.smartcampus.controller;



import com.aiproject.smartcampus.model.rag.FileloadFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/konledgedatabase")
@RequiredArgsConstructor
@Slf4j
public class KonledgeDatabaeController {

    private final FileloadFunction fileloadFunction;

    @GetMapping("/load")
    public String load() {

        /*File pdfFile = new File("/Users/lk_hhh/Desktop/Thinking+in+Java+4th+Edition（JAVA编程思想 第四版 英文版）.pdf");
        log.info("documentsloade 接收到的 file: {}, exists: {}", pdfFile, pdfFile == null ? "null" : pdfFile.exists());*/
        fileloadFunction.documentsloade(null);
        return "加载成功";
    }


}
