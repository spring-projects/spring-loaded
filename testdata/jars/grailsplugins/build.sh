mkdir -p grails/plugins
mkdir -p grails/plugins2
cp ../../bin/grails/plugins/A.class ./grails/plugins/.
cp ../../bin/grails/plugins2/C.class ./grails/plugins2/.
jar -cvMf grailsplugins.jar grails/plugins/A.class grails/plugins2/C.class

rm grails/plugins/*.class
rm grails/plugins2/*.class
rmdir grails/plugins
rmdir grails/plugins2
rmdir grails

