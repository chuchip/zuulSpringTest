package com.profesorp.zuulSpringTest.Filters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.profesorp.zuulSpringTest.Requests.GatewayRequest;
import com.profesorp.zuulSpringTest.Requests.URIRequest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.DefaultClientConfig;


public class RouteURLFilter extends ZuulFilter {		
	
	
	@Override
	public String filterType() {
		return FilterConstants.ROUTE_TYPE;
	}

	@Override
	public int filterOrder() {
		return FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER - 1;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if ( ctx.getRequest().getRequestURI() == null || ! ctx.getRequest().getRequestURI().startsWith("/url"))
			return false;
		return ctx.getRouteHost() != null
				&& ctx.sendZuulResponse();
	}

    @Override
    public Object run() {
    	try {
    			
    		RequestContext ctx = RequestContext.getCurrentContext();
			HttpServletRequest request = ctx.getRequest();
		
		 URIRequest uriBankia;
		 InputStream inputStream = request.getInputStream();
		 byte[] body= StreamUtils.copyToByteArray(inputStream);
		 try  {
			 uriBankia= getURIRedirection(ctx);
			 if (uriBankia.getBody()!=null)
				 body=uriBankia.getBody();
		 } catch (ParseException k)
		 {			 	 
			 ctx.setResponseBody(k.getMessage());
			 ctx.setResponseStatusCode(HttpStatus.BAD_REQUEST.value());
			 ctx.setSendZuulResponse(false);
			 return null;
		 }	    		 
		
		 UriComponentsBuilder uriComponent=UriComponentsBuilder.fromHttpUrl(uriBankia.getUrl() );
	     if (uriBankia.getPath()==null)
	        	uriBankia.setPath("/");
	     uriComponent.path(uriBankia.getPath());
	     uriBankia.getPathSegmentos().forEach( ( campo,valor) -> {
	    	  try {
				uriComponent.queryParam(campo, URLEncoder.encode(valor,"UTF-8"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	    	
	     	}
	     );
	     
	     
	     String uri=uriComponent.build().toUriString();
	     WebResource webResource = Client.create(new DefaultClientConfig()).resource(uri);
	      		 					
		Builder builder = webResource.accept(MediaType.APPLICATION_JSON);
		Enumeration<String> enumerat =  request.getHeaderNames();
		boolean typeJSON=false;
		while (enumerat.hasMoreElements())
		{
			String name=enumerat.nextElement();
			Enumeration<String> valores=request.getHeaders(name);
			while (valores.hasMoreElements())
			{
				String value=valores.nextElement();
				if (name.equals("hostdestino")
						|| name.equals("pathdestino")
						|| name.equals("pathsegmentosdestino"))
					continue;
				if (name.equals("content-type") && value.equals("application/json"))
					typeJSON=true;
				webResource.header(name, value);
			}
		}
		
		
		ClientResponse response=null;
		String metodo=request.getMethod().toUpperCase();
		switch (metodo)
		{
			case "POST":
				if (typeJSON)
					response = builder.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,body );
				else
					response = builder.post(ClientResponse.class,body );
				break;
			case "GET":
				if (typeJSON)
					response = builder.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
				else
					response = builder.get(ClientResponse.class);
				break;
			case "PUT":
				if (typeJSON)
					response = builder.type(MediaType.APPLICATION_JSON).put(ClientResponse.class,body);
				else
					response = builder.put(ClientResponse.class,body);
				break;
			case "DELETE":
				if (typeJSON)
					response = builder.type(MediaType.APPLICATION_JSON).delete(ClientResponse.class,body);
				else
					response = builder.delete(ClientResponse.class,body);
				break;
			default:
				 ctx.setResponseBody("Tipo de peticion HTTP no valida "+metodo);
				 ctx.setResponseStatusCode(HttpStatus.BAD_REQUEST.value());
				 ctx.setSendZuulResponse(false);
				 return null;
		}
		 
		List<Pair<String, String>> filteredResponseHeaders = new ArrayList<>();
		response.getHeaders().forEach( (valor,lista) -> 
		{
			lista.forEach( key -> filteredResponseHeaders.add(new Pair<String,String>(valor,key)));
		});
		
		
		ctx.put("zuulResponseHeaders", filteredResponseHeaders);		
    	ctx.setSendZuulResponse(false);
    	ctx.setResponseBody(response.getEntity(String.class));
		ctx.setRouteHost(null); // prevent SimpleHostRoutingFilter from running
    	} catch (IOException k)
    	{
    		k.printStackTrace();
    	}
		return null;
    }
    URIRequest getURIRedirection(RequestContext ctx) throws ParseException
	{
    	URIRequest uriRequest;
		 String hostDestino=ctx.getRequest().getHeader("hostDestino");
		 if ( hostDestino==null)
	     {
			 uriRequest=getURIFromBody(ctx);
			if (uriRequest.getUrl()!=null)
				return uriRequest;
	    	throw new ParseException("La variable 'hostDestino' debe establecerse como variable de HEADER",0);
	     }
		
		 String pathDestino=ctx.getRequest().getHeader("pathDestino");
		 Enumeration<String> pathSegmentosDestino=ctx.getRequest().getHeaders("pathSegmentosDestino");
	  
	     UriComponentsBuilder uri=UriComponentsBuilder.fromHttpUrl(hostDestino);
	     if (pathDestino!=null)
	     {
	    	 uri=uri.path(pathDestino);
	     }
	     
	     HashMap<String,String> hmParams=new HashMap<>() ;
	     if (pathSegmentosDestino!=null)
	     {	    	    	
	    	 while (pathSegmentosDestino.hasMoreElements())
	    	 {
	    		 String valor=pathSegmentosDestino.nextElement();
	    		 String[] campos=valor.split("=");
	    		 if (campos.length==2)
	    		 {
	    			 hmParams.put(campos[0],campos[1]);	    	    			 
	    		 }
	    	 }	    	    	
	     }
	     uriRequest=new URIRequest();
	     uriRequest.setUrl(hostDestino);
	     uriRequest.setPath(pathDestino);
	     uriRequest.setPathSegmentos(hmParams);
		 return uriRequest;
	}
	
    URIRequest getURIFromBody(RequestContext ctx)  throws ParseException
	{
		Charset utf8=Charset.forName("UTF-8");
		URIRequest uriRequest=new URIRequest();
		try {			
			InputStream in = (InputStream) ctx.get("requestEntity");	 	     
	 	     if (in == null) {
	 	         in = ctx.getRequest().getInputStream();
	 	     }
	 	     String body = StreamUtils.copyToString(in, utf8);
	 	     if (body!=null)
	 	     {
	 	    	 
	 	    	 ObjectMapper map = new ObjectMapper();
	 			 
	 			 map.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	 			 try
	 			 {
	 			 	GatewayRequest gatewayRequest =map.readValue(body,GatewayRequest.class);
	 			 	if (gatewayRequest!=null)
	 			 	{
	 			 		uriRequest=gatewayRequest.getUri();
	 			 		if (uriRequest == null || uriRequest.getUrl()==null)
	 			 			throw new ParseException("La variable 'URL' del body en el objeto URI  debe tener un valor",0);
	 			 		body=gatewayRequest.getBody();
	 			 		uriRequest.setBody(body.getBytes(utf8));
	 			 	}
	 			 } catch (IOException  k1)
	 			 {	    	 				
	 			 }
	 	    	 ctx.set("requestEntity", new ByteArrayInputStream(body.getBytes(utf8)));
	 	    	
	 	    	 return uriRequest;
	 	     }	 		
		} catch (IOException k1)
		{
			k1.printStackTrace();
		}
		return uriRequest;
	}
}