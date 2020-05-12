package com.hts.spring.controller;

import com.hts.demo.service.IDemoService;
import com.hts.spring.framwork.annotation.HTSAutowired;

public class HelloController {

    @HTSAutowired
    IDemoService demoService;

}
