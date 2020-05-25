#!/bin/sh

if test "$1" = "configure";
then
  # set classpath ( former classpath-config.sh and config-bit.sh )
  export CLASSPATH=$CLASSPATH:~/ist-cnv-sudoku/instrumented:~/ist-cnv-sudoku/project:~/ist-cnv-sudoku/BIT:~/ist-cnv-sudoku/BIT/samples:.
  export CLASSPATH=$CLASSPATH:/tmp/cnv/BIT:/tmp/cnv/BIT/samples:./
  export CLASSPATH=$CLASSPATH:~/aws-java-sdk-1.11.762/lib/aws-java-sdk-1.11.762.jar:~/aws-java-sdk-1.11.762/third-party/lib/*:.

  # export java variables (former java-config-rnl-vm.sh)l
  export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
  export JAVA_ROOT=/usr/lib/jvm/java-7-openjdk-amd64/
  export JDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
  export JRE_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre
  export PATH=/usr/lib/jvm/java-7-openjdk-amd64/bin/:$PATH
  export SDK_HOME=/usr/lib/jvm/java-7-openjdk-amd64/
  export _JAVA_OPTIONS="-XX:-UseSplitVerifier "$_JAVA_OPTIONS

  # create required folder structure
  mkdir -p instrumented/pt/ulisboa/tecnico/cnv/solver
fi

if test "$1" = "compile";
then
  # Find and compile all files
  find -name "*.java" > sources.txt
  javac @sources.txt
  
  # Update instrumented files
  java BIT/InstrumentationTool project/pt/ulisboa/tecnico/cnv/solver instrumented/pt/ulisboa/tecnico/cnv/solver/
fi

if test "$1" = "WebServer" ;
then
    java pt.ulisboa.tecnico.cnv.server.WebServer
fi

if test "$1" = "LoadBalancer" ;
then
    java loadbalancer.LoadBalancer $2
fi

if test "$1" = "AutoScaler" ;
then 
    java autoscaler.AutoScaler $2
fi

if test "$1" = "KillThreads" ;
then 
    pkill java
fi