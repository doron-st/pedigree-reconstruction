package prepare.pedreconstruction;

import prepare.graph.*;
import prepare.misc.MyLogger;
import prepare.relationship.RelationshipProbWeight;
import prepare.pedigree.Pedigree;

import java.util.ArrayList;
import java.util.List;

import static prepare.relationship.Relationship.HALF_SIB;

public class HalfSibGraphExpander {
    private final Graph contractedGraph;
    private final Contraction contraction;//sibGroup contraction
    private final Pedigree ped;
    private Graph expendedGraph;

    public HalfSibGraphExpander(Graph nucFamGraph, Pedigree ped, Contraction contraction) {
        this.contractedGraph = nucFamGraph;
        this.ped = ped;
        this.contraction = contraction;
    }

    /**
     * Run Expansion algorithm:
     *
     * @return - The expended relationships prepare.graph
     */
    public Graph run() {
        removeRedundantEdges();
        expandSibGroups();
        return expendedGraph;
    }

    private void removeRedundantEdges() {

        //get mates of all siblings
        for (Vertex sv : contractedGraph.getVertices()) {

            MyLogger.important("sv=" + sv + " svid=" + sv.getVertexId());
            List<Integer> allMateIDs = new ArrayList<>();


            for (Vertex sib : ((SuperVertex) sv.getData()).getInnerVertices()) {
                int id = sib.getVertexId();
                List<Integer> mateIDs = ped.getMates(id);
                allMateIDs.addAll(mateIDs);
                MyLogger.important("Added mates " + mateIDs);

                for (Edge se : sv.getEdgeMap().values()) {
                    Vertex potentSib = se.getVertex2();

                    //treat only ML half-sibs
                    if (!((RelationshipProbWeight) sv.getEdgeTo(potentSib.getVertexId()).getWeight()).isMaxProbCategory(HALF_SIB))
                        continue;

                    double myProb = ((RelationshipProbWeight) se.getWeight()).getProb(HALF_SIB);

                    for (int mateID : mateIDs) {
                        Vertex mate = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID).getId());
                        if (mate.hasEdgeTo(potentSib.getVertexId())) {
                            //first priority - size of the siblings group
                            if (((SuperVertex) sv.getData()).getInnerVertices().size() > ((SuperVertex) mate.getData()).getInnerVertices().size()) {
                                MyLogger.important("Mate " + mate + " has halfSibEdge to " + potentSib + " with less individuals then " + sv + ", removing edge");
                                mate.removeEdgeTo(potentSib);
                                potentSib.removeEdgeTo(mate);
                            } else {
                                if (((SuperVertex) sv.getData()).getInnerVertices().size() >= ((SuperVertex) mate.getData()).getInnerVertices().size()) {
                                    if (((RelationshipProbWeight) mate.getEdgeTo(potentSib.getVertexId()).getWeight()).isMaxProbCategory(HALF_SIB)) {
                                        Edge mateEdge = mate.getEdgeTo(potentSib.getVertexId());
                                        double mateProb = ((RelationshipProbWeight) mateEdge.getWeight()).getProb(HALF_SIB);
                                        if (mateProb < myProb) {
                                            MyLogger.important("Mate " + mate + " has halfSibEdge to " + potentSib + " with lower probability then " + sv + ", removing edge");
                                            mate.removeEdgeTo(potentSib);
                                        }
                                    }
                                }
                                //If sibling group sizes are equal, remove lower prob edge
                            }

                        }
                    }//for each mate
                }//for each sib
            }//for each super-edge

            //dispose prepare.common parent between mates, cause are already siblings
            for (int mateID1 : allMateIDs) {
                for (int mateID2 : allMateIDs) {
                    Vertex mate1 = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID1).getId());
                    Vertex mate2 = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID2).getId());

                    if (mate1.hasEdgeTo(mateID2) && ((RelationshipProbWeight) mate1.getEdgeTo(mateID2).getWeight()).isMaxProbCategory(HALF_SIB)) {

                        mate1.removeEdgeTo(mate2);
                        mate2.removeEdgeTo(mate1);
                        MyLogger.important(mateID1 + " has half-sibling edge to " + mateID2 + " although their mates are siblings " + sv);
                    }
                }
            }
        }
    }


    private void expandSibGroups() {
        List<Vertex> innerList = new ArrayList<>();
        for (Vertex sv : contractedGraph.getVertices()) {
            innerList.addAll(((SuperVertex) sv.getData()).getInnerVertices());
        }

        expendedGraph = new Graph(innerList);

        for (Vertex sv : contractedGraph.getVertices()) {
            for (Edge se : sv.getEdgeMap().values()) {
                Vertex su = se.getVertex2();
                for (Vertex sib1 : ((SuperVertex) sv.getData()).getInnerVertices()) {
                    for (Vertex sib2 : ((SuperVertex) su.getData()).getInnerVertices()) {
                        Edge e = new BaseEdge(sib1, sib2, se.getWeight());
                        expendedGraph.addEdge(e);
                    }
                }
            }
        }
    }
}
