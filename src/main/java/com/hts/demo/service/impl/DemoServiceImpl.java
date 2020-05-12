package com.hts.demo.service.impl;

import com.hts.demo.service.IDemoService;
import com.hts.spring.framwork.annotation.HTSService;

@HTSService
public class DemoServiceImpl implements IDemoService {
    @Override
    public String hello(String name, String id) {
        return "my name is" + name +",my id is " + id;
    }
}
