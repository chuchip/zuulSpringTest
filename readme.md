En este articulo explicare como crear una pasarela para peticiones REST (una *gateway*)  utilizando Zuul.

**[Zuul](https://cloud.spring.io/spring-cloud-netflix/multi/multi__router_and_filter_zuul.html)** es parte del  paquete [Spring Cloud NetFlix](https://spring.io/projects/spring-cloud-netflix) y permite redirigir peticiones REST, realizando diversos tipos de filtros. 

En casi cualquier proyecto donde haya microservicios, es deseable que todas las comunicaciones entre esos microservicios pasen por un lugar común, de tal manera que se registren las entradas y salidas, se pueda implementar seguridad o se puedan redirigir las peticiones dependiendo de diversos parámetros. 

Con **Zuul** esto es muy fácil de implementar ya que esta perfectamente integrado con *Spring Boot*.

Como siempre [en mi página de GitHub](https://github.com/chuchip/zuulSpringTest) podéis ver los fuentes sobre los que esta basado este articulo.



### Creando el proyecto.

Si tenemos instalado *Eclipse* con el [plugin de *Spring Boot*](https://marketplace.eclipse.org/content/spring-tools-4-spring-boot-aka-spring-tool-suite-4) (lo cual recomiendo), el crear el proyecto seria tan fácil como añadir un  nuevo proyecto del tipo *Spring Boot* incluyendo el *starter* **Zuul**. Para poder hacer algunas pruebas también incluiremos el *starter*  **Web**, como se ve en la imagen:

![Eclipse Plugin Spring Boot](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/springio.png)

Como siempre tenemos la opción de crear un proyecto Maven desde la página web https://start.spring.io/ que luego importaremos desde nuestro IDE preferido.

![Crear proyecto Maven desde start.spring.io](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/springio.png)

Además de estos starters  en nuestro proyecto deberemos incluir las siguientes dependencias en Maven

```
<!-- https://mvnrepository.com/artifact/com.sun.jersey/jersey-client -->
		<dependency>
		    <groupId>com.sun.jersey</groupId>
		    <artifactId>jersey-client</artifactId>
		</dependency>

		<!-- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind -->
		<dependency>
		    <groupId>com.fasterxml.jackson.core</groupId>
		    <artifactId>jackson-databind</artifactId>		  
		</dependency>
```

Estas dependencias son necesarias por este proyecto por las especificaciones que tiene, pero **Zuul** como tal no las necesita en absoluto.

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

En http://localhost:8080/api estará escuchando un servicio REST que esta implementada en la clase **TestController** . Esta clase simplemente deja un registro de los datos de la  petición recibida y devuelve en el body los parámetros recibidos

```java
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

Este filtro sera declarado del tipo **router** en la función **filterType** por lo cual se ejecutara después de los filtros *pre* y antes de ejecutar la redirección y llamar al servidor final.

```
	@Override
	public String filterType() {
		return FilterConstants.ROUTE_TYPE;
	}
```

En la función **run** esta el código que ejecuta realmente la petición a la URL solicitada

Si encuentra en el header la variable `hostDestino` será donde mandara la petición recibida. También buscara en la cabecera de la petición  la variables `pathDestino` y `pathSegmentosDestino`. La primera será para añadida al `hostDestino` y la segunda serán los parámetros añadidos.  

Por ejemplo, supongamos una petición como esta:

``` 
> curl --header "hostDestino: http://localhost:8080" --header "pathDestino: api"   --header "pathSegmentosDestino: q=profesor-p" localhost:8080/url
```

La llamada será redirigida a http://localhost:8080/api?q=profesor-p como se puede ver en la siguiente captura de **postman**



![postman1](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/postman1.png)

También puede recibir la URL a redireccionar en un objeto JSON en el cuerpo de la peticion HTML. El objeto JSON debe tener el formato definido por las clases **GatewayRequest** que a su vez contendrá un objeto **URIRequest**

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
	HashMap<String,String> pathSegmentos=new HashMap<>();
	byte[] body=null;
```



Aquí se puede ver una captura de pantalla de postman realizando la llamada a través del body:

![postman2](https://raw.githubusercontent.com/chuchip/zuulSpringTest/master/postman2.PNG)

Realmente en este filtro lo que se hace es crear una nueva petición HTTP, usando la librería  `com.sun.jersey.api.client` , capturar la salida de esa petición y devolverla a **Zuul**. Pero no voy a entrar a explicar en profundidad su funcionamiento. Siempre se puede estudiar el código, ¿ verdad ? ;-)

Como se ve, **Zuul** tiene mucha potencia y es una excelente herramienta para realizar redirecciones. En este articulo solo he arañado las principales características de esta fantástica herramienta, pero espero que haya servido para ver las posibilidades que ofrece.

¡¡Nos vemos en la próxima entrada!!