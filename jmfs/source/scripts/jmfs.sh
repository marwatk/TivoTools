export CLASSPATH=./bin:./@jar-name@:$CLASSPATH
java -Xmx512m "$@"
