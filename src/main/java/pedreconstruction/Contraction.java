package pedreconstruction;

import graph.*;
import misc.MyLogger;
import pedigree.NucFamily;
import pedigree.Person;
import pedigree.Pedigree;
import pedigree.Pedigree.PedVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Contraction {

    Map<Integer, SuperVertex> contractionMap = new HashMap<>();
    List<SuperVertex> contractedNodes = new ArrayList<>();
    List<Vertex> unexpandedNodes = new ArrayList<>();

    /**
     * Create a contraction from the founder pedigree vertices
     *
     * @param p pedigree
     */
    public Contraction(Pedigree p) {

        for (PedVertex v1 : p.getFounders()) {

            List<PedVertex> descendants1 = p.getDescendants(v1.getId());
            List<PedVertex> livingDescendants1 = p.getDescendants(v1.getId());
            for (PedVertex pv : descendants1) {
                if (!pv.isAlive())
                    livingDescendants1.remove(pv);
            }
            //descendants1.remove(v1);
            //System.out.println(v1 + " " + livingDescendants1);

            for (PedVertex v2 : p.getFounders()) {
                if (v2.isAlive())
                    continue;

                List<PedVertex> descendants2 = p.getDescendants(v2.getId());
                List<PedVertex> livingDescendants2 = p.getDescendants(v2.getId());
                for (PedVertex pv : descendants2) {
                    if (!pv.isAlive())
                        livingDescendants2.remove(pv);
                }
                //descendants2.remove(v2);

                int vid1 = v1.getId();
                int vid2 = v2.getId();

                if (vid1 >= vid2) continue;//Do only one side calculation, don't self compare


                if (livingDescendants1.containsAll(livingDescendants2) && livingDescendants2.containsAll(livingDescendants1)) {
                    MyLogger.debug("Contracting " + v1 + "," + v2);
                    Person p1 = p.getPopulation().getPerson(vid1);
                    Person p2 = p.getPopulation().getPerson(vid2);


                    //if both are yet to be contracted
                    if (contractionMap.get(vid1) == null && contractionMap.get(vid1) == null) {
                        MyLogger.debug("contract " + vid1 + " & " + vid2);
                        SuperVertex contractedNode = new SuperVertex(new BaseVertex(p1));
                        contractedNode.addVertex(new BaseVertex(p2));
                        contractedNodes.add(contractedNode);
                        contractionMap.put(v1.getId(), contractedNode);
                        contractionMap.put(v2.getId(), contractedNode);
                    }
                    //If vid1 is already contracted and vid2 is not in the same super vertex
                    else if (contractionMap.get(vid1) != null && contractionMap.get(vid1) != contractionMap.get(vid2)) {
                        MyLogger.debug("Adding " + vid2 + " to " + contractionMap.get(vid1));
                        //add p2 to the super-vertex of vid1
                        contractionMap.get(vid1).addVertex(new BaseVertex(p2));
                        //set this super-vertex as the contraction of vid2
                        contractionMap.put(vid2, contractionMap.get(vid1));
                    } else if (contractionMap.get(vid2) != null && contractionMap.get(vid1) != contractionMap.get(vid2)) {
                        MyLogger.debug("Adding " + vid1 + " to " + contractionMap.get(vid2));
                        contractionMap.get(vid2).addVertex(new BaseVertex(p1));
                        contractionMap.put(vid1, contractionMap.get(vid2));
                    }
                }
            }
            //If no one to contract with, create singleton super vertex
            if (contractionMap.get(v1.getId()) == null) {
                Person p1 = p.getPopulation().getPerson(v1.getId());
                SuperVertex sv = new SuperVertex(new BaseVertex(p1));
                contractedNodes.add(sv);
                contractionMap.put(v1.getId(), sv);
                MyLogger.debug("Contracting " + v1);

            }
            MyLogger.info("SuperVertex of " + v1 + " is" + contractionMap.get(v1.getId()));
        }
    }

    /**
     * create sibling contraction
     **/
    public Contraction(List<NucFamily> nucFamilies) {
        for (NucFamily fam : nucFamilies) {
            SuperVertex sv = new SuperVertex(new BaseVertex(fam.siblings.get(0)));
            for (Person sib : fam.siblings)
                if (sib != fam.siblings.get(0))
                    sv.addVertex(new BaseVertex(sib));
            contractedNodes.add(sv);
            for (Person sib : fam.siblings)
                contractionMap.put(sib.getId(), sv);
        }
    }

    public List<SuperVertex> getSuperVertices() {
        return contractedNodes;
    }

    /**
     * Applies the vertex contraction on g
     */
    public Graph createEdgelessContractedGraph() {
        List<VertexData> dataList = new ArrayList<>(contractedNodes);
        return new Graph(dataList);
    }

    public SuperVertex getWrappingSuperVertex(Vertex v) {
        return contractionMap.get(v.getVertexId());
    }

    public SuperVertex getWrappingSuperVertex(int id) {
        return contractionMap.get(id);
    }

    public void addUnexpandedList(List<Vertex> uList) {
        unexpandedNodes = uList;

    }

}