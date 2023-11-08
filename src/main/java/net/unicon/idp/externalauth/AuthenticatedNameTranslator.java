package net.unicon.idp.externalauth;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.authn.ExternalAuthentication;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apereo.cas.client.authentication.AttributePrincipal;
import org.apereo.cas.client.validation.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple translation of the principal name from the CAS assertion to the string value used by Shib
 *
 * @author chasegawa@unicon.net
 * @author jgasper@unicon.net
 */
public class AuthenticatedNameTranslator implements CasToShibTranslator {
    private final Logger logger = LoggerFactory.getLogger(AuthenticatedNameTranslator.class);

    @Override
    public void doTranslation(final HttpServletRequest request, final HttpServletResponse response,
                              final Assertion assertion, final String authenticationKey) {
        if (assertion == null || assertion.getPrincipal() == null) {
            logger.error("No valid assertion or principal could be found to translate");
            return;
        }
        final AttributePrincipal casPrincipal = assertion.getPrincipal();
        logger.debug("principalName found and being passed on: {}", casPrincipal.getName());

        // Pass authenticated principal back to IdP to finish its part of authentication request processing
        final Collection<IdPAttributePrincipal> assertionAttributes = produceIdpAttributePrincipal(assertion.getAttributes());
        final Collection<IdPAttributePrincipal> principalAttributes = produceIdpAttributePrincipal(casPrincipal.getAttributes());

        if (!assertionAttributes.isEmpty() || !principalAttributes.isEmpty()) {
            logger.debug("Found attributes from CAS. Processing...");
            final Set<Principal> principals = new HashSet<>();

            principals.addAll(assertionAttributes);
            principals.addAll(principalAttributes);
            principals.add(new UsernamePrincipal(casPrincipal.getName()));

            request.setAttribute(ExternalAuthentication.SUBJECT_KEY, new Subject(false, principals,
                Collections.emptySet(), Collections.emptySet()));
            logger.info("Created an IdP subject instance with principals containing attributes for {} ", casPrincipal.getName());

        } else {
            logger.debug("No attributes released from CAS. Creating an IdP principal for {}", casPrincipal.getName());
            request.setAttribute(ExternalAuthentication.PRINCIPAL_NAME_KEY, casPrincipal.getName());
        }
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(final Object that) {
        return EqualsBuilder.reflectionEquals(this, that);
    }

    private Collection<IdPAttributePrincipal> produceIdpAttributePrincipal(final Map<String, Object> casAttributes) {
        final Set<IdPAttributePrincipal> principals = new HashSet<>();
        for (final Map.Entry<String, Object> entry : casAttributes.entrySet()) {
            logger.debug("Processing CAS attribute {}", entry.getKey());
            try {
                final List<IdPAttributeValue> attributeValues = new ArrayList<>();
                if (entry.getValue() instanceof Collection) {
                    for (final Object value : (Collection) entry.getValue()) {
                        final String attributeValue = value.toString();
                        if (attributeValue.trim().isEmpty()) {
                            logger.warn("Skipping attribute {} with empty value(s)", entry.getKey());
                        } else {
                            logger.debug("Adding value {} for attribute {}", attributeValue, entry.getKey());
                            attributeValues.add(new StringAttributeValue(attributeValue));
                        }
                    }
                } else {
                    final String attributeValue = entry.getValue().toString();
                    if (attributeValue.trim().isEmpty()) {
                        logger.warn("Skipping attribute {} with empty value(s)", entry.getKey());
                    } else {
                        logger.debug("Adding value {} for attribute {}", attributeValue, entry.getKey());
                        attributeValues.add(new StringAttributeValue(attributeValue));
                    }
                }
                if (!attributeValues.isEmpty()) {
                    final IdPAttribute attribute = new IdPAttribute(entry.getKey());
                    attribute.setValues(attributeValues);
                    logger.debug("Added attribute {} with values {}", entry.getKey(), entry.getValue());
                    principals.add(new IdPAttributePrincipal(attribute));
                } else {
                    logger.warn("Skipped attribute {} since it contains no values", entry.getKey());
                }
            } catch (Exception e) {
                final String message = "Unable to process attribute: " + entry.getKey() + ":" + e.getMessage();
                if (logger.isDebugEnabled()) {
                    logger.warn(message, e);
                } else {
                    logger.warn(message);
                }
            }
        }
        return principals;
    }
}
