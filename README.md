# Fragment Assembler (FragAssembler)

FragAssembler aims on the computer-assisted structure elucidation of (un)known compounds by re-assembling 
known fragments of a dedicated library, solely based on given 13C NMR information. <br> 

This approach follows the idea by M. Will et al.<sup>1</sup>. 

## SSC Library 
 
Each fragment is stored as a substructure-subspectrum-correlation (SSC) object in a SSC library and 
consists of its substructure and the assigned chemical shift (13C NMR) for each carbon atom. <br>
Therefore, the NMRShiftDB<sup>2</sup> was used to build such SSC library. Each 
structure in NMRShiftDB was fragmented at each atom by using the hierarchical order of HOSE codes<sup>3</sup> 
with maximum spheres of 2, 3 and 4.

## Procedure

Once a 13C NMR query spectrum, incl. multiplicities, of the compound under investigation is given, each SSC in SSC 
library is compared with the query spectrum and is considered to be a hit if all signals in SSC could match to a 
signal in the query spectrum. All SSC hits are then ranked according their spectral similarity to the query spectrum. <br> 
After that the assembling process starts with the first SSC hit and tries to extend its substructure by the 
next ranked SSCs and their substructures if there is a structural overlap (HOSE code) and 
some validation steps are passed successfully. <br>
The result is a list of assembled structure proposals for a query spectrum in a file containing SMILES.


#### References 
<sup>1</sup> M. Will et al., J. Chem. Inf. Comput. Sci.1996362221-227, https://doi.org/10.1021/ci950092p <br>
<sup>2</sup> C. Steinbeck et al., J. Chem. Inf. Comput. Sci.20034361733-1739, https://doi.org/10.1021/ci0341363 <br>
<sup>3</sup> W. Bremser, Analytica Chimica Acta, 103(4), 355-365., https://doi.org/10.1016/S0003-2670(01)83100-7


## Installation and Usage

### Requirement

