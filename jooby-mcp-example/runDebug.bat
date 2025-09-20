@setlocal
@set MAVEN_DEBUG_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=18095
mvn jooby:run
