package prepare.graph;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A prepare.graph G = (V, E)
 * <p>
 * invariant: all SimpleEdges are directed || all SimpleEdges are undirected
 */
public class SimpleGraph {
    private final Map<SimpleVertex, List<SimpleVertex>> adj = new HashMap<>();
    private final Map<String, SimpleVertex> nameMap = new HashMap<>();
    private final Map<String, SimpleEdge> edges = new HashMap<>();

    /**
     * adds a new SimpleVertex to the prepare.graph
     *
     * @precondition v, or a SimpleVertex u : v.name.equals(u.name) does not belong to G
     */
    public void addVertex(SimpleVertex v) {
        if (!nameMap.containsKey(v.name)) {
            nameMap.put(v.name, v);
            adj.put(v, new LinkedList<>());
        }
    }

    public SimpleVertex getVertex(String name) {
        return nameMap.get(name);
    }

    public void removeVertex(SimpleVertex v) {
        if (nameMap.containsKey(v.name)) {

            List<SimpleVertex> neighbors = getNeighbors(v);
            //remove v from the neighbor lists of its neighbors
            for (SimpleVertex u : neighbors) {
                List<SimpleVertex> uNeighbors = adj.get(u);
                uNeighbors.remove(v);
            }
            nameMap.remove(v.name);
            adj.remove(v);
        } else {
            System.out.println("Trying to remove a non-existent vertex");
        }
    }

    /**
     * adds a new undirected SimpleEdge to the prepare.graph
     *
     * @precondition the SimpleEdge (v1, v2) && (v2, v1) does not belong to E
     */
    public void addEdge(SimpleVertex v1, SimpleVertex v2) {
        addEdge(v1, v2, 1);
    }

    /**
     * adds a new undirected SimpleEdge to the prepare.graph
     *
     * @precondition the SimpleEdge (v1, v2) && (v2, v1) does not belong to E
     */
    public void addEdge(SimpleVertex v1, SimpleVertex v2, double weight) {
        addDirEdge(v1, v2, weight);
        addDirEdge(v2, v1, weight);
    }

    /**
     * Remove both direction edges between v1 and v2
     *
     * @param v1 first vertex
     * @param v2 second vertex
     */
    public void removeEdge(SimpleVertex v1, SimpleVertex v2) {
        this.adj.get(v1).remove(v2);
        this.adj.get(v2).remove(v1);
        this.edges.remove(v1.name + "," + v2.name);
        this.edges.remove(v2.name + "," + v1.name);
    }

    /**
     * adds a new directed SimpleEdge to the prepare.graph
     *
     * @precondition the SimpleEdge (v1, v2) does not belong to E
     */
    public void addDirEdge(SimpleVertex v1, SimpleVertex v2, double weight) {
        adj.get(v1).add(v2);
        edges.put(v1.name + "," + v2.name, new SimpleEdge(v1, v2, weight));
    }

    /**
     * creates a SimpleVertex named <@param name> and adds it to te prepare.graph
     *
     * @precondition the name is unique
     */
    public void createSimpleVertex(String name) {
        createSimpleVertex(name, 1);
    }

    /**
     * creates a SimpleVertex named <@param name> and adds it to te prepare.graph
     *
     * @precondition the name is unique
     */
    public void createSimpleVertex(String name, double weight) {
        SimpleVertex v = new SimpleVertex(name);
        nameMap.put(name, v);
        adj.put(v, new LinkedList<>());
        v.weight = weight;
    }

    /**
     * Creates an undirected SimpleEdge that connects the SimpleVertex named str1 to the SimpleVertex named str2
     * that means each SimpleVertex will add the other to its adjacency list
     *
     * @precondition the SimpleEdge is unique [,  no double SimpleEdges]
     * !str1.equals(str2) [, no loops]
     */
    public void createSimpleEdge(String str1, String str2, double weight) {
        createDirSimpleEdge(str1, str2, weight);
        createDirSimpleEdge(str2, str1, weight);
    }

    /**
     * Creates directed SimpleEdge that connects the SimpleVertex named str1 to the SimpleVertex named str2
     *
     * @precondition the SimpleEdge is unique [,  no double SimpleEdges]
     * !str1.equals(str2) [, no loops]
     */
    public void createDirSimpleEdge(String str1, String str2, double weight) {
        SimpleVertex v1 = nameMap.get(str1);
        SimpleVertex v2 = nameMap.get(str2);
        addDirEdge(v1, v2, weight);
    }

    public List<SimpleVertex> getVertices() {
        List<SimpleVertex> l = new ArrayList<>(adj.keySet());
        l.sort(Comparator.comparing(arg0 -> arg0.name));
        return l;
    }

    public double getEdgeWeight(SimpleVertex v1, SimpleVertex v2) {
        if (edges.get(v1.name + "," + v2.name) != null) {
            return (edges.get(v1.name + "," + v2.name)).weight;
        } else if (edges.get(v2.name + "," + v1.name) != null) {
            return (edges.get(v2.name + "," + v1.name)).weight;
        } else {
            return -1;
        }

    }

    public void setEdgeWeight(SimpleVertex v1, SimpleVertex v2, double weight) {
        (edges.get(v1.name + "," + v2.name)).weight = weight;
    }

    public List<SimpleVertex> getNeighbors(SimpleVertex v) {
        return adj.get(v);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("V={ ");
        for (SimpleVertex v : adj.keySet()) {
            str.append(v.name).append(" ");
        }
        str.append("}\tE={ ");
        for (SimpleVertex v : adj.keySet()) {
            if (!adj.get(v).isEmpty()) {
                for (SimpleVertex u : adj.get(v))
                    str.append("(").append(v.name).append(",").append(u.name).append(")");
            }
        }
        str.append("}");
        return str.toString();
    }

    /**
     * Calculate the sum over all edges between vertices with names like in vSet
     *
     * @param list: list of vertices with names corresponding to vertices in this prepare.graph
     * @return the weight sum
     */
    public double calcSumOfEdgesWeight(List<SimpleVertex> list) {
        double sum = 0;
        for (SimpleVertex v : list) {
            SimpleVertex thisV = getVertex(v.name);
            for (SimpleVertex u : list) {
                SimpleVertex thisU = getVertex(u.name);
                if (adj.get(thisV).contains(thisU)) {
                    sum += edges.get(v.name + "," + u.name).weight;
                }
            }
        }
        return sum;
    }

    /**
     * Inner class - SimpleEdge representation
     */
    public class SimpleEdge {
        public SimpleVertex[] p;
        public double weight;

        public SimpleEdge(SimpleVertex v1, SimpleVertex v2, double weight) {
            this.p = new SimpleVertex[]{v1, v2};
            this.weight = weight;
        }

        @Override
        public String toString() {
            return p[0].name + " " + p[1].name + " " + weight;
        }
    }

    /**
     * Inner class - SimpleVertex representation
     * precondition: ,fields must be initialized in the algorithm
     */
    public class SimpleVertex {
        public int d;
        public int f;
        public SimpleVertex parent, root;
        public String name;
        public Color color;
        public double weight;

        public SimpleVertex(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            return this.name.equals(((SimpleVertex) other).name);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}

