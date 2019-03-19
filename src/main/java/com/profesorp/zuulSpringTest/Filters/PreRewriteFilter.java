package com.profesorp.zuulSpringTest.Filters;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;


public class PreRewriteFilter extends ZuulFilter {
	Logger log = LoggerFactory.getLogger(PreRewriteFilter.class); 
	@Override
	public Object run() {		
		 RequestContext ctx = RequestContext.getCurrentContext();	    
	     StringBuffer strLog=new StringBuffer();
	     strLog.append("\n------ FILTRANDO ACCESO A PRIVADO - PREREWRITE FILTER  ------\n");	    
	     
	     try {	    	
    		 String url=UriComponentsBuilder.fromHttpUrl("http://localhost:8080/").path("/api").build().toUriString();
    		 String usuario=ctx.getRequest().getHeader("usuario")==null?"":ctx.getRequest().getHeader("usuario");
    		 String password=ctx.getRequest().getHeader("clave")==null?"":ctx.getRequest().getHeader("clave");
    		 
    	     if (! usuario.equals(""))
    	     {
    	    	if (!usuario.equals("profesorp") || !password.equals("profe"))
    	    	{
	    	    	String msgError="Usuario y/o contraseña invalidos";
	    	    	strLog.append("\n"+msgError+"\n");	  
	    	    	ctx.setResponseBody(msgError);
	    	    	ctx.setResponseStatusCode(HttpStatus.FORBIDDEN.value());
	    	    	ctx.setSendZuulResponse(false); 
	    	    	log.info(strLog.toString());	    	    	
	    	    	return null;
    	    	}
    	    	// Borrando usuario y contraseña que no quiero que se pasen
    	    	// No utilizado pero dejado como ejemplo de las posibilidades.
//    	    	removeRequestHeader(ctx,Arrays.asList("usuario","clave"));
    	    	
//    	    	ctx.addZuulRequestHeader("usuario",null);
//    	    	ctx.getZuulRequestHeaders().remove("clave");
    	    	ctx.setRouteHost(new URL(url));
    	     }	    	     	    	
		} catch ( IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	     log.info(strLog.toString());
	     return null;
	}
	/**
	 * Quito de las HEADERS los parametros pasados en la lista.
	 * @param ctx
	 * @param listRemove
	 */
	void removeRequestHeader(RequestContext ctx, List<String> listRemove)
	{
		Map<String,String> headers=ctx.getZuulRequestHeaders();
		if (headers==null)
			return;
		List<String> headerPut= new ArrayList<>();
		headers.forEach( (key,value) -> {
			if (!listRemove.contains(key))
			{
				headerPut.add(key);
			}
		});
		// Esto solo funcionara si el orden del filtro es inferior a PRE_DECORATION_FILTER_ORDER
		ctx.put("zuulResponseHeaders", headerPut);
	}
	@Override
	public boolean shouldFilter() {				
		return RequestContext.getCurrentContext().getRequest().getRequestURI().startsWith("/privado");
	}

	@Override
	public int filterOrder() {
		
		return FilterConstants.PRE_DECORATION_FILTER_ORDER+1; 
	}

	@Override
	public String filterType() {
		return FilterConstants.PRE_TYPE;
	}

}
