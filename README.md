# ist-cnv-sudoku

## Usage

```bash
source classpath-config.sh 
java ICount project/pt/ulisboa/tecnico/cnv/solver/ instrumented/pt/ulisboa/tecnico/cnv/solver/
java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -n1 9 -n2 9 -un 81 -i SUDOKU_PUZZLE_9x19_101 -s DLX -b [[2,0,0,8,0,5,0,9,1],[9,0,8,0,7,1,2,0,6],[0,1,4,2,0,3,7,5,8],[5,0,1,0,8,7,9,2,4],[0,4,9,6,0,2,0,8,7],[7,0,2,1,4,9,3,0,5],[1,3,7,5,0,6,0,4,9],[4,2,5,0,1,8,6,0,3],[0,9,6,7,3,4,0,1,2]]
```


## To compile ICount and run on instrumented file in examples

```bash
source BIT/java-config-rnl-vm.sh && source BIT/config-bit.sh&& source classpath-config.sh && javac BIT/samples/ICount.java && java ICount BIT/examples BIT/examples/output && cd BIT/examples/output && java Hello && cd ../../../
```

```bash
source BIT/java-config-rnl-vm.sh && source BIT/config-bit.sh && source classpath-config.sh && javac BIT/samples/ICount.java && java ICount project/pt/ulisboa/tecnico/cnv/solver instrumented/pt/ulisboa/tecnico/cnv/solver/ && java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -n1 9 -n2 9 -un 81 -i SUDOKU_PUZZLE_9x19_101 -s DLX -b [[2,0,0,8,0,5,0,9,1],[9,0,8,0,7,1,2,0,6],[0,1,4,2,0,3,7,5,8],[5,0,1,0,8,7,9,2,4],[0,4,9,6,0,2,0,8,7],[7,0,2,1,4,9,3,0,5],[1,3,7,5,0,6,0,4,9],[4,2,5,0,1,8,6,0,3],[0,9,6,7,3,4,0,1,2]]
```