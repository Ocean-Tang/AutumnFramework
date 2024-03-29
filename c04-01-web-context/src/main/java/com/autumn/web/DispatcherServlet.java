package com.autumn.web;

import com.autumn.context.ApplicationContext;
import com.autumn.io.PropertiesResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;


/**
 * @author huangcanjie
 */
public class DispatcherServlet extends HttpServlet {

    final Logger logger = LoggerFactory.getLogger(getClass());
    ApplicationContext applicationContext;

    public DispatcherServlet(ApplicationContext applicationContext, PropertiesResolver propertiesResolver) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.info("{} {}", req.getMethod(), req.getRequestURI());
        PrintWriter writer = resp.getWriter();
        writer.write("<h1>Hello World</h1>");
        writer.flush();
    }
}
