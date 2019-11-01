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
package fragmentation;

import casekit.NMR.Utils;
import casekit.NMR.model.Assignment;
import casekit.NMR.model.Spectrum;
import hose.HOSECodeBuilder;
import hose.model.ConnectionTree;
import model.SSC;
import model.SSCLibrary;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import utils.ParallelTasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Fragmentation {
    
    /**
     * Builds a set of substructure-subspectrum-correlations (SSC objects) from
     * an atom container set for all its molecules and atoms by using a 
     * breadth first search with spherical limit.
     *
     * @param SSCComponentsSet Set of components for each structure to build SSCs from it
     * @param maxSphere Spherical limit for building a substructure into 
     * all directions
     * @param nThreads Number of threads to use for parallelization
     *
     * @return
     *
     * @throws java.lang.InterruptedException
     *
     * @see casekit.NMR.dbservice.NMRShiftDB#getSSCComponentsFromNMRShiftDB(String, String)
     * @see Fragmentation#buildSSCCollection(IAtomContainer, Spectrum, Assignment, int)
     */
    public static SSCLibrary buildSSCLibrary(final HashMap<Integer, Object[]> SSCComponentsSet, final int maxSphere, final int nThreads) throws InterruptedException {

        final SSCLibrary sscLibrary = new SSCLibrary(nThreads);
        final ConcurrentLinkedQueue<SSC> builtSSCs = new ConcurrentLinkedQueue<>();
        final ArrayList<Callable<Collection<SSC>>> callables = new ArrayList<>();
        // add all task to do
        for (final int index: SSCComponentsSet.keySet()) {
            callables.add(() -> Fragmentation.buildSSCCollection((IAtomContainer) SSCComponentsSet.get(index)[0], (Spectrum) SSCComponentsSet.get(index)[1], (Assignment) SSCComponentsSet.get(index)[2], maxSphere));
        }
        ParallelTasks.processTasks(callables, sscCollection -> sscCollection.parallelStream().forEach(builtSSCs::add), nThreads);
        sscLibrary.extend(builtSSCs);

//        HashMap<String, ArrayList<SSC>> map = new HashMap<>();
//        for (final SSC ssc : builtSSCs){
//            String extendedHOSECode = Compare.getExtendedHOSECode(ssc);
//            if(!map.containsKey(extendedHOSECode)){
//                map.put(extendedHOSECode, new ArrayList<>());
//            }
//            map.get(extendedHOSECode).add(ssc);
//        }
//        long sscCount = 0;
//        for(final Map.Entry<String, ArrayList<SSC>> entry : map.entrySet()){
//            sscCount += entry.getValue().size();
//        }
//        System.out.println(" BUILT SSC COUNT -> " + builtSSCs.size() + " vs. MAP SIZE -> " + map.size() + ", " + sscCount);

        return sscLibrary;
    }
    
    /**
     * Builds a collection of substructure-subspectrum-correlations (SSC objects) from one
     * structure for all its atoms by using a breadth first search 
     * with spherical limit. 
     *
     * @param structure Structure to build the SSCs from
     * @param spectrum Spectrum with signals to use for each assigned atom in structure
     * @param assignment Assignments between atoms in structure and belonging signals in spectrum
     * @param maxSphere Spherical limit for building a substructure into 
     * all directions to be the same type as in used spectrum property.
     *
     * @return
     * @see Fragmentation#buildSSC(IAtomContainer, Spectrum, Assignment, int, int)
     */
    public static Collection<SSC> buildSSCCollection(final IAtomContainer structure, final Spectrum spectrum, final Assignment assignment, final int maxSphere) throws CDKException {
        // if the structure contains explicit hydrogens atoms then, after 
        // removing them, the assignments have to be corrected 
        if(Utils.containsExplicitHydrogens(structure)){
            final HashMap<IAtom, Integer> prevAtomIndices = Utils.convertExplicitToImplicitHydrogens(structure);  
            for (final IAtom atom : prevAtomIndices.keySet()) {
                // consider only atoms with same atom type as first spectrum nucleus type
                if(!atom.getSymbol().equals(Utils.getAtomTypeFromSpectrum(spectrum, 0))){
                    continue;
                }
                for (int i = 0; i < structure.getAtomCount(); i++) {
                    if((structure.getAtom(i) == atom) && (i != prevAtomIndices.get(atom))){
                        assignment.setAssignment(0, assignment.getIndex(0, prevAtomIndices.get(atom)), i);
                        break;
                    } 
                }
            }
        }
        final Collection<SSC> sscCollection = new ArrayList<>();
        SSC ssc;
        for (int i = 0; i < structure.getAtomCount(); i++) {      
            ssc = Fragmentation.buildSSC(structure, spectrum, assignment, i, maxSphere);
            // if one part of the structure could not be built then skip the whole structure
            // and return an empty SSC library
            if (ssc == null) {
                return new ArrayList<>();
            }
            sscCollection.add(ssc);
        }

        return sscCollection;
    }        
    
    /**
     * Builds a substructure-subspectrum-correlation ({@link model.SSC}) object 
     * from a structure, a spectrum and signal to atom assignments.
     * The structure fragmentation is done by using breadth first 
     * search with a spherical limit and each atom as starting point. 
     *
     * @param structure structure to fragment into substructures
     * @param spectrum spectrum to split into subspectra
     * @param assignment signal to atom assignments
     * @param rootAtomIndex Index of start atom
     * @param maxSphere Spherical limit for building a substructure into 
     * all directions
     * 
     * @return
     * @throws org.openscience.cdk.exception.CDKException
     * @see Fragmentation#buildSubstructure(org.openscience.cdk.interfaces.IAtomContainer, int, int)
     */
    public static SSC buildSSC(final IAtomContainer structure, final Spectrum spectrum, final Assignment assignment, final int rootAtomIndex, final int maxSphere) throws CDKException {
        Utils.setAromaticityAndKekulize(structure);
        final ArrayList<Integer> substructureAtomIndices = Fragmentation.buildSubstructureAtomIndicesList(structure, rootAtomIndex, maxSphere);
        final IAtomContainer substructure = Fragmentation.buildSubstructure(structure, rootAtomIndex, maxSphere);
        final Spectrum subspectrum = new Spectrum(spectrum.getNuclei());
        final Assignment subassignment = new Assignment(subspectrum);
        IAtom atomInStructure;
        for (int j = 0; j < substructure.getAtomCount(); j++) {
            atomInStructure = structure.getAtom(substructureAtomIndices.get(j));
            if(atomInStructure.getSymbol().equals(Utils.getAtomTypeFromSpectrum(subspectrum, 0))){
                if((assignment.getIndex(0, substructureAtomIndices.get(j)) == null) || (spectrum.getSignal(assignment.getIndex(0, substructureAtomIndices.get(j))) == null)){
                    return null;
                }
                subspectrum.addSignal(spectrum.getSignal(assignment.getIndex(0, substructureAtomIndices.get(j))));
                subassignment.addAssignment(new int[]{j});                
            }
        }
        subspectrum.setSolvent(spectrum.getSolvent());
        subspectrum.setSpectrometerFrequency(spectrum.getSpectrometerFrequency());
        subspectrum.detectEquivalences();
        // tries to return a valid SSC with all complete information
        // if something is missing/incomplete then null will be returned 
        try {
            return new SSC(subspectrum, subassignment, substructure, 0, maxSphere);
        } catch (Exception e) {
            return null;
        }         
    }
    
    /**
     * Builds a substructure from a structure using a breadth first search 
     * with spherical limit, starting point as well as HOSE code priority order
     * of next neighbor atoms. 
     *
     * @param structure IAtomContainer as structure
     * @param rootAtomIndex Index of start atom
     * @param maxSphere Spherical limit for building a substructure into 
     * all directions
     * @return
     *
     * @see HOSECodeBuilder#buildConnectionTree(IAtomContainer, int, Integer)
     * @see HOSECodeBuilder#buildAtomContainer(ConnectionTree)
     */
    public static IAtomContainer buildSubstructure(final IAtomContainer structure, final int rootAtomIndex, final int maxSphere) {
        return HOSECodeBuilder.buildAtomContainer(HOSECodeBuilder.buildConnectionTree(structure, rootAtomIndex, maxSphere));
    }
    
    /**
     * Builds a set of substructure atom indices from a structure using a 
     * breadth first search with spherical limit, starting point as well as 
     * HOSE code priority order of next neighbor atoms. 
     *
     * @param structure IAtomContainer as structure
     * @param rootAtomIndex Index of start atom
     * @param maxSphere Spherical limit for building a substructure into 
     * all directions
     * @return
     *
     * @see HOSECodeBuilder#buildConnectionTree(IAtomContainer, int, Integer)
     */
    public static ArrayList<Integer> buildSubstructureAtomIndicesList(final IAtomContainer structure, final int rootAtomIndex, final int maxSphere) {
        return HOSECodeBuilder.buildConnectionTree(structure, rootAtomIndex, maxSphere).getKeys();
    }
}
