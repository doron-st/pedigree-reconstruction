package pedreconstruction;

import graph.*;
import misc.MyLogger;
import relationship.RelationshipProbWeight;
import pedigree.Pedigree;

import java.util.*;

public class SibGraphExpander {

    private final Graph contractedGraph;
    private final Contraction contraction;
    private final Map<Vertex, Boolean> vacancyMap = new HashMap<Vertex, Boolean>();
    private final List<Vertex> eList;//list of expended vertices
    private int expendedNum = 0;

    public SibGraphExpander(Graph contractedGraph, Contraction cont) {
        this.contractedGraph = contractedGraph;
        contraction = cont;
        eList = new ArrayList<Vertex>();
        int numOfCont = 0;
        for (Vertex sv : contractedGraph.getVertices()) {
            for (Vertex v : ((SuperVertex) sv.getData()).getInnerVertices()) {
                vacancyMap.put(v, true);
            }
            if (((SuperVertex) sv.getData()).getInnerVertices().size() > 1)
                numOfCont++;
        }
        MyLogger.important("RelationGraphExpender::num of contracted vertices=" + numOfCont);
    }

    /**
     * Run Expension algorithm:
     * Partitions ambigous relationships into father/mother relationships
     * Run on all edges sorted by probability
     *
     * @return - The expended relationships graph
     */
    public Graph run() {
        List<Edge> allSuperEdges = new ArrayList<Edge>();
        //	double t = 0.2;
        List<Vertex> vertices = contractedGraph.getVertices();
        for (int i = 0; i < vertices.size(); i++) {
            Vertex sv1 = vertices.get(i);
            for (int j = i + 1; j < vertices.size(); j++) {
                Vertex sv2 = vertices.get(j);
                Edge se = contractedGraph.getEdge(sv1, sv2);
                if (se != null && ((RelationshipProbWeight) se.getWeight()).isMaxProbCategory("fullSib"))
                    allSuperEdges.add(se);
            }
        }
        final double[] probs = new double[allSuperEdges.size()];
        Integer[] ind = new Integer[allSuperEdges.size()];

        int i = 0;
        for (Edge se : allSuperEdges) {
            probs[i] = ((RelationshipProbWeight) se.getWeight()).getProb("fullSib");
            ind[i] = i;
            i++;
        }
        Arrays.sort(ind, new Comparator<Integer>() {
            @Override
            public int compare(final Integer o1, final Integer o2) {
                return Double.compare(probs[o2], probs[o1]);
            }
        });

        i = 0;
        Edge[] sortedSuperEdges = new Edge[allSuperEdges.size()];
        for (i = 0; i < ind.length; i++)
            sortedSuperEdges[i] = allSuperEdges.get(ind[i]);

        for (Edge se : sortedSuperEdges)
            MyLogger.important("Added a super-edge " + se.getVertex1() + "," + se.getVertex2() + ", with prob " + se.getWeight());
        for (Edge se : sortedSuperEdges)
            expandOnEdge(se);


        MyLogger.important("RelationGraphExpender::expended " + expendedNum + " vertices");
        //Add all unexpended vertices to list (single children)
        List<Vertex> uList = new ArrayList<Vertex>();//list of unexpended vertices
        for (Vertex sv : contractedGraph.getVertices()) {
            for (Vertex v : ((SuperVertex) sv.getData()).getInnerVertices())
                if (!eList.contains(v)) {
                    eList.add(v);
                    MyLogger.important(v + " was not expended!");
                    if (!uList.contains(sv))
                        uList.add(sv);
                }
        }
        contraction.addUnexpandedList(uList);

        return new Graph(eList);
    }


