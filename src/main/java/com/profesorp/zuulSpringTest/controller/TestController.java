package com.profesorp.zuulSpringTest.controller;

import java.io.IOException;
import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
	Logger log = LoggerFactory.getLogger(TestController.class); 
	@RequestMapping(path="/api")
	public String test(HttpServletRequest request)
	{
		StringBuffer strLog=new StringBuffer();
		strLog.append("................ RECIBIDA PETICION EN /api ......  </BR></br>");
		strLog.append("Metodo: "+request.getMethod()+"</BR>");
		strLog.append("URL: "+request.getRequestURL()+"</BR>");
		strLog.append("Host Remoto: "+request.getRemoteHost()+"</BR>");
		strLog.append("----- MAP ----</BR>");
		request.getParameterMap().forEach( (key,value) ->
		{
			for (int n=0;n<value.length;n++)
			{
				strLog.append("Clave:"+key+ " Valor: "+value[n]+"</BR>");
			}
		} );
		
		strLog.append("</BR>----- Headers ----</BR>");
		Enumeration<String> nameHeaders=request.getHeaderNames();				
		while (nameHeaders.hasMoreElements())
		{
			String name=nameHeaders.nextElement();
			Enumeration<String> valueHeaders=request.getHeaders(name);
			while (valueHeaders.hasMoreElements())
			{
				String value=valueHeaders.nextElement();
				strLog.append("Clave:"+name+ " Valor: "+value+"</BR>");
			}
		}
		try {
			strLog.append("</br>----- BODY ----</BR>");
			strLog.append( request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
		} catch (IOException e) {
			
		}
		log.info(strLog.toString());
		return "<html>"
				+" <title>Prueba de ZUUL</TITLE> <HEAD>"+
				strLog.toString()+
				"</HEAD>"
				+ "</html>"; 
	}
}
