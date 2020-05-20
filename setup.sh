#!/bin/sh

if test "$1" = "configure";
then
  # set classpath ( former classpath-config.sh and config-bit.sh )
  export CLASSPATH=$CLASSPATH:~/ist-cnv-sudoku/instrumented:~/ist-cnv-sudoku/project:~/ist-cnv-sudoku/BIT:~/ist-cnv-sudoku/BIT/samples:.
  export CLASSPATH=$CLASSPATH:/tmp/cnv/BIT:/tmp/cnv/BIT/samples:./

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
  mkdir -p BIT/examples/output
fi

if test "$2" = "aws-cli";
then
  export CLASSPATH=$CLASSPATH:~/aws-java-sdk-1.11.762/lib/aws-java-sdk-1.11.762.jar:~/aws-java-sdk-1.11.762/third-party/lib/*:.
fi

if test "$1" = "compile";
then
  # Find and compile all files
  find -name "*.java" > sources.txt
  javac @sources.txt
  
  # Update instrumented files
  java BIT/InstrumentationTool project/pt/ulisboa/tecnico/cnv/solver instrumented/pt/ulisboa/tecnico/cnv/solver/
fi

if test "$1" = "InstrumentationToolHello";
then
    java BIT/InstrumentationTool BIT/examples BIT/examples/output
    cd BIT/examples/output && java Hello && cd ../../../
fi

if test "$1" = "InstrumentationToolSolver" ;
then
    java BIT/InstrumentationTool project/pt/ulisboa/tecnico/cnv/solver instrumented/pt/ulisboa/tecnico/cnv/solver/
    java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -n1 9 -n2 9 -un 81 -i SUDOKU_PUZZLE_9x19_101 -s DLX -b [[2,0,0,8,0,5,0,9,1],[9,0,8,0,7,1,2,0,6],[0,1,4,2,0,3,7,5,8],[5,0,1,0,8,7,9,2,4],[0,4,9,6,0,2,0,8,7],[7,0,2,1,4,9,3,0,5],[1,3,7,5,0,6,0,4,9],[4,2,5,0,1,8,6,0,3],[0,9,6,7,3,4,0,1,2]]
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
