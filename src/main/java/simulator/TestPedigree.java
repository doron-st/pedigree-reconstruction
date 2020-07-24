package simulator;
import static org.junit.Assert.*;

import graph.MyLogger;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;



public class TestPedigree {

	@Test
	public void test() {
		Pedigree p= new Pedigree();
		for(int i=1;i<10;i++)
			p.addVertex(i);
		
		p.addVertex(11,1,2,true);
		p.addVertex(12,1,2,true);
		p.addVertex(13,3,4,true);
		p.addVertex(14,5,6,true);
		p.addVertex(17,7,8,true);
		
		Map<Integer, Integer> idConversion = new HashMap<Integer, Integer>();
		Map<Integer, Integer> enumartionTable = new HashMap<Integer, Integer>();
		
		Pedigree p1 = p.extractSubPedigree(p.getVertex(1), p.getVertex(3), idConversion, enumartionTable);
		Pedigree p2 = p.extractSubPedigree(p.getVertex(1), p.getVertex(7), idConversion, enumartionTable);
		assertEquals(p1,p2);;
		
		p1 = p.extractSubPedigree(p.getVertex(1), p.getVertex(3), idConversion, enumartionTable);
		p2 = p.extractSubPedigree(p.getVertex(1), p.getVertex(4), idConversion, enumartionTable);
		
		MyLogger.important(p1 + "\n");
		MyLogger.important(p2 + "\n");
		assertEquals(p1,p2);;
	}

}