    public void removeRedundantEdges(Pedigree p) {
        for (Vertex sv : contractedGraph.getVertices()) {
            //If vertex is a singleton super-vertex
            if (((SuperVertex) sv.getData()).getInnerVertices().size() == 1) {
                int id = sv.getVertexId();
                List<Integer> mateIDs = p.getMates(id);
                //MyLogger.important("MateIDs=" + mateIDs);

                //Remove edges only from mates of polygamous individuals
                if (mateIDs.size() == 1)
                    continue;

                for (Edge se : sv.getEdgeMap().values()) {
                    Vertex potentSib = se.getVertex2();
                    //concern only with full-sib edges
                    if (((RelationshipProbWeight) se.getWeight()).isMaxProbCategory("fullSib")) {
                        //	double myProb = ((RelationshipProbWeight)se.getWeight()).getProb("fullSib");
                        for (int mateID : mateIDs) {
                            Vertex mate = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID).getId());
                            if (mate.hasEdgeTo(potentSib.getVertexId()) && ((RelationshipProbWeight) mate.getEdgeTo(potentSib.getVertexId()).getWeight()).isMaxProbCategory("fullSib")) {
                                MyLogger.important("Mate " + mateID + " has sibEdge to " + potentSib.getVertexId() + " altough " + sv + ", has multiple mates, removing edge");
                                mate.removeEdgeTo(potentSib);
                                potentSib.removeEdgeTo(mate);

								/*Edge mateEdge = mate.getEdgeTo(potentSib.getVertexId());
								double mateProb = ((RelationshipProbWeight)mateEdge.getWeight()).getProb("fullSib");
								if(mateProb<myProb){
									MyLogger.important("Mate " + mateID + " has sibEdge to " + potentSib.getVertexId() +" with lower probability then " + sv + ", removing edge");
									mate.removeEdgeTo(potentSib);
								}
								 */
                            }
                        }
                    }
                }
            }
        }
    }

    private void expandOnEdge(Edge se) {
        MyLogger.important("Expanding edge " + se.getVertex1() + " " + se.getVertex2());
        SuperVertex sv1 = (SuperVertex) se.getVertex1().getData();
        SuperVertex sv2 = (SuperVertex) se.getVertex2().getData();

        //If both supervertices have one inner vertex
        if (sv1.getInnerVertices().size() == 1 && sv2.getInnerVertices().size() == 1) {
            MyLogger.important("expanding pseudo super-vertices: " + sv1.getId() + "," + sv2.getId());
            assignSuperEdge(se, sv1.getInnerVertices().get(0), sv2.getInnerVertices().get(0));
            return;
        }

        //Try to add a vacant node from sv1 to an existing siblings clique in sv2
        for (Vertex v : (sv1.getInnerVertices())) {
            if (vacancyMap.get(v)) {
                for (Vertex u : sv2.getInnerVertices()) {
                    if (!vacancyMap.get(u)) {
                        //v is vacant and u is occupied
                        if (testSiblingAndAddToClique(v, u, se)) {
                            return;
                        }
                    }
                }
            }
        }
        //Try to add a vacant node in sv1, to an existing siblings clique in sv1
        for (Vertex u : (sv2.getInnerVertices())) {
            if (vacancyMap.get(u)) {
                for (Vertex v : sv1.getInnerVertices()) {
                    if (!vacancyMap.get(v)) {
                        //u is vacant and v is occupied
                        if (testSiblingAndAddToClique(u, v, se))
                            return;
                    }
                }
            }
        }

        //Try to create a new sibling group, by finding two vacant nodes
        //Try to add a vacant node in sv1, to an existing siblings clique in sv1
        for (Vertex v : (sv1.getInnerVertices())) {
            if (vacancyMap.get(v)) {
                for (Vertex u : sv2.getInnerVertices()) {
                    if (vacancyMap.get(u)) {
                        MyLogger.important("Found vacant pair: " + v + "," + u);
                        assignSuperEdge(se, v, u);
                        return;
                    }
                }
            }
        }
        MyLogger.important("Could not assign " + se + " because all inner pairs are occupied");
    }

    private void assignSuperEdge(Edge se, Vertex v, Vertex u) {
        MyLogger.important("Assigning " + v + "," + u);
        vacancyMap.put(v, false);
        vacancyMap.put(u, false);
        v.addEdge(new BaseEdge(v, u, se.getWeight()));
        u.addEdge(new BaseEdge(u, v, RelationshipProbWeight.switchWeightsDirection((RelationshipProbWeight) se.getWeight())));
        se.getVertex1().removeEdgeTo(se.getVertex2());
        se.getVertex2().removeEdgeTo(se.getVertex1());
        //Add new vertices to eList
        if (!eList.contains(v)) {
            for (Vertex inner : contraction.getWrappingSuperVertex(v.getVertexId()).getInnerVertices())
                eList.add(inner);
            expendedNum++;
        }

        if (!eList.contains(u)) {
            for (Vertex inner : contraction.getWrappingSuperVertex(u.getVertexId()).getInnerVertices())
                eList.add(inner);
            expendedNum++;
        }
    }

    //Check if v can be added to u's sibling clique
    private boolean testSiblingAndAddToClique(Vertex v, Vertex u, Edge se) {

        Vertex V = contractedGraph.getVertex(contraction.getWrappingSuperVertex(v).getId());
        //iterate u's edges
        for (Edge e : u.getEdgeMap().values()) {
            Vertex w = e.getVertex2();
            Vertex W = contractedGraph.getVertex(contraction.getWrappingSuperVertex(w).getId());

            if (W.hasEdgeTo(V.getVertexId())) {
                MyLogger.important("Found third edge : " + W + "," + V);
            } else {
                MyLogger.important("No third edge : " + W + "," + V);
                return false;
            }
        }
        //Has unassigned edges to all siblings of v.
        MyLogger.important("Adding " + v + " to the sibling clique of " + u);
        for (Edge e : u.getEdgeMap().values()) {
            Vertex w = e.getVertex2();
            Vertex W = contractedGraph.getVertex(contraction.getWrappingSuperVertex(w).getId());
            Edge siblingSE = W.getEdgeTo(V.getVertexId());
            assignSuperEdge(siblingSE, v, w);
        }
        assignSuperEdge(se, v, u);
        return true;
    }
}
