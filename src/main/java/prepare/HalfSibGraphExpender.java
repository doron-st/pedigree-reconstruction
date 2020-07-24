package prepare;

import graph.*;
import relationship.RelationshipProbWeight;
import simulator.Pedigree;

import java.util.ArrayList;
import java.util.List;

public class HalfSibGraphExpender {

    private Graph contractedGraph;
    private Contraction contraction;//sibGroup contraction
    private Pedigree ped;
    private Graph expendedGraph;

    public HalfSibGraphExpender(Graph nucFamGraph, Pedigree ped, Contraction contraction) {
        this.contractedGraph = nucFamGraph;
        this.ped = ped;
        this.contraction = contraction;
    }

    /**
     * Run Expension algorithm:
     *
     * @return - The expended relationships graph
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
            List<Integer> allMateIDs = new ArrayList<Integer>();


            for (Vertex sib : ((SuperVertex) sv.getData()).getInnerVertices()) {
                int id = sib.getVertexId();
                List<Integer> mateIDs = ped.getMates(id);
                allMateIDs.addAll(mateIDs);
                MyLogger.important("Added mates " + mateIDs);

                for (Edge se : sv.getEdgeMap().values()) {
                    Vertex potentSib = se.getVertex2();

                    //treat only ML halfsibs
                    if (!((RelationshipProbWeight) sv.getEdgeTo(potentSib.getVertexId()).getWeight()).isMaxProbCategory("halfSib"))
                        continue;

                    double myProb = ((RelationshipProbWeight) se.getWeight()).getProb("halfSib");

                    for (int mateID : mateIDs) {
                        Vertex mate = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID).getId());
                        if (mate.hasEdgeTo(potentSib.getVertexId())) {
                            //first priority - size of the siblings group
                            if (((SuperVertex) sv.getData()).getInnerVertices().size() > ((SuperVertex) mate.getData()).getInnerVertices().size()) {
                                MyLogger.important("Mate " + mate + " has halfSibEdge to " + potentSib + " with less individuals then " + sv + ", removing edge");
                                mate.removeEdgeTo(potentSib);
                                potentSib.removeEdgeTo(mate);
                            } else if (((SuperVertex) sv.getData()).getInnerVertices().size() < ((SuperVertex) mate.getData()).getInnerVertices().size())
                                continue;
                                //If sibling group sizes are equal, remove lower prob edge
                            else if (((RelationshipProbWeight) mate.getEdgeTo(potentSib.getVertexId()).getWeight()).isMaxProbCategory("halfSib")) {
                                Edge mateEdge = mate.getEdgeTo(potentSib.getVertexId());
                                double mateProb = ((RelationshipProbWeight) mateEdge.getWeight()).getProb("halfSib");
                                if (mateProb < myProb) {
                                    MyLogger.important("Mate " + mate + " has halfSibEdge to " + potentSib + " with lower probability then " + sv + ", removing edge");
                                    mate.removeEdgeTo(potentSib);
                                }
                            }
                        }
                    }//for each mate
                }//for each sib
            }//for each suoer-edge

            //dispose common parent between mates, cause are already siblings
            for (int mateID1 : allMateIDs) {
                for (int mateID2 : allMateIDs) {
                    Vertex mate1 = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID1).getId());
                    Vertex mate2 = contractedGraph.getVertex(contraction.getWrappingSuperVertex(mateID2).getId());

                    if (mate1.hasEdgeTo(mateID2) && ((RelationshipProbWeight) mate1.getEdgeTo(mateID2).getWeight()).isMaxProbCategory("halfSib")) {

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