The packages HOSECodeBuilder (https://github.com/michaelwenk/HOSECodeBuilder) and 
casekit (https://github.com/michaelwenk/casekit) have to be installed on the local machine. Both of them
are dependencies in FragAssembler's pom.xml and have to ready to use, e.g. in Maven's .m2 folder. <br>
That can be done with commands like:

    
    mvn install:install-file -Dfile=PATH/TO/HOSECodeBuilder-1.0-SNAPSHOT-jar-with-dependencies.jar -DgroupId=org.openscience -DartifactId=HOSECodeBuilder -Dversion=1.0 -Dpackaging=jar
    mvn install:install-file -Dfile=PATH/TO/casekit-1.0-SNAPSHOT-jar-with-dependencies.jar -DgroupId=org.openscience -DartifactId=casekit -Dversion=1.0 -Dpackaging=jar 

### Installation

1. Clone this repository:

    git clone https://github.com/michaelwenk/FragAssembler.git

2. Change the directory:

    cd FragAssembler

3. And build the jar file with following Maven command:
    
    mvn clean package

### Usage

The following are the arguments with which you can start FragAssembler (an example is given below):

    usage: java -jar FragAssembler-1.0-SNAPSHOT-jar-with-dependencies.jar -f
           <arg> -q <arg> -tol <arg> -mft <arg> [-min <arg>] [-o <arg>] [-nt
           <arg>] [-ns <arg>] [-import] [-extend] [-nmrshiftdb <arg>]
           [-maxsphere <arg>] [-u <arg>] [-p <arg>] [-a <arg>] [-db <arg>] [-c
           <arg>] [-nd] [-j <arg>]
    
     -f,--format <arg>          Format to use:
                                case 1: "j" for JSON. The parameter "j" has to
                                be set.
                                case 2: "m" for MongoDB. The parameters "u",
                                "p", "a", "db" and "c" have to be set.
     -q,--query <arg>           Path to a file containing the query spectra.
     -tol,--tolerance <arg>     Tolerance value for shift matching.
     -mft,--mfthreshold <arg>   Threshold value for maximum match factor.
     -min,--minsphere <arg>     Minimum matching sphere count. The default is
                                set to "1".
     -o,--output <arg>          Path to a output directory for results. The
                                default is set to ".".
     -nt,--nthreads <arg>       Number of threads to use for parallelization.
                                The default is set to 1.
     -ns,--nstarts <arg>        Specified number of ranked SSCs to use for
                                assembly process. The default is set to use
                                all matched SSC given a query spectrum.
     -import                    Indicates that a NMRShiftDB file (SDF) will be
                                used to build a SSC library from that and to
                                overwrite all entries within a MongoDB
                                collection or JSON file. The parameters
                                "nmrshiftdb" and "maxsphere" must be set too.
     -extend                    Indicates that a NMRShiftDB file (SDF) will be
                                used to build a SSC library from that and to
                                extend a MongoDB collection or JSON file. The
                                parameters "nmrshiftdb" and "maxsphere" must
                                be set too.
     -nmrshiftdb <arg>          Path to NMRShiftDB (SDF).
     -maxsphere <arg>           Maximum sphere limit for SSC creation. Minimum
                                value is 1.
                                If the "import" option is selected: SSCs with
                                spherical limit (>= 2) will be created until
                                "maxsphere" is reached.
                                If the "extend" option is selected: only SSCs
                                with exactly spherical limit "maxsphere" will
                                be created.
                                If both parameters are given: the "import"
                                will be done.
     -u,--user <arg>            User name to use for login into MongoDB.
     -p,--password <arg>        User password to use for login into MongoDB.
     -a,--authDB <arg>          Authentication database name to use for login
                                into MongoDB.
     -db,--database <arg>       Database name to use for operations in
                                MongoDB.
     -c,--collection <arg>      Collection name to fetch from selected
                                database in MongoDB.
     -nd,--noduplicates         If given, the SSC library to build/extend will
                                contain no structural duplicates with similar
                                chemical shifts (deviations smaller than 5.0
                                ppm).
     -j,--json <arg>            Path to SSC library in JSON format.
     
### Example

Lets have look on the following example:

    java -jar /target/FragAssembler-jar-with-dependencies.jar -q /PATH/TO/queries.txt -o PATH/TO/results/ -f j -j ssclibrary.json -import -nmrshiftdb /PATH/TO/NMRShiftDB.sdf -maxsphere 3 -tol 4.0 -mft 3.0 -nt 2 -ns 5 

This command above starts FragAssembler with a queries file "queries.txt" and the results should be stored in a
results folder. The chosen format is JSON and the NMRShiftDB.sdf is used to create a new SSC library (-import) with 
SSCs with a maximum limit of 3 spheres for each root atom. The SSC library is stored in the ssclibrary.json file. <br> 
Between the chemical shifts of two signals a tolerance of 4 ppm is allowed but in average the
shift deviations of all matched signals between two spectra (match factor) must not be higher than 3.0 ppm. <br>
The number of start SSCs in assembly process is limited to 5. For parallelization, two threads are allowed for this job.

The queries file with query spectra in NMRShiftDB spectrum format looks like:

    //  10016316 coffein
    13C, 27.8, Q, 0.0
    13C, 29.6, Q, 0.0
    13C, 33.5, Q, 0.0
    13C, 107.8, S, 0.0
    13C, 144.3, D, 0.0
    13C, 147.5, S, 0.0
    13C, 151.6, S, 0.0
    13C, 155.3, S, 0.0
    
    //  2205 Methyl 4-(4-carboxyphenyl)-6-chloro-5-formyl-2-methyl-1,4-dihydropyridine-3-carboxylate 
    13C, 17.8, Q, 0.0
    13C, 37.7, D, 0.0
    13C, 51.2, Q, 0.0
    13C, 104.1, S, 0.0
    13C, 110.7, S, 0.0
    13C, 127.0, D, 0.0
    13C, 127.0, D, 0.0
    13C, 128.9, D, 0.0
    13C, 128.9, D, 0.0
    13C, 132.7, S, 0.0
    13C, 142.9, S, 0.0
    13C, 145.8, S, 0.0
    13C, 148.5, S, 0.0
    13C, 166.5, S, 0.0
    13C, 167.9, S, 0.0
    13C, 186.5, D, 0.0

At the moment, only 13C 1D NMR spectra are allowed as queries. <br>
Each query spectrum starts with "//" and a description for the it. 
Then, in the signal list, each signal nucleus is followed by the chemical shift, 
the multiplicity and the intensity. <br>
Two query spectra are separated by an empty line.

One of the output file lines for coffein query spectrum looks like:

    CN1C=NC2=C1C(=O)N(C)C(=O)N2C 0 1.0

It means for an assembled structure (SMILES) it has the highest rank (0) with a Tanimoto coefficient 
of 1.0 regarding the assembled spectrum of the result and the query spectrum.