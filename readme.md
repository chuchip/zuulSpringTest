En este articulo explicare como crear una pasarela para peticiones REST (una *gateway*)  utilizando Zuul.

**[Zuul](https://cloud.spring.io/spring-cloud-netflix/multi/multi__router_and_filter_zuul.html)** es parte del  paquete [Spring Cloud NetFlix](https://spring.io/projects/spring-cloud-netflix) y permite redirigir peticiones REST, realizando diversos tipos de filtros. 

En casi cualquier proyecto donde haya microservicios, es deseable que todas las comunicaciones entre esos microservicios pasen por un lugar común, de tal manera que se registren las entradas y salidas, se pueda implementar seguridad o se puedan redirigir las peticiones dependiendo de diversos parámetros. 

Con **Zuul** esto es muy fácil de implementar ya que esta perfectamente integrado con *Spring Boot*.

Como siempre [en mi página de GitHub](https://github.com/chuchip/zuulSpringTest) podéis ver los fuentes sobre los que esta basado este articulo.



### Creando el proyecto.

Si tenemos instalado *Eclipse* con el [plugin de *Spring Boot*](https://marketplace.eclipse.org/content/spring-tools-4-spring-boot-aka-spring-tool-suite-4) (lo cual recomiendo), el crear el proyecto seria tan fácil como añadir un  nuevo proyecto del tipo *Spring Boot* incluyendo el *starter* **Zuul**. Para poder hacer algunas pruebas también incluiremos el *starter*  **Web**, como se ve en la imagen:

![Eclipse Plugin Spring Boot](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/starters.png)

Como siempre tenemos la opción de crear un proyecto Maven desde la página web https://start.spring.io/ que luego importaremos desde nuestro IDE preferido.

![Crear proyecto Maven desde start.spring.io](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/springio.png)



### Empezando

Partiendo que nuestro programa esta escuchando en http://localhost:8080/ , vamos a a suponer que queremos que todo lo que vaya a la URL, http://localhost:8080/google sea redirigida a https://www.google.com. 

Para ello deberemos crear el fichero `application.yml` dentro del directorio *resources*, como se ve en la imagen

![Estructura del proyecto](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/estructura_ficheros.png)

En este fichero incluiremos las siguientes líneas:

```
zuul:  
  routes:
    google:
      path: /google/**
      url: https://www.google.com/
```

Con ellas especificaremos que todo lo que vaya a la ruta **/google/** y algo más (\**)  sea redirigido a  **https://www.google.com/** , teniendo en cuenta que si por ejemplo la petición es realizada `http://localhost:8080/google/search?q=profesor_p` esta será redirigida a `https://www.google.com/search?q=profesor_p`. Es decir lo que añadamos después de **/google/** será incluido en la redirección, debido a los dos asteriscos añadidos al final del path.

Para que el programa  funcione solo será necesario añadir la anotación `@EnableZuulProxy`en la clase de inicio, en este caso en: **ZuulSpringTestApplication**

```java
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
@SpringBootApplication
@EnableZuulProxy
public class ZuulSpringTestApplication {
	public static void main(String[] args) {
		SpringApplication.run(ZuulSpringTestApplication.class, args);
	}
}
```

### Filtrando: Dejando logs

En esta parte vamos a ver como crear un filtro de tal manera que se deje un registro de las peticiones realizadas.

Para ello crearemos la clase `PreFilter.java` la cual debe extender de **ZuulFilter**

```

public class PreFilter extends ZuulFilter {
	Logger log = LoggerFactory.getLogger(PreFilter.class); 
	@Override
	public Object run() {		
		 RequestContext ctx = RequestContext.getCurrentContext();	    
	     StringBuffer strLog=new StringBuffer();
	     strLog.append("\n------ NUEVA PETICION ------\n");	    	    	    
	     strLog.append(String.format("Server: %s Metodo: %s Path: %s \n",ctx.getRequest().getServerName()	    		 
					,ctx.getRequest().getMethod(),
					ctx.getRequest().getRequestURI()));
	     Enumeration<String> enume= ctx.getRequest().getHeaderNames();
	     String header;
	     while (enume.hasMoreElements())
	     {
	    	 header=enume.nextElement();
	    	 strLog.append(String.format("Headers: %s = %s \n",header,ctx.getRequest().getHeader(header)));	    			
	     };	  	    
	     log.info(strLog.toString());
	     return null;
	}

	@Override
	public boolean shouldFilter() {		
		return true;
	}

	@Override
	public int filterOrder() {
		return FilterConstants.SEND_RESPONSE_FILTER_ORDER;
	}

	@Override
	public String filterType() {
		return "pre";
	}

}
```

En esta clase deberemos sobrescribir las funciones que vemos en el fuente. A continuación explico que haremos en cada de ellas 

- **public Object run()**

  Aquí pondremos lo que queremos que se ejecute por cada petición recibida. En ella podremos ver el contenido de la petición y manipularla si fuera necesario.

- **public boolean shouldFilter()** 

  Si devuelve **true** se ejecutara la función **run** .

- **public int filterOrder()** 

  Devuelve cuando  que se ejecutara este filtro, pues normalmente hay diferentes filtros, para cada tarea. Hay que tener en cuenta que ciertas redirecciones o cambios en la petición hay que hacerlas en ciertos ordenes, por la misma lógica que tiene **zuul**  a la hora de procesar las peticiones.

- **public String filterType()**
  Especifica cuando se ejecutara el filtro. Si devuelve "pre"  se ejecutara antes de que se haya realizado la redirección y por lo tanto antes de que se haya llamado al servidor final (a google en nuestro ejemplo).

  Si devuelve "post" se ejecutara después de que el servidor haya respondido a la petición.

  En la clase `org.springframework.cloud.netflix.zuul.filters.support.FilterConstants` tenemos definidos los tipos a devolver, PRE_TYPE , POST_TYPE,ERROR_TYPE o ROUTE_TYPE.

En la clase de ejemplo vemos como antes de realizar la petición al servidor final, se registran algunos datos de la petición, dejando un log con ellos.

Por último, para que Spring Boot utilize este filtro debemos añadir la función siguiente en  nuestra clase principal.

```
@Bean
public PreFilter preFilter() {
        return new PreFilter();
 }
```

**Zuul** buscara *beans*  hereden de la clase **ZuulFilter **y los usara. 

En este ejemplo, también esta la clase **PostFilter.java**  que implementa otro filtro pero que se ejecuta después de realizar la petición al servidor  final. Como he comentado esto se consigue devolviendo "post" en la función **filterType()**.

Para que **Zuul**  use esta clase deberemos crear otro *bean* con una función como esta:

```
 @Bean
 public PostFilter postFilter() {
        return new PostFilter();
 }
```

Recordar que también hay un filtro para tratar los errores y otro para tratar justo después  del redirección ("route"), del que hablare más tarde en este articulo.

Aclarar que aunque no lo trato en este articulo con **Zuul** no solo podemos redirigir hacia URL estáticas sino también a servicios, suministrados por Eureka Server, del cual hable en un articulo articulo. Además se integra con Hystrix para tener tolerancia a fallos, de tal manera que si no puede alcanzar un servidor se puede especificar que acción tomar.

### Filtrando. Implementando seguridad

Añadamos una nueva redirección al fichero **application.yml**

```
  privado:
      path: /privado/**
      url: http://www.profesor-p.com  
```

Como se ve, esta redirección llevara cualquier petición tipo http://localhost:8080/privado/LO_QUE_SEA a la pagina donde esta  este articulo (http://www.profesor-p.com )

En la clase `PreRewriteFilter`he implementando otro filtro tipo **pre** que trata solo esta redirección. ¿ como ?. Fácil, poniendo este código en la función `shouldFilter()`

	@Override
	public boolean shouldFilter() {				
		return RequestContext.getCurrentContext().getRequest().getRequestURI().startsWith("/privado");
	}
Ahora en la función **run** incluimos el siguiente código

```java
Logger log = LoggerFactory.getLogger(PreRewriteFilter.class); 
	@Override
	public Object run() {		
		 RequestContext ctx = RequestContext.getCurrentContext();	    
	     StringBuffer strLog=new StringBuffer();
	     strLog.append("\n------ FILTRANDO ACCESO A PRIVADO - PREREWRITE FILTER  ------\n");	    
	     
	     try {	    	
    		 String url=UriComponentsBuilder.fromHttpUrl("http://www.profesor-p.com/").path("/wp-admin").build().toUriString();
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
    	    	ctx.setRouteHost(new URL(url));
    	     }	    	     	    	
		} catch ( IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	     log.info(strLog.toString());
	     return null;
	}
```

Busca en las cabeceras de la petición (*headers*) si bi existe la cabecera **usuario** no hace nada con lo cual se redireccionara a comprueba que sea `http://www.profesor-p.com`como se indica en el filtro. En caso de que exista la cabecera **usuario** se comprobara que tenga el valor `profesorp`y que exista la variable **clave** con el valor `profe`.  Si esa condición se cumple, la petición se redirigirá a `http://www.profesor-p.com/wp-admin` en caso contrario devolverá un código FORBIDEN devolviendo la cadena `"Usuario y/o contraseña invalidos"`  en el cuerpo de la respuesta HTTP. Ademas se cancela el flujo de la petición debido a que se llama a **ctx.setSendZuulResponse(false);**

### Filtrando. Filtrado dinámico

Para terminar incluiremos dos nuevas redirecciones en el fichero `applicaction.yml`

```
 local:
    path: /local/**
    url: http://localhost:8080/api
 url:
      path: /url/**
      url: http://url.com
```

En la primera cuando vayamos a la URL `http://localhost:8080/local/LO_QUE_SEA` seremos redirigidos a  `http://localhost:8080/api/LO_QUE_SEA`. Aclarar que la etiqueta `local:`es arbitraria y podría poner `pepe:` no teniendo porque coincidir con el path que deseamos redirigir.

En la segunda cuando vayamos a la URL `http://localhost:8080/url/LO_QUE_SEA` seremos redirigidos a  `http://localhost:8080/api/LO_QUE_SEA`

En http://localhost:8080/api estará escuchando un servicio REST que esta implementada en la clase **TestController** . Esta clase simplemente deja un registro de los datos de la  petición recibida y devuelve en el *body* los parámetros recibidos

```java
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

```

La clase **RouteURLFilter** sera la encargada de tratar **solo** las peticiones a **/url** para ello en la función **shouldFilter** tendremos este código:

```
@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		if ( ctx.getRequest().getRequestURI() == null || ! ctx.getRequest().getRequestURI().startsWith("/url"))
			return false;
		return ctx.getRouteHost() != null
				&& ctx.sendZuulResponse();
	}
```

Este filtro será declarado del tipo **pre** en la función **filterType** por lo cual se ejecutara después de los filtros *pre* y antes de ejecutar la redirección y llamar al servidor final.

```
	@Override
	public String filterType() {
		return FilterConstants.PRE_TYPE;
	}
```

En la función **run** esta el código que ejecuta realmente la petición a la URL solicitad, una vez hayamos capturado la *URL* de destino y  el *path*, como explico más adelante,  es utilizada la función **setRouteHost()**  del **RequestContext** para redirigirla adecuadamente.

```
	@Override
	public Object run() {
		try {
			RequestContext ctx = RequestContext.getCurrentContext();
			URIRequest uriRequest;
			try {
				uriRequest = getURIRedirection(ctx);
			} catch (ParseException k) {
				ctx.setResponseBody(k.getMessage());
				ctx.setResponseStatusCode(HttpStatus.BAD_REQUEST.value());
				ctx.setSendZuulResponse(false);
				return null;
			}

			UriComponentsBuilder uriComponent = UriComponentsBuilder.fromHttpUrl(uriRequest.getUrl());
			if (uriRequest.getPath() == null)
				uriRequest.setPath("/");
			uriComponent.path(uriRequest.getPath());

			String uri = uriComponent.build().toUriString();
			ctx.setRouteHost(new URL(uri));
		} catch (IOException k) {
			k.printStackTrace();
		}
		return null;
	}
```



Si encuentra en el header la variable `hostDestino` será donde mandara la petición recibida. También buscara en la cabecera de la petición  la variables `pathDestino` y `pathSegmentosDestino`. La primera será para añadida al `hostDestino` y la segunda serán los parámetros añadidos.  

Por ejemplo, supongamos una petición como esta:

``` 
> curl --header "hostDestino: http://localhost:8080" --header "pathDestino: api" \    localhost:8080/url?nombre=profesorp
```

La llamada será redirigida a http://localhost:8080/api?q=profesor-p y mostrara la siguiente salida:

```
---------- Prueba de ZUUL ------------
................ RECIBIDA PETICION EN /api ......
Metodo: GET
URL: http://localhost:8080/api
Host Remoto: 127.0.0.1
----- MAP ----
Clave:nombre Valor: profesorp

----- Headers ----
Clave:user-agent Valor: curl/7.60.0
Clave:accept Valor: */*
Clave:hostdestino Valor: http://localhost:8080
Clave:pathdestino Valor: api
Clave:x-forwarded-host Valor: localhost:8080
Clave:x-forwarded-proto Valor: http
Clave:x-forwarded-prefix Valor: /url
Clave:x-forwarded-port Valor: 8080
Clave:x-forwarded-for Valor: 0:0:0:0:0:0:0:1
Clave:accept-encoding Valor: gzip
Clave:host Valor: localhost:8080
Clave:connection Valor: Keep-Alive

----- BODY ----

```

También puede recibir la URL a redireccionar en un objeto JSON en el cuerpo de la petición HTML. El objeto JSON debe tener el formato definido por las clases **GatewayRequest** que a su vez contendrá un objeto **URIRequest**

```
public class GatewayRequest {
	URIRequest uri;
	String body;

}
```



```
public class URIRequest {
	String url;
	String path;
	byte[] body=null;
```

Este es un ejemplo de una redirección poniendo la URL destino en el body:

```
curl -X POST \
  'http://localhost:8080/url?nombre=profesorp' \
  -H 'Content-Type: application/json' \
  -d '{
    "body": "El body chuli", "uri": { 	"url":"http://localhost:8080", 	"path": "api"    }
}'
```

URL:  "http://localhost:8080/url?nombre=profesorp"

Cuerpo de la petición:

```json
{
 "body": "El body chuli",
    "uri": {
    	"url":"http://localhost:8080",
    	"path": "api"
    }
}
```

La salida recibida será:

```
---------- Prueba de ZUUL ------------
................ RECIBIDA PETICION EN /api ......
Metodo: POST
URL: http://localhost:8080/api
Host Remoto: 127.0.0.1
----- MAP ----
Clave:nombre Valor: profesorp

----- Headers ----
Clave:user-agent Valor: curl/7.60.0
Clave:accept Valor: */*
Clave:content-type Valor: application/json
Clave:x-forwarded-host Valor: localhost:8080
Clave:x-forwarded-proto Valor: http
Clave:x-forwarded-prefix Valor: /url
Clave:x-forwarded-port Valor: 8080
Clave:x-forwarded-for Valor: 0:0:0:0:0:0:0:1
Clave:accept-encoding Valor: gzip
Clave:content-length Valor: 91
Clave:host Valor: localhost:8080
Clave:connection Valor: Keep-Alive

----- BODY ----
El body chuli

```

Como se ve el cuerpo es tratado y al servidor final solo es mandado lo que se envía en el parámetro `body` de la petición **JSON**

Como se ve, **Zuul** tiene mucha potencia y es una excelente herramienta para realizar redirecciones. En este articulo solo he arañado las principales características de esta fantástica herramienta, pero espero que haya servido para ver las posibilidades que ofrece.

¡¡Nos vemos en la próxima entrada!!