package com.aiproject.smartcampus.controller;



import com.aiproject.smartcampus.model.rag.FileloadFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/konledgedatabase")
@RequiredArgsConstructor
public class KonledgeDatabaeController {

    private final FileloadFunction fileloadFunction;

    @GetMapping("/load")
    public String load() {
        fileloadFunction.documentsloade(null);
        return "加载成功";
    }


}
