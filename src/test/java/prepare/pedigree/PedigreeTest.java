package prepare.pedigree;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PedigreeTest {

    @Test
    public void testConstruction(){
        Pedigree pedigree = new Pedigree();
        pedigree.addVertex(1);
        pedigree.addVertex(2);
        pedigree.addVertex(3, 1, 2, true);
        assertEquals(3, pedigree.getVertices().size());
        assertEquals(1, pedigree.getLiving().size());
        assertEquals(2, pedigree.getFounders().size());
        assertEquals(new Integer(2), pedigree.getMates(1).get(0));
        assertEquals(new Integer(1), pedigree.getMates(2).get(0));
        assertEquals(1, pedigree.getVertex(3).getFatherId());
        assertEquals(2, pedigree.getVertex(3).getMotherId());
    }

    @Test
    public void testGetNewId(){
        Pedigree pedigree = new Pedigree();
        pedigree.addVertex(1);
        pedigree.addVertex(2);
        pedigree.addVertex(3, 1, 2, true);
        assertEquals(4, pedigree.getNewID());

        pedigree = new Pedigree();
        pedigree.addVertex(6);
        pedigree.addVertex(7);
        pedigree.addVertex(8, 6, 7, true);
        assertEquals(9, pedigree.getNewID());
    }

    @Test
    public void testToString(){
        Pedigree pedigree = new Pedigree();
        pedigree.addVertex(1);
        pedigree.addVertex(2);
        pedigree.addVertex(3, 1, 2, true);
        assertEquals("id: 1, father: -1, mother: -1, isAlive: false, isFounder: true," +
                "id: 2, father: -1, mother: -1, isAlive: false, isFounder: true," +
                "id: 3, father: 1, mother: 2, isAlive: true, isFounder: false,", pedigree.toString());
    }
}
