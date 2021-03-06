/*
 * The MIT License
 *
 * Copyright (c) 2019 Michael Wenk [https://github.com/michaelwenk]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package start;

import model.SSCLibrary;
import org.apache.commons.cli.*;
import org.openscience.cdk.exception.CDKException;

import java.io.IOException;

/**
 *
 * @author Michael Wenk [https://github.com/michaelwenk]
 */
public class Start {  
    
    private String pathToNMRShiftDB, mongoUser, mongoPassword, mongoAuthDB, mongoDBName, mongoDBCollection, pathToQueriesFile, pathToOutputsFolder, pathToJSON, format;
    private int nThreads, nStarts, maxSphere, minMatchingSphere;
    private boolean importFromNMRShiftDB, extendFromNMRShiftDB, useMongoDB, useJSON, removeDuplicates;
    private SSCLibrary sscLibrary;   
    private ProcessQueries processQueries;
    private double shiftTol, matchFactorThrs;
    public static double EQUIV_SIGNAL_THRS = 0.5, DUPLICATES_SHIFT_TOL = 5.0;
    public static int DECIMAL_PLACES = 2;

    public static final String SIGNAL_NUCLEUS = "13C";
    public static final String SPECTRUM_PROPERTY = "Spectrum " + Start.SIGNAL_NUCLEUS + " 0";
    
