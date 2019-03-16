package com.profesorp.zuulSpringTest.controller;

import java.io.BufferedReader;
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
	final  static String SALTOLINEA="\n";
	
	Logger log = LoggerFactory.getLogger(TestController.class); 
	@RequestMapping(path="/api")
	public String test(HttpServletRequest request)
	{
		StringBuffer strLog=new StringBuffer();
		
		strLog.append("................ RECIBIDA PETICION EN /api ......  "+SALTOLINEA);
		strLog.append("Metodo: "+request.getMethod()+SALTOLINEA);
		strLog.append("URL: "+request.getRequestURL()+SALTOLINEA);
		strLog.append("Host Remoto: "+request.getRemoteHost()+SALTOLINEA);
		strLog.append("----- MAP ----"+SALTOLINEA);
		request.getParameterMap().forEach( (key,value) ->
		{
			for (int n=0;n<value.length;n++)
			{
				strLog.append("Clave:"+key+ " Valor: "+value[n]+SALTOLINEA);
			}
		} );
		
		strLog.append(SALTOLINEA+"----- Headers ----"+SALTOLINEA);
		Enumeration<String> nameHeaders=request.getHeaderNames();				
		while (nameHeaders.hasMoreElements())
		{
			String name=nameHeaders.nextElement();
			Enumeration<String> valueHeaders=request.getHeaders(name);
			while (valueHeaders.hasMoreElements())
			{
				String value=valueHeaders.nextElement();
				strLog.append("Clave:"+name+ " Valor: "+value+SALTOLINEA);
			}
		}
		try {
			strLog.append(SALTOLINEA+"----- BODY ----"+SALTOLINEA);
			BufferedReader reader= request.getReader();
			if (reader!=null)
			{
				char[] linea= new char[100];
				int nCaracteres;
				while  ((nCaracteres=reader.read(linea,0,100))>0)
				{				
					strLog.append( linea);
					
					if (nCaracteres!=100)
						break;
				} 
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		log.info(strLog.toString());
		
		return SALTOLINEA+"---------- Prueba de ZUUL ------------"+SALTOLINEA+
				strLog.toString();
	}
}
