package com.redhat.gps.pathfinder.web.api.security;

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

import java.util.ArrayList;

import com.redhat.gps.pathfinder.domain.Member;
import com.redhat.gps.pathfinder.repository.MembersRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CustomTokenAuthenticationProvider implements AuthenticationManager {
    private static final Logger log = LoggerFactory.getLogger(CustomTokenAuthenticationProvider.class);
  
    public CustomTokenAuthenticationProvider(final MembersRepository userRepository) {
      this.userRepository = userRepository;
    }

    MembersRepository userRepository;

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
      final String name = authentication.getName();

      log.debug("CustomTokenAuthenticationProvider::authenticate() name={}, token={}", name, name);

      final Member user = userRepository.findOne(name);
      
      if (null==user) return null;
      
       return new PreAuthenticatedAuthenticationToken(name, name, new ArrayList<>());
    }
}
