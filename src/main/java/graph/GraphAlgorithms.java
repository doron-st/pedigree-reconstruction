package graph;

import graph.SimpleGraph.SimpleVertex;
import misc.Combinations;

import java.awt.*;
import java.util.List;
import java.util.Queue;
import java.util.*;


public class GraphAlgorithms {

    static private int dfsTimer;
    static private SimpleVertex root;
    static private Queue<SimpleVertex> deque;

    public static void dfs(SimpleGraph G) {
        initialize(G);
        for (SimpleVertex s : G.getVertices()) {
            if (s.color.equals(Color.WHITE)) {
                root = s;
                dfsVisit(G, s);
            }
        }
        buildTree(G);
    }

    /**
     * The recursive body of a DFS
     *
     * @precondition must call initializeDFS(G) before execution
     */
    private static void dfsVisit(SimpleGraph G, SimpleVertex v) {
        v.color = Color.GRAY;
        dfsTimer++;
        v.d = dfsTimer;
        v.root = root;
        for (SimpleVertex u : G.getNeighbors(v))
            if (u.color.equals(Color.WHITE)) {
                u.parent = v;
                dfsVisit(G, u);
            }
        v.color = Color.BLACK;
        dfsTimer++;
        v.f = dfsTimer;
        deque.add(v);
    }

    /**
     * Topologically sorts a directed acyclic graph
     *
     * @return a list of the sorted Vertices
     */
    public static Queue<SimpleVertex> topologicalSort(SimpleGraph G) {
        dfs(G);
        return deque;
    }

    /**
     * Finds the strongly connected components of a directed graph
     */
    public static Map<SimpleVertex, SimpleGraph> SCC(SimpleGraph G) {
        Queue<SimpleVertex> L = topologicalSort(G);
        initialize(G);
        while (!L.isEmpty()) {
            SimpleVertex s = L.poll();
            if (s.color.equals(Color.WHITE)) {
                root = s;
                dfsVisit(G, s);
            }
        }
        SimpleGraph dfsForest = buildTree(G);

        Map<SimpleVertex, SimpleGraph> CCMap = new HashMap<>();
        //add vertices to CC map
        for (SimpleVertex v : dfsForest.getVertices()) {
            if (CCMap.containsKey(v.root)) {
                CCMap.get(v.root).addVertex(v);
            } else {
                SimpleGraph g = new SimpleGraph();
                g.addVertex(v);
                CCMap.put(v.root, g);
            }
        }
        //SimpleGraph inter = new SimpleGraph();
        //add edges to CC map
        for (SimpleVertex v : dfsForest.getVertices()) {
            SimpleGraph g = CCMap.get(v.root);
            //if(v.name.equals("13473")){
            //inter = g;
            //}
            for (SimpleVertex u : G.getNeighbors(v)) {
                if (!g.getNeighbors(v).contains(u))
                    g.addEdge(v, u);
            }
        }
        //	System.out.println(inter);
        return CCMap;
    }

    /**
     * Find all the cliques of specified size in graph G
     *
     * @param G:         input SimpleGraph
     * @param cliqueSize size of clique
     * @return a list of SimpleGraphs, each contains the verteces of a clique (no edges)
     */
    public static List<SimpleGraph> findCliques(SimpleGraph G, int cliqueSize) {

        SimpleVertex[] vertices = Arrays.asList(G.getVertices().toArray()).toArray(new SimpleVertex[G.getVertices().toArray().length]);
        int n = vertices.length;

        int[] vertexGroup = new int[cliqueSize];
        int total = Combinations.choose(n, cliqueSize);

        List<SimpleGraph> cliques = new ArrayList<>();

        //For each possible combination sizes cliqueSize
        for (int i = 0; i < total; i++) {
            int ind = 0;
            for (int x : Combinations.element(n, cliqueSize, i)) {
                vertexGroup[ind] = x;
                ind++;
            }

            //Check if combination is a clique
            boolean isClique = true;
            //for each vertex in the combination
            for (ind = 0; ind < cliqueSize && isClique; ind++) {
                SimpleVertex v = vertices[vertexGroup[ind]];

                List<SimpleVertex> neighbors = G.getNeighbors(v);
                //check if there is an edge to all other vertices in the combination
                for (int ind2 = ind + 1; ind2 < cliqueSize; ind2++) {
                    SimpleVertex u = vertices[vertexGroup[ind2]];
                    //v, or the edge from v to u might have been removed because v or u were in a clique
                    if (neighbors == null || neighbors.isEmpty() || !neighbors.contains(u)) {
                        isClique = false;
                        break;
                    }
                }
            }
            if (isClique) {
                SimpleGraph g = new SimpleGraph();
                for (ind = 0; ind < cliqueSize; ind++) {
                    g.addVertex(vertices[vertexGroup[ind]]);

                }
                cliques.add(g);
            }
        }

        return cliques;
    }

    /**
     * builds a DFS forest or a BFS tree by parent values of the Vertices
     *
     * @precondition called inside a DFS || BFS rutine, as the return value
     */
    private static SimpleGraph buildTree(SimpleGraph G) {
        SimpleGraph forest = new SimpleGraph();
        for (SimpleVertex v : G.getVertices())
            forest.addVertex(v);
        for (SimpleVertex v : G.getVertices()) {
            if (v.parent != null)
                forest.addDirEdge(v.parent, v, 1);
        }
        return forest;
    }

    /**
     * initialize a graph before a search algorithm
     *
     * @precondition must be called first thing in a DFS/BFS rutine
     */
    private static void initialize(SimpleGraph G) {
        //initialization
        dfsTimer = 0;
        deque = new LinkedList<>();
        for (SimpleVertex v : G.getVertices()) {
            v.color = Color.WHITE;
            v.parent = null;
            v.d = Integer.MAX_VALUE;
            v.f = Integer.MAX_VALUE;
        }
    }
}