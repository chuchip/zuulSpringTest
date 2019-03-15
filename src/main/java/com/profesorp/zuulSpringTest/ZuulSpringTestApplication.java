package com.profesorp.zuulSpringTest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

import com.profesorp.zuulSpringTest.Filters.PostFilter;
import com.profesorp.zuulSpringTest.Filters.PreFilter;
import com.profesorp.zuulSpringTest.Filters.PreRewriteFilter;
import com.profesorp.zuulSpringTest.Filters.RouteURLFilter;

@SpringBootApplication
@EnableZuulProxy
public class ZuulSpringTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZuulSpringTestApplication.class, args);
	}
	  
    @Bean
    public RouteURLFilter routerFilter() {
        return new RouteURLFilter();
    }
    @Bean
    public PreFilter preFilter() {
        return new PreFilter();
    }
    @Bean
    public PreRewriteFilter preRewriteFilter() {
        return new PreRewriteFilter();
    }
    @Bean
    public PostFilter postFilter() {
        return new PostFilter();
    }
}
