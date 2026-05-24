package com.sneakershop.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// Bật tính năng phân quyền bằng Annotation @PreAuthorize
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
                    config.setAllowedMethods(Arrays.asList(
                            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
                    ));
                    config.setAllowedHeaders(Arrays.asList("*"));
                    config.setAllowCredentials(true);
                    return config;
                })
                .and().csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/orders/**").permitAll()
                .antMatchers("/api/customers/**").permitAll()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/v1/cart/**").permitAll()
                .antMatchers("/api/v1/checkout/**").permitAll()
                .antMatchers("/api/v1/sepay/webhook").permitAll()
                .antMatchers("/api/v1/sepay/payment-info/**").permitAll()
                .antMatchers("/api/storefront/orders/lookup").permitAll()
                .antMatchers(HttpMethod.GET, "/api/storefront/colors").permitAll()
                .antMatchers(HttpMethod.GET, "/api/storefront/sizes").permitAll()
                .antMatchers("/api/storefront/orders/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/prices/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/pricing/**").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/pricing/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/prices/**").hasAuthority("ADMIN")
                .antMatchers("/api/products/**").permitAll()
                .antMatchers("/api/categories/**").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers("/api/admin/promotions/**").hasAuthority("ADMIN")
                .antMatchers("/api/admin/returns/**").hasAnyAuthority("ADMIN", "SALES")
                .antMatchers("/api/returns/**").authenticated()
                .antMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .antMatchers("/api/admin/products/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/pricing/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/price-campaign/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/price-campaign/**").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/price-campaign/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/vouchers/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/vouchers/**").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/vouchers/**").permitAll()
                .antMatchers("/api/vouchers/customers-list").permitAll()
                .antMatchers("/api/reports/promotions/**").permitAll()
                .antMatchers("/api/reports/promotion-dashboard/**").permitAll()
                .antMatchers("/api/management/logs/**").hasAuthority("ADMIN")

                // QUAN TRỌNG: Cấu hình quyền cho Logs và Users
                .antMatchers("/api/management/logs/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.POST, "/api/management/users/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/management/users/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/management/users/**").hasAuthority("ADMIN")
                .antMatchers(HttpMethod.GET, "/api/management/users/**").authenticated()

                .anyRequest().authenticated();

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}