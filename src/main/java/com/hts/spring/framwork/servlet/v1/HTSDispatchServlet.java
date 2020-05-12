package com.hts.spring.framwork.servlet.v1;

import com.hts.spring.framwork.annotation.HTSAutowired;
import com.hts.spring.framwork.annotation.HTSController;
import com.hts.spring.framwork.annotation.HTSRequestMapping;
import com.hts.spring.framwork.annotation.HTSService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class HTSDispatchServlet extends HttpServlet {

    //初始化IoC容器
    private Map<String,Object> ioc = new HashMap<String,Object>();

    private Properties contextConfig  =  new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String,Method> handlerMapping = new HashMap<String,Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp){
        //运行阶段
        //运行阶段，根据url获取相关的执行操作
        try {
            doDispatch(req,resp);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 not found");
        }
        Method method = this.handlerMapping.get(url);
        Map<String, String[]> parameterMap = req.getParameterMap();
        //反射调用method，第一个参数是method所在的实例，第二个参数是形参
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,resp,parameterMap.get("name")[0],parameterMap.get("id")[0]});
    }

    //初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2 扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3 初始化扫描到的类，并创建实例保存到IoC容器中
        doInstance();

        //4 完成DI依赖注入，自动赋值
        doAutowired();

        //====== mvc ======
        //初始化handlerMapping
        doInitHandlerMapping();

    }


    private void doAutowired() {

        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            //拿到IoC容器所有实例的属性
            //private protected public defalut

            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();

            for(Field field : declaredFields){
                //只注入加了@HTSAutowired的属性
                if(!field.isAnnotationPresent(HTSAutowired.class)){
                    continue;
                }

                HTSAutowired autowired = field.getAnnotation(HTSAutowired.class);
                String beanName = autowired.getValue();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }

                field.setAccessible(true);

                try {
                    //filed是实例中被注入的属性 相当于 @HTSAutowired IDemoService iDemoService;
                    //entry.getValue()是被注入属性的类
                    //ioc.get(beanName)是去IoC容器中获取key为com.hts.demo.service.IDemoService的实例
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        }

    }

    private void doInitHandlerMapping() {

        if(ioc.isEmpty()){
            return;
        }


        for(Map.Entry<String,Object> entry : ioc.entrySet()){

            Class<?> clazz = entry.getValue().getClass();

            if(!clazz.isAnnotationPresent(HTSController.class)){
                continue;
            }

            String baseUrl = "";
            if(clazz.isAnnotationPresent(HTSRequestMapping.class)){
                HTSRequestMapping requestMapping = clazz.getAnnotation(HTSRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //只需要获取public的方法
            Method[] methods = entry.getValue().getClass().getMethods();

            for(Method method : methods){
                //没有加路径的方法不需要扫描
                if(!method.isAnnotationPresent(HTSRequestMapping.class)){
                    continue;
                }

                HTSRequestMapping requestMapping = method.getAnnotation(HTSRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("mapped "+url +"," +method);

            }

        }

    }

    private void doInstance() {

        if(classNames.isEmpty()){
            return;
        }

        for(String className : classNames){
            try {
                Class<?> clazz = Class.forName(className);

                if(clazz.isAnnotationPresent(HTSController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                }else if(clazz.isAnnotationPresent(HTSService.class)){
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //获取注解上自定义的beanName
                    HTSService service = clazz.getAnnotation(HTSService.class);
                    if(!"".equals(service.value)){
                       beanName = service.value;
                    }

                    Object instance = clazz.newInstance();
                    for(Class<?> i : clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("bean " + i.getName() +" is already exists!");
                        }
                        ioc.put(i.getName(),instance);
                    }
                    ioc.put(beanName, instance);
                }else{
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //类明首字母小写
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        //小写字母的ascii码比大写字母ascii码大32
        chars[0] += 32;
        return String.valueOf(chars);

    }

    private void doScanner(String scanPackage) {
        //去classPath下找相关的.class
        //scanPackage包路径就是对应的文件夹
        URL resource = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classPath =  new File(resource.getFile());
        for(File file : classPath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                //取反减少代码嵌套
                if(!file.getName().endsWith(".class")){
                   continue;
                }
                //可以再实例化阶段调用Class.forName(className)利用反射创建实例，反射利用类的全限定名
                String className = scanPackage + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        //从classpath下去查找Sprig的配置文件
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != resourceAsStream){
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
