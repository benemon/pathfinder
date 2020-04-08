package com.redhat.gps.pathfinder.web.api.security;


import java.net.URISyntaxException;

/*-
 * #%L
 * Pathfinder
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import com.redhat.gps.pathfinder.domain.Member;
import com.redhat.gps.pathfinder.repository.MembersRepository;
import com.redhat.gps.pathfinder.service.util.MapBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import mjson.Json;

@RestController
public class AuthenticationRestController {

    @Value("Authorization")
    private String tokenHeader;

    @Autowired
    MembersRepository membersRepository;
    
    private AuthenticationManager basicAuthenticationManager;
    private AuthenticationManager tokenAuthenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    @Qualifier("jwtUserDetailsService")
    private UserDetailsService userDetailsService;

    @RequestMapping(value = "/auth", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthToken(@RequestBody String authRequest, 
                                             @RequestHeader(value="x-forwarded-user", required=false) String xForwardedUser) throws RuntimeException, URISyntaxException, CredentialsException {
        Json jsonReq = Json.read(authRequest);
        String username = null;                                      
        if ( null != xForwardedUser) {
          try {
            if (null==tokenAuthenticationManager) tokenAuthenticationManager=new CustomTokenAuthenticationProvider(membersRepository);
              tokenAuthenticationManager.authenticate(new PreAuthenticatedAuthenticationToken(xForwardedUser, xForwardedUser));
              username = xForwardedUser;
              // Reload password post-security so we can generate the token
              try{          
                return buildResult(getToken(username), getMember(username));
              }catch(UsernameNotFoundException e){
                throw new CredentialsException("No user with the name "+username);
              }              
          } catch (DisabledException e) {
              throw new CredentialsException("User is disabled!", e);
          } catch (BadCredentialsException e) {
              throw new CredentialsException("Bad credentials!", e);
          }          
        }            
        else {            
          username=jsonReq.at("username").asString();
          String password=jsonReq.at("password").asString();

          Objects.requireNonNull(username);
          Objects.requireNonNull(password);
          
          try {
            if (null==basicAuthenticationManager) basicAuthenticationManager=new CustomBasicAuthenticationProvider(membersRepository);
              basicAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

              // Reload password post-security so we can generate the token
              try{          
                return buildResult(getToken(username), getMember(username));
              }catch(UsernameNotFoundException e){
                throw new CredentialsException("No user with the name "+username);
              }
          } catch (DisabledException e) {
              throw new CredentialsException("User is disabled!", e);
          } catch (BadCredentialsException e) {
              throw new CredentialsException("Bad credentials!", e);
          }
        }
    } 

    /**
     * Return a token based on username
     * @param username
     * @return String
     */
    private String getToken(String username) {
      final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
      final String token = jwtTokenUtil.generateToken(userDetails);
      return token;
    }

    /**
     * Return Member details based on username
     * @param username
     * @return Member
     */
    private Member getMember(String username) {
      return membersRepository.findOne(username);
    }

    @RequestMapping(value = "/refresh", method = RequestMethod.GET)
    public ResponseEntity<?> refreshAndGetAuthenticationToken(HttpServletRequest request) {
        String authToken = request.getHeader(tokenHeader);
        final String token = authToken.substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        // just to check the user still exists associated with the token
        final UserDetails userDetails = (UserDetails)userDetailsService.loadUserByUsername(username);
        
        Member member=membersRepository.findOne(username);
        
        String refreshedToken = jwtTokenUtil.refreshToken(token);
        
        return buildResult(refreshedToken, member);
    }
    
    private ResponseEntity buildResult(String token, Member member){
      return ResponseEntity.ok(new MapBuilder<String,String>()
          .put("token", token)
          .put("username", member.getUsername())
          .put("displayName", member.getDisplayName())
          .build());
    }

    public ResponseEntity<String> handleAuthenticationException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

//    /**
//     * Authenticates the user. If something is wrong, an {@link AuthenticationException} will be thrown
//     */
//    private void authenticate(String username, String password) {
//        Objects.requireNonNull(username);
//        Objects.requireNonNull(password);
//
//        try {
//            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
//        } catch (DisabledException e) {
//            throw new RuntimeException("User is disabled!", e);
//        } catch (BadCredentialsException e) {
//            throw new RuntimeException("Bad credentials!", e);
//        }
//    }
}
