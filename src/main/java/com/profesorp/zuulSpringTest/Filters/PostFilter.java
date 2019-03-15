package com.profesorp.zuulSpringTest.Filters;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public class PostFilter extends ZuulFilter {
	Logger log = LoggerFactory.getLogger(PostFilter.class); 
	@Override
	public Object run() {		
		 RequestContext ctx = RequestContext.getCurrentContext();
	     ctx.addZuulRequestHeader("Test", "TestSample");
	     StringBuffer strLog=new StringBuffer();
	     
	     strLog.append("\n-- VUELTA DE PETICION--\n");
	     strLog.append(String.format("Request URL: %s Metodo: %s \n",ctx.getRequest().getRequestURL()	    		 
					,ctx.getRequest().getMethod()));
	     Enumeration<String> enume= ctx.getRequest().getHeaderNames();
	     String header;
	     while (enume.hasMoreElements())
	     {
	    	 header=enume.nextElement();
	    	 strLog.append(String.format("Headers: %s = %s \n",header,ctx.getRequest().getHeader(header)));	    			
	     };
//	     String body="";
//	     if (ctx.getResponseDataStream()!=null)
//	     {
//	    	 try {
//	    		InputStream input=ctx.getResponseDataStream();
//	    		
//				body=IOUtils.toString(ctx.getResponseDataStream());
//				ctx.setResponseDataStream(IOUtils.);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	     }
//	     log.info(String.format("Body: Data: %s - %s", body,
//					ctx.getResponseBody()));
	     strLog.append("Codigo Respuesta: "+ctx.getResponse().getStatus()+"\n");
	     log.info(strLog.toString());	     
	     return null;
	}

	@Override
	public boolean shouldFilter() {		
		return true;
	}

	@Override
	public int filterOrder() {
		return 99;
	}

	@Override
	public String filterType() {
		return "post";
	}

}
