## NOTE Documentation and release artifacts are being worked on. As such documenatation and/or artifacts may not match, we thank you for your patience!

## A Shibboleth IdP v4.X plugin for delegating authentication to an external SSO Server using the CAS protocol


This is a Shibboleth IdP external authentication plugin that delegates primary authentication to an external 
Single Sign On Server using the Central Authentication Server protocol. The biggest advantage of using this component over the plain 
`REMOTE_USER` header solution provided by Shibboleth is the ability to utilize a full range 
of native CAS protocol features such as `renew` and `gateway`, plus the ability to share with CAS the 
EntityID of the relying application.

The plugin takes advantage of and extends the Shibboleth IdP's external authentication flow, and consists of a number of JAR artifacts that bridge the gap between Shibboleth and CAS.

Maintenance Status
-------------------------------------------------------------

Maintenance of this project is sponsored by Unicon's [Open Source Support program](https://unicon.net/support). Professional support/integration assistance for this module is available. For more information, visit <https://unicon.net/opensource/shibboleth>.

Also, please do note that the Shibboleth IdP v3x+ has support for the CAS protocol and Apereo CAS server v5+ also has support for the SAML2 protocol. Unless justified otherwise, a better approach long-term would be to consolidate down to one platform removing the need to deploy and configure this plugin.


Software Requirements
-------------------------------------------------------------

