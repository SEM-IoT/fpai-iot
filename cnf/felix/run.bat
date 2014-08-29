
@echo off
title VIOS
echo VIOS
echo Running from %CD%

start "VIOS" java ^
      -Djava.security.policy=etc/all.policy ^
      -Dfelix.config.properties=file:etc/config.properties ^
      -Dfelix.cm.dir="%CD%\config" ^
      -Dlogback.configurationFile=etc/logback.xml ^
      -jar base_bundles/org.apache.felix.main-4.2.1.jar
		