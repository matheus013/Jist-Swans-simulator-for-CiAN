@set JAVA="java"

@set JAVA_OPT=-server

@%JAVA% -version
@set CP=..\libs\bcel.jar;..\libs\bsh.jar;..\libs\jargs.jar;..\libs\log4j.jar;..\libs\jython.jar;..\classes;%CLASSPATH%
@%JAVA% %JAVA_OPT% -Xmx250000000 -classpath %CP% jist.runtime.Main -S -p 555
