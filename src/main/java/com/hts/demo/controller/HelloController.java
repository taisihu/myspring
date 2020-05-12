package com.hts.demo.controller;


import com.hts.demo.service.IDemoService;
import com.hts.spring.framwork.annotation.HTSAutowired;
import com.hts.spring.framwork.annotation.HTSController;
import com.hts.spring.framwork.annotation.HTSRequestMapping;
import com.sun.deploy.net.HttpRequest;
import com.sun.deploy.net.HttpResponse;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@HTSController
@HTSRequestMapping("/demo")
public class HelloController {

    @HTSAutowired
    IDemoService iDemoService;

    @HTSRequestMapping("/hello")
    public void sayHello(HttpServletRequest request, HttpServletResponse response,String name,String id){
//        String id = (String)request.getAttribute("id");
//        String name = (String)request.getAttribute("name");
        String hello = iDemoService.hello(name, id);
        try {
            response.getWriter().write(hello);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
