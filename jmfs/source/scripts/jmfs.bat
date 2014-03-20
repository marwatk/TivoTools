@echo off
setlocal

set CLASSPATH=.\bin;.\@jar-name@;%CLASSPATH%
java -Xmx512m %*

endlocal