package org.cloudfoundry.identity.uaa.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.oauth.token.RevocableToken;
import org.cloudfoundry.identity.uaa.oauth.token.RevocableTokenProvisioning;
import org.cloudfoundry.identity.uaa.util.UaaUrlUtils;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.util.StringUtils.hasText;

public class IsSelfCheck {

    private static Logger logger = LoggerFactory.getLogger(IsSelfCheck.class);

    private final RevocableTokenProvisioning tokenProvisioning;

    public IsSelfCheck(RevocableTokenProvisioning tokenProvisioning) {
        this.tokenProvisioning = tokenProvisioning;
    }

    public boolean isUserSelf(HttpServletRequest request, int pathParameterIndex) {
        String pathInfo = UaaUrlUtils.getRequestPath(request);
        String idFromUrl = extractIdFromUrl(pathParameterIndex, pathInfo);
        String idFromAuth = extractUserIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());

        return idFromAuth!=null &&
               idFromAuth.equals(idFromUrl);
    }

    protected String extractClientIdFromAuthentication(Authentication authentication) {
        if (authentication==null) {
            return null;
        }
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication a = (OAuth2Authentication)authentication;
            return a.getOAuth2Request().getClientId();
        }
        return null;
    }

    protected String extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication==null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof UaaPrincipal) {
            return ((UaaPrincipal)authentication.getPrincipal()).getId();
        }
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication a = (OAuth2Authentication)authentication;
            if (!a.isClientOnly()) {
                if (a.getUserAuthentication().getPrincipal() instanceof UaaPrincipal) {
                    return ((UaaPrincipal)a.getUserAuthentication().getPrincipal()).getId();
                }
            }
        }
        return null;
    }

    protected String extractIdFromUrl(int pathParameterIndex, String pathInfo) {
        if (!hasText(pathInfo)) {
            return null;
        }
        return UaaUrlUtils.extractPathVariableFromUrl(pathParameterIndex, pathInfo);
    }

    public boolean isTokenRevocationForSelf(HttpServletRequest request, int index) {
        String pathInfo = UaaUrlUtils.getRequestPath(request);
        String tokenId = extractIdFromUrl(index, pathInfo);
        if (hasText(pathInfo) && hasText(tokenId)) {
            try {
                RevocableToken revocableToken = tokenProvisioning.retrieve(tokenId, IdentityZoneHolder.get().getId());
                String clientIdFromToken = revocableToken.getClientId();
                String clientIdFromAuthentication = extractClientIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
                if (clientIdFromToken.equals(clientIdFromAuthentication)) {
                    return true;
                }
                String userIdFromToken = revocableToken.getUserId();
                String userIdFromAuthentication = extractUserIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
                if (hasText(userIdFromToken) && userIdFromToken.equals(userIdFromAuthentication)) {
                    return true;
                }
            } catch (EmptyResultDataAccessException x) {
                logger.debug("Token not found:"+tokenId);
            }
        }
        return false;
    }

    public boolean isUserTokenRevocationForSelf(HttpServletRequest request, int index) {
        String pathInfo = UaaUrlUtils.getRequestPath(request);
        String userIdFromPath = extractIdFromUrl(index, pathInfo);
        String userIdFromAuth = extractUserIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        return (hasText(userIdFromPath) && userIdFromPath.equals(userIdFromAuth));
    }

    public boolean isClientTokenRevocationForSelf(HttpServletRequest request, int index) {
        String pathInfo = UaaUrlUtils.getRequestPath(request);
        String clientIdFromPath = extractIdFromUrl(index, pathInfo);
        String clientIdFromAuth = extractClientIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        return (hasText(clientIdFromPath) && clientIdFromPath.equals(clientIdFromAuth));
    }

    public boolean isTokenListForAuthenticatedClient(HttpServletRequest request) {
        String pathInfo = UaaUrlUtils.getRequestPath(request);
        String clientId = extractIdFromUrl(4, pathInfo);
        String idFromAuth = extractClientIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        return hasText(idFromAuth) && idFromAuth.equals(clientId);
    }

    public boolean isTokenListForAuthenticatedUser(HttpServletRequest request) {
        String pathInfo = UaaUrlUtils.getRequestPath(request);
        String userId = extractIdFromUrl(4, pathInfo);
        String idFromAuth = extractUserIdFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        return hasText(idFromAuth) && idFromAuth.equals(userId);
    }
}