    public void start() throws Exception {
        if (this.useMongoDB) {
            final PrepareMongoDB prepareMongoDB = new PrepareMongoDB(this.nThreads, this.importFromNMRShiftDB, this.extendFromNMRShiftDB, this.pathToNMRShiftDB, this.maxSphere);
            prepareMongoDB.prepare(this.mongoUser, this.mongoPassword, this.mongoAuthDB, this.mongoDBName, this.mongoDBCollection, this.removeDuplicates);
            this.sscLibrary = new SSCLibrary();
        } else if (this.useJSON) {
            final PrepareJSONFile prepareJSONFile = new PrepareJSONFile(this.nThreads, this.importFromNMRShiftDB, this.extendFromNMRShiftDB, this.pathToNMRShiftDB, this.maxSphere);
            this.sscLibrary = prepareJSONFile.prepare(this.pathToJSON,  this.removeDuplicates);
        } else {
            throw new CDKException(Thread.currentThread().getStackTrace()[1].getMethodName() + ": invalid format: \"" + this.format + "\"");
        }
        this.processQueries = new ProcessQueries(this.sscLibrary, this.pathToQueriesFile, this.pathToOutputsFolder, this.nThreads, this.nStarts, this.shiftTol, this.matchFactorThrs, this.minMatchingSphere);
        if (this.useMongoDB) {
            this.processQueries.initMongoDBProcessing(this.mongoUser, this.mongoPassword, this.mongoAuthDB, this.mongoDBName, this.mongoDBCollection);
        }
        this.processQueries.process();

        System.gc();
    }
    
    
    private void parseArgs(String[] args) throws org.apache.commons.cli.ParseException, CDKException {

//        SmilesParser smilesParser = new SmilesParser(new SilentChemObjectBuilder());
//        IAtomContainer ac = smilesParser.parseSmiles("FC1=CC=CC(Cl)=C1");
//        ac.getAtom(2).setImplicitHydrogenCount(0);
//        IAtomContainer ac2 = smilesParser.parseSmiles("OC(=O)C1=C(F)C=CC=C1");//OC(=O)C1=C(F)C=C(Cl)C=C1
//        ac2.getAtom(7).setImplicitHydrogenCount(0);
//        try {
//            Utils.generatePicture(ac, "/Users/mwenk/Downloads/ac.png");
//            for (int i = 0; i < ac.getAtomCount(); i++) {
//                System.out.println(i + " -> " + HOSECodeBuilder.buildHOSECode(ac, i, 2, false));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println();
//        try {
//            Utils.generatePicture(ac2, "/Users/mwenk/Downloads/ac2.png");
//            for (int i = 0; i < ac2.getAtomCount(); i++) {
//                System.out.println(i + " -> " + HOSECodeBuilder.buildHOSECode(ac2, i, 2, false));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        IAtomContainer ac3 = smilesParser.parseSmiles("OC(=O)C1=C(F)C=C(Cl)C=C1");
//        try {
//            Utils.generatePicture(ac3, "/Users/mwenk/Downloads/ac3.png");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        final Options options = setupOptions(args);
        final CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            this.format = cmd.getOptionValue("format");            
            switch (this.format) {
                case "m":
                    this.useMongoDB = true;
                    this.useJSON = false;
                    
                    this.mongoUser = cmd.getOptionValue("user");
                    this.mongoPassword = cmd.getOptionValue("password");
                    this.mongoAuthDB = cmd.getOptionValue("authDB");
                    this.mongoDBName = cmd.getOptionValue("database");
                    this.mongoDBCollection = cmd.getOptionValue("collection");
                    if ((this.mongoUser == null)
                            || (this.mongoPassword == null)
                            || (this.mongoAuthDB == null)
                            || (this.mongoDBName == null)
                            || (this.mongoDBCollection == null)) {
                        throw new CDKException(Thread.currentThread().getStackTrace()[1].getMethodName() + ": at least one the parameters \"u\", \"p\", \"a\", \"db\" and \"c\" is missing");
                    }
                    break;
                case "j":
                    this.useMongoDB = false;
                    this.useJSON = true;
                    
                    this.pathToJSON = cmd.getOptionValue("json");
                    if (this.pathToJSON == null) {
                        throw new CDKException(Thread.currentThread().getStackTrace()[1].getMethodName() + ": parameter \"j\" is missing");
                    }
                    break;
                default:
                    this.useMongoDB = false;
                    this.useJSON = false;
                    throw new CDKException(Thread.currentThread().getStackTrace()[1].getMethodName() + ": invalid format: \"" + this.format + "\"");
            }   
            
            System.out.println("-useMongoDB: " + this.useMongoDB);
            System.out.println("-useJSON: " + this.useJSON);
            if(this.useJSON){
                System.out.println(" --> " + this.pathToJSON);
            }
            
            if(cmd.hasOption("import") || cmd.hasOption("extend")){
                if (cmd.hasOption("import")) {
                    this.importFromNMRShiftDB = true;
                    this.extendFromNMRShiftDB = false;                    
                } else {//if (cmd.hasOption("extend")) {
                    this.importFromNMRShiftDB = false;
                    this.extendFromNMRShiftDB = true;                    
                }           
                System.out.println("-importFromNMRShiftDB: " + this.importFromNMRShiftDB);
                System.out.println("-extendFromNMRShiftDB: " + this.extendFromNMRShiftDB); 
                if (!cmd.hasOption("nmrshiftdb") || !cmd.hasOption("maxsphere")) {
                    throw new CDKException(Thread.currentThread().getStackTrace()[1].getMethodName() + ": parameter \"nmrshiftdb\" and/or \"maxsphere\" is missing\"");
                } else {
                    this.pathToNMRShiftDB = cmd.getOptionValue("nmrshiftdb");   
                    this.maxSphere = Integer.parseInt(cmd.getOptionValue("maxsphere"));
                    if (this.maxSphere < 1) {
                        throw new CDKException(Thread.currentThread().getStackTrace()[1].getMethodName() + ": invalid number of maximum sphere: \"" + this.maxSphere + "\" < 1");
                    }
                    System.out.println("-pathToNMRShiftDB: " + this.pathToNMRShiftDB);
                    System.out.println("-maxSphere: " + this.maxSphere);
                }
            } else {
                this.importFromNMRShiftDB = false;
                this.extendFromNMRShiftDB = false;
                
                System.out.println("-importFromNMRShiftDB: " + this.importFromNMRShiftDB);
                System.out.println("-extendFromNMRShiftDB: " + this.extendFromNMRShiftDB); 
            }   
            
            this.removeDuplicates = false;
            if(cmd.hasOption("noduplicates")){
                this.removeDuplicates = true;
            }
            
            this.shiftTol = Double.parseDouble(cmd.getOptionValue("tol"));
            this.matchFactorThrs = Double.parseDouble(cmd.getOptionValue("mft"));
            this.minMatchingSphere = Integer.parseInt(cmd.getOptionValue("minsphere", "1"));
            this.nThreads = Integer.parseInt(cmd.getOptionValue("nthreads", "1"));            
            this.nStarts = Integer.parseInt(cmd.getOptionValue("nstarts", "-1"));                        
            this.pathToQueriesFile = cmd.getOptionValue("query");
            this.pathToOutputsFolder = cmd.getOptionValue("output", ".");
            
            System.out.println("-shiftTol: " + this.shiftTol);
            System.out.println("-matchFactorThrs: " + this.matchFactorThrs);   
            System.out.println("-minMatchingSphere: " + this.minMatchingSphere);   
            System.out.println("-nThreads: " + this.nThreads);
            System.out.println("-nStarts: " + this.nStarts);
            System.out.println("-removeDuplicates: " + this.removeDuplicates);
            System.out.println("-pathToQueriesFile: " + this.pathToQueriesFile);
            System.out.println("-pathToOutputsFolder: " + this.pathToOutputsFolder + "\n\n");
            
            this.pathToJSON = cmd.getOptionValue("json");
            
        } catch (org.apache.commons.cli.ParseException e) {
            // TODO Auto-generated catch block
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            String header = "FragAssembler aims on the computer-assisted structure elucidation of (un)known compounds by re-assembling known fragments of a dedicated library, solely based on given 13C NMR information. .\n\n";
            String footer = "\nPlease report issues at https://github.com/michaelwenk/FragAssembler.";
            formatter.printHelp("java -jar FragAssembler-1.0-SNAPSHOT-jar-with-dependencies.jar", header, options, footer, true);
            throw new org.apache.commons.cli.ParseException("Problem at parsing command line!!!");
        }
    }

    private Options setupOptions(String[] args) {
        Options options = new Options();
        Option dbFormatOption = Option.builder("f")
                .required(true)
                .hasArg()
                .longOpt("format")
                .desc("Format to use: " 
                        + "\ncase 1: \"j\" for JSON. The parameter \"j\" has to be set." 
                        + "\ncase 2: \"m\" for MongoDB. The parameters \"u\", \"p\", \"a\", \"db\" and \"c\" have to be set.")
                .build();
        options.addOption(dbFormatOption);
        Option pathToQueryFileOption = Option.builder("q")
                .required(true)
                .hasArg()
                .longOpt("query")
                .desc("Path to a file containing the query spectra.")
                .build();
        options.addOption(pathToQueryFileOption);    
        Option shiftTolOption = Option.builder("tol")
                .required(true)
                .hasArg()
                .longOpt("tolerance")
                .desc("Tolerance value for shift matching.")
                .build();
        options.addOption(shiftTolOption);
        Option matchFactorThrsOption = Option.builder("mft")
                .required(true)
                .hasArg()
                .longOpt("mfthreshold")
                .desc("Threshold value for maximum match factor.")
                .build();
        options.addOption(matchFactorThrsOption);
        Option minMatchingSphereOption = Option.builder("min")
                .required(false)
                .hasArg()
                .longOpt("minsphere")
                .desc("Minimum matching sphere count. The default is set to \"1\".")
                .build();
        options.addOption(minMatchingSphereOption);
        Option pathToOutputsFolderOption = Option.builder("o")
                .required(false)
                .hasArg()
                .longOpt("output")
                .desc("Path to a output directory for results. The default is set to \".\".")
                .build();
        options.addOption(pathToOutputsFolderOption);
        Option nthreadsOption = Option.builder("nt")
                .required(false)
                .hasArg()
                .longOpt("nthreads")
                .desc("Number of threads to use for parallelization. The default is set to 1.")
                .build();
        options.addOption(nthreadsOption); 
        Option nstartsOption = Option.builder("ns")
                .required(false)
                .hasArg()
                .longOpt("nstarts")
                .desc("Specified number of ranked SSCs to use for assembly process. The default is set to use all matched SSC given a query spectrum.")
                .build();
        options.addOption(nstartsOption);
        Option importFromNMRShiftDBOption = Option.builder("import")
                .required(false)
                .desc("Indicates that a NMRShiftDB file (SDF) will be used to build a SSC library from that and to overwrite all entries within a MongoDB collection or JSON file. The parameters \"nmrshiftdb\" and \"maxsphere\" must be set too.")
                .build();
        options.addOption(importFromNMRShiftDBOption);
        Option extendFromNMRShiftDBOption = Option.builder("extend")
                .required(false)
                .desc("Indicates that a NMRShiftDB file (SDF) will be used to build a SSC library from that and to extend a MongoDB collection or JSON file. The parameters \"nmrshiftdb\" and \"maxsphere\" must be set too.")
                .build();
        options.addOption(extendFromNMRShiftDBOption);
        Option nmrshiftdb = Option.builder("nmrshiftdb")
                .required(false)
                .hasArg()
                .desc("Path to NMRShiftDB (SDF).")
                .build();
        options.addOption(nmrshiftdb);
        Option maxsphereOption = Option.builder("maxsphere")
                .required(false)
                .hasArg()
                .desc("Maximum sphere limit for SSC creation. Minimum value is 1."
                        + "\nIf the \"import\" option is selected: SSCs with spherical limit (>= 2) will be created until \"maxsphere\" is reached."
                        + "\nIf the \"extend\" option is selected: only SSCs with exactly spherical limit \"maxsphere\" will be created." 
                        + "\nIf both parameters are given: the \"import\" will be done.")
                .build();
        options.addOption(maxsphereOption);
        Option mongoUserOption = Option.builder("u")
                .required(false)
                .hasArg()
                .longOpt("user")
                .desc("User name to use for login into MongoDB.")
                .build();
        options.addOption(mongoUserOption); 
        Option mongoPasswordOption = Option.builder("p")
                .required(false)
                .hasArg()
                .longOpt("password")
                .desc("User password to use for login into MongoDB.")
                .build();
        options.addOption(mongoPasswordOption);
        Option mongoAuthDBOption = Option.builder("a")
                .required(false)
                .hasArg()
                .longOpt("authDB")
                .desc("Authentication database name to use for login into MongoDB.")
                .build();
        options.addOption(mongoAuthDBOption);
        Option mongoDatabaseNameOption = Option.builder("db")
                .required(false)
                .hasArg()
                .longOpt("database")
                .desc("Database name to use for operations in MongoDB.")
                .build();
        options.addOption(mongoDatabaseNameOption);
        Option mongoCollectionNameOption = Option.builder("c")
                .required(false)
                .hasArg()
                .longOpt("collection")
                .desc("Collection name to fetch from selected database in MongoDB.")
                .build();
        options.addOption(mongoCollectionNameOption);
        Option removeDuplicatesOption = Option.builder("nd")
                .required(false)
                .longOpt("noduplicates")
                .desc("If given, the SSC library to build/extend will contain no structural duplicates with similar chemical shifts (deviations smaller than " + Start.DUPLICATES_SHIFT_TOL + " ppm).")
                .build();
        options.addOption(removeDuplicatesOption);
        Option pathToJSONLibraryOption = Option.builder("j")
                .required(false)
                .hasArg()
                .longOpt("json")
                .desc("Path to SSC library in JSON format.")
                .build();
        options.addOption(pathToJSONLibraryOption);
        
        
        return options;
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        final Start start = new Start();
        try {
            start.parseArgs(args);
            start.start();
        } catch (IOException | CloneNotSupportedException | InterruptedException | org.apache.commons.cli.ParseException | CDKException e) {
            System.err.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }                
    
}
