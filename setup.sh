#!/bin/sh

# Config
chmod +x BIT/java-config-rnl-vm.sh
chmod +x BIT/config-bit.sh
chmod +x classpath-config.sh

./BIT/java-config-rnl-vm.sh
./BIT/config-bit.sh
./classpath-config.sh

# Find and compile all files
find -name "*.java" > sources.txt
javac @sources.txt


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
