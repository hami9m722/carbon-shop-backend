package uit.carbon_shop.base.service;

import org.springframework.security.core.userdetails.UserDetails;


public interface TokenService {

    String generateToken(final UserDetails userDetails);

    String validateTokenAndGetUsername(final String token);

}
