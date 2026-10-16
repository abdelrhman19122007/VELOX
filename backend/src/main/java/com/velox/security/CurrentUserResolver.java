package com.velox.security;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.velox.auth.repository.UserRepository;
import com.velox.common.error.ApiException;

/**
 * Resolves {@code @CurrentUser AuthUser} from the {@code Authorization: Bearer <token>} header: the signature and
 * expiry must be valid, the account must exist, not be blocked, and its token_version must match (so signed-out,
 * blocked or password-changed sessions stop at once).
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private final TokenService tokens;
    private final UserRepository users;

    public CurrentUserResolver(TokenService tokens, UserRepository users) {
        this.tokens = tokens;
        this.users = users;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType() == AuthUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        AuthUser user = authenticate(request.getHeader("Authorization"));
        boolean required = parameter.getParameterAnnotation(CurrentUser.class).required();
        if (user == null && required) {
            throw ApiException.unauthorized();
        }
        return user;
    }

    /** The account behind an Authorization header, or null. Also used by streaming endpoints. */
    public AuthUser authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokens.verify(authorization.substring(7).trim())
                .flatMap(c -> users.findActiveByPublicId(c.publicId())
                        .filter(u -> u.tokenVersion() == c.version()))
                .map(u -> u.toAuthUser())
                .orElse(null);
    }
}
