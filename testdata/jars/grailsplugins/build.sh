mkdir -p grails/plugins
cp ../../bin/grails/plugins/A.class ./grails/plugins/.
jar -cvMf grailsplugins.jar grails/plugins/A.class

