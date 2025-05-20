package com.aiproject.smartcampus.exception;

/**
 * @program: SmartCampus
 * @description: rag
 * @author: lk
 * @create: 2025-05-19 18:14
 **/

public class RagExpection extends  RuntimeException{

    public RagExpection(String message) {
        super(message);
    }

    public RagExpection() {}

}