This minimum supported version of Shibboleth Identity Provider is `4.3.0`. 
See [releases](https://github.com/Unicon/shib-cas-authn/releases) to find the the appropriate version.


Installation
---------------------------------------------------------------

#### Overview

- Download and extract the "latest release" zip or tar [from releases](https://github.com/Unicon/shib-cas-authn/releases).
- Copy the no-conversation-state.jsp file (also found inside this repo in IDP_HOME/edit-webapp) to your IdP's `IDP_HOME/edit-webapp`
- Copy two included jar files (`cas-client-core-x.x.x.jar` and `shib-casuathenticator-x.x.x.jar`) into the `IDP_HOME/edit-webapp/WEB-INF/lib`.
- Copy and Update the IdP's `web.xml`.
- Update the IdP's `external-authn.xml` file.
- (Optional) Update the IdP's `general-authn.xml` file.
- Update the IdP's `idp.properties` file.
- Rebuild the war file.

**NOTE:** You should **ALWAYS** refers to the `README.md` file that is [packaged with the release](https://github.com/Unicon/shib-cas-authn/releases) for instructions.


#### Update the IdP's `web.xml`

Add the ShibCas Auth Servlet entry in `IDP_HOME/edit-webapp/WEB-INF/web.xml`. If there's no existing `web.xml` file in that location, copy the original from `IDP_HOME/dist/webapp/WEB-INF/web.xml` into `IDP_HOME/edit-webapp/WEB-INF/web.xml` and edit there.

Example snippet `web.xml`:

```xml
...
    <!-- Servlet for receiving a callback from an external CAS Server and continues the IdP login flow -->
    <servlet>
        <servlet-name>ShibCas Auth Servlet</servlet-name>
        <servlet-class>net.unicon.idp.externalauth.ShibcasAuthServlet</servlet-class>
        <load-on-startup>2</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>ShibCas Auth Servlet</servlet-name>
        <url-pattern>/Authn/External/*</url-pattern>
    </servlet-mapping>
...
```

#### Update the IdP's external-authn.xml file

In the `IDP_HOME/authn/external-authn.xml` file, ensure the context path points to `Authn/External` as shown below.

```xml
    <!-- Servlet context-relative path to wherever your implementation lives. -->
    <bean id="shibboleth.authn.External.externalAuthnPath" class="java.lang.String"
        c:_0="contextRelative:Authn/External" />
```


#### OPTIONAL Update the IdP's general-authn.xml file

You may also need to ensure the `authn/External` flow is able to accept passive and forced authentication if you wish to use those features. The `authn/External` bean is modified in the `IDP_HOME/authn/general-authn.xml` file as shown below. Note that non browser flow is not possible or supported so it should be false.

```xml
<bean id="authn/External" parent="shibboleth.AuthenticationFlow"
  p:passiveAuthenticationSupported="true"
  p:forcedAuthenticationSupported="true"
  p:nonBrowserSupported="false" />
```


#### Update the IdP's idp.properties file

1. Set the `idp.authn.flows` to `External` in `IDP_HOME/conf/idp.properties`. Or, for advance cases, add `External` to the list if you have others.
1. Add new properties for the ShibCas plugin.

```properties   
...
# Regular expression matching login flows to enable, e.g. IPAddress|Password
#idp.authn.flows = Password
idp.authn.flows = External

# CAS Client properties (usage loosely matches that of the Java CAS Client)
## CAS Server Properties
shibcas.casServerUrlPrefix = https://cassserver.example.edu/cas
shibcas.casServerLoginUrl = ${shibcas.casServerUrlPrefix}/login

## Shibboleth Server Properties
shibcas.serverName = https://shibserver.example.edu

# By default you always get the AuthenticatedNameTranslator, add additional code to cover your custom needs.
# Takes a comma separated list of fully qualified class names
# shibcas.casToShibTranslators = com.your.institution.MyCustomNamedTranslatorClass
# shibcas.parameterBuilders = com.your.institution.MyParameterBuilderClass

# Specify CAS validator to use - either 'cas10', 'cas20' or 'cas30' (default)
# shibcas.ticketValidatorName = cas30


# Specify if the Relying Party/Service Provider entityId should be appended as a separate entityId query string parameter
# or embedded in the "service" querystring parameter - `append` (default) or `embed`
# shibcas.entityIdLocation = append
...
```


#### Rebuild the war file

From the `IDP_HOME/bin` directory, run `./build.sh` or `build.bat` to rebuild the `idp.war`. Redeploy if necessary.


#### OPTIONAL EntityId / CAS Service Passing
By setting `shibcas.entityIdLocation=embed`, shib-cas-authn will embed the entityId in the service string so that CAS Server
can use the entityId when evaluating a service registry entry match. Using serviceIds of something like: 
`https://shibserver.example.edu/idp/Authn/ExtCas\?conversation=[a-z0-9]*&entityId=http://testsp.school.edu/sp`
or
`https://shibserver.example.edu/idp/Authn/ExtCas\?conversation=[a-z0-9]*&entityId=http://test.unicon.net/sp`
will match as two different entries in the service registry which will allow as CAS admin to enable MFA or use access strategies on an SP by SP basis. 


OPTIONAL Handling REFEDS MFA Profile
---------------------------------------------------------------

The plugin has native support for [REFEDS MFA profile](https://refeds.org/profile/mfa). The requested authentication context class that is `https://refeds.org/profile/mfa`
is passed along from the Shibboleth IdP over to this plugin and is then translated to a multifactor authentication strategy supported by and configured CAS (i.e. Duo Security). 
The CAS server is notified of the required authentication method via a special `authn_method` parameter by default. Once a service ticket is issued and plugin begins to
validate the service ticket, it will attempt to ensure that the CAS-produced validation payload contains and can successfully assert the required/requested
authentication context class.

The supported multifactor authentication providers are listed below:

- Duo Security  (Requesting `authn_method=mfa-duo` and expecting validation payload attribute `authnContextClass=mfa-duo`)


#### REFEDS MFA Profile Configuration

In the `IDP_HOME/conf/idp.properties` file, ensure the following settings are set:

```properties
shibcas.casToShibTranslators = net.unicon.idp.externalauth.CasDuoSecurityRefedsAuthnMethodTranslator
shibcas.parameterBuilders = net.unicon.idp.authn.provider.extra.CasMultifactorRefedsToDuoSecurityAuthnMethodParameterBuilder
```

Finally add the authn context refs in the supported principals property list to `authn/External` in `general-authn.xml` as shown below. 

```xml
<bean id="authn/External" parent="shibboleth.AuthenticationFlow"
  p:passiveAuthenticationSupported="true"
  p:forcedAuthenticationSupported="true"
  p:nonBrowserSupported="false">
    <property name="supportedPrincipals">
        <list>
            <bean parent="shibboleth.SAML2AuthnContextClassRef"
                  c:classRef="https://refeds.org/profile/mfa" />
              <bean parent="shibboleth.SAML2AuthnContextClassRef"
                  c:classRef="urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport" />
        </list>
    </property>
</bean>
```

Release Notes
-------------------------------------------------------------
See [here](https://github.com/Unicon/shib-cas-authn/releases/).

Developer Notes
-------------------------------------------------------------
The project distributables can be built using `./gradlew clean build`. The artifacts will be in `build/distributions`.

This project includes a Docker environment to assist with development/testing. 

To build and execute: `./gradlew clean; ./gradlew up`
Then browse to: `https://idptestbed/idp/profile/SAML2/Unsolicited/SSO?providerId=https://sp.idptestbed/shibboleth`

> You'll need a `hosts` file entry that points `idptestbed` to your Docker server's IP address. 

The IdP only has a session of 1 minute (to test expired session/conversation key issues), so login into CAS Server quickly.
