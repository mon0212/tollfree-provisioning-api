package com.telecom.tollfree.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
	@Value("${app.jwt.secret}") String secret;
	@Bean SecurityFilterChain chain(HttpSecurity h)throws Exception{
		return h.cors(c->c.configurationSource(corsSource())).csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/swagger-ui/**","/v3/api-docs/**","/actuator/health").permitAll().anyRequest().authenticated()).addFilterBefore(new JwtFilter(secret),UsernamePasswordAuthenticationFilter.class).build();
		}
	@Bean CorsConfigurationSource corsSource(){
		CorsConfiguration cfg=new CorsConfiguration();
		cfg.setAllowedOriginPatterns(List.of("http://localhost:3000","http://localhost:*"));
		cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
		cfg.setAllowedHeaders(List.of("*"));
		cfg.setAllowCredentials(false);
		UrlBasedCorsConfigurationSource src=new UrlBasedCorsConfigurationSource();
		src.registerCorsConfiguration("/**",cfg);
		return src;
	}
	
	static class JwtFilter extends OncePerRequestFilter {
		private final byte[] key;
		JwtFilter(String x){
			key=x.getBytes(StandardCharsets.UTF_8);
			}
		
		@Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse p,FilterChain c)throws ServletException,IOException{
			String h=r.getHeader("Authorization");
			if(h!=null&&h.startsWith("Bearer "))
				try{Claims claims=Jwts.parser().verifyWith(new SecretKeySpec(key,"HmacSHA256")).build().parseSignedClaims(h.substring(7)).getPayload();
			String role=claims.get("role",String.class);
			var auth=new UsernamePasswordAuthenticationToken(claims.getSubject(),null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
			org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
			}
			
			catch(JwtException ignored){
				
			}
			c.doFilter(r,p);
			}
		} 
	}
