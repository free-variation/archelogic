import static org.junit.Assert.*;

import org.junit.Test;

import archelogic.MicroKanren;
import archelogic.MicroKanren.Stream;

public class TestMicroKanren {

	@Test
	public void testCons() {
		MicroKanren.Cons c1 = MicroKanren.cons(10, 20);
		assertEquals((Integer) c1.car, (Integer) 10);
		assertEquals((Integer) c1.cdr, (Integer) 20);

		MicroKanren.Cons c2 = MicroKanren.cons(30, c1);
		assertEquals((Integer) c2.car, (Integer) 30);
		assertEquals(c2.cdr, c1);
	}

	@Test
	public void testIsPair() {
		MicroKanren.Cons c1 = MicroKanren.cons(10, 20);
		assertEquals(MicroKanren.isPair(c1), true);
		assertEquals(MicroKanren.isPair(20), false);
	}
	
	@Test 
	public void testNth() {
		MicroKanren.Cons c1 = MicroKanren.cons(10, 20);
		assertEquals(10, c1.nth(0));
		assertEquals(20, c1.nth(1));
		
		MicroKanren.Cons c2 = MicroKanren.cons(30, c1);
		assertEquals(30, c2.nth(0));
		assertEquals(10, c2.nth(1));
		assertEquals(20, c2.nth(2));
		
		MicroKanren.Cons list = MicroKanren.list(10, 20, 30, 40);
		assertEquals(40, list.nth(3));
		
	}
	
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testNthException() {
		MicroKanren.Cons list = MicroKanren.list(10, 20, 30, 40);
		assertEquals(40, list.nth(4));
	}
	
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testNthExceptionNegIndex() {
		MicroKanren.Cons list = MicroKanren.list(10, 20, 30, 40);
		assertEquals(40, list.nth(-1));
	}
	
	@Test
	public void testCount() {
		assertEquals(0, MicroKanren.nil.count());
		assertEquals(1, MicroKanren.cons(10, null).count());
		assertEquals(2, MicroKanren.cons(10, 20).count());
		assertEquals(1, MicroKanren.list(10).count());
		assertEquals(0, MicroKanren.list().count());
		assertEquals(4, MicroKanren.list(10, 20, 30, 40).count());
	}
	
	@Test
	public void testToArray() {
		assertEquals(new Object[0], MicroKanren.nil.toArray());
		assertEquals(new Object[] {10}, MicroKanren.cons(10, null).toArray());
		assertEquals(new Object[] {10, 20}, MicroKanren.cons(10, 20).toArray());
		assertEquals(new Object[] {10, 20, 30}, MicroKanren.list(10, 20, 30).toArray());
		assertEquals(new Object[] {10, 20, 30, 40}, MicroKanren.list(10, 20, 30, 40).toArray());
	
	}


	@Test
	public void testMap() {
		MicroKanren.Cons a = MicroKanren.list(1, 2, 3, 4);
		MicroKanren.Cons b = a.map((i) -> (Integer) i + 1);
		assertEquals(2, b.car);
		assertEquals(3, ((MicroKanren.Cons) b.cdr).car);

		assertEquals(MicroKanren.nil, MicroKanren.nil.map((o) -> o));
	}

	@Test
	public void testVar() {
		MicroKanren.LVar v1 = MicroKanren.var(1);
		MicroKanren.LVar v2 = MicroKanren.var(1);

		assertEquals(MicroKanren.isVar(v1), true);
		assertEquals(MicroKanren.isVar(v2), true);
		assertEquals(v1.id, v2.id);
		assertEquals(v1.toString(), "<1>");
		assertEquals(v1.hashCode(), 1);
	}

	@Test
	public void testIsVar() {
		MicroKanren.LVar v1 = MicroKanren.var(1);

		assertEquals(MicroKanren.isVar(v1), true);
		assertEquals(MicroKanren.isVar(10), false);
	}

	@Test
	public void testWalk() {
		MicroKanren.Subst s = new MicroKanren.Subst();
		MicroKanren.LVar v1 = MicroKanren.var(1);
		MicroKanren.LVar v2 = MicroKanren.var(2);

		s = MicroKanren.extend(v1, 10, s);
		assertEquals(10, MicroKanren.walk(v1, s));
		assertEquals(v2, MicroKanren.walk(v2, s));

		s = MicroKanren.extend(v2, v1, s);
		assertEquals(10, MicroKanren.walk(v2, s));
	}

	@Test
	public void testExtend() {
		MicroKanren.Subst s = new MicroKanren.Subst();
		MicroKanren.LVar v = MicroKanren.var(0);
		s = MicroKanren.extend(v, 10, s);
		assertEquals(s.get(v), 10);
		assertEquals(s.get(MicroKanren.var(1)), null);
	}

	@Test
	public void testUnify() {
		MicroKanren.Subst s = new MicroKanren.Subst();
		MicroKanren.LVar v1 = MicroKanren.var(1);
		MicroKanren.LVar v2 = MicroKanren.var(2);

		s = MicroKanren.unify(v2, v1, s);
		assertEquals(v1, MicroKanren.walk(v2, s));

		s = MicroKanren.extend(v1, 10, s);
		assertEquals(10, MicroKanren.walk(v2, s));

	}

	@Test
	
	public void testUnifyConses() {
		MicroKanren.Subst s = new MicroKanren.Subst();
		MicroKanren.LVar v1 = MicroKanren.var(1);
		MicroKanren.LVar v2 = MicroKanren.var(2);
		MicroKanren.Cons c1 = MicroKanren.cons(v1, 10);
		MicroKanren.Cons c2 = MicroKanren.cons(v2, 10);
		MicroKanren.Cons c3 = MicroKanren.cons(10, 20);

		s = MicroKanren.unify(c1, c2, s);
		assertEquals(v2, s.get(v1));
		assertEquals(MicroKanren.unify(c1, c3, s), null);

	}


	@Test
	public void testEquals() {
		// equal ground terms
		MicroKanren.Goal g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.equals(5, 5));
		MicroKanren.Stream s = (Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);

		// unequal ground terms
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.equals(4, 5));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(s, null);

		// default vars, left and right
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.equals(4, MicroKanren.joker));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);
		// no entry for default var in subst
		assertTrue(((MicroKanren.State)s.get(0)).subst.isEmpty());

		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.equals(MicroKanren.joker, 4));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);
		assertTrue(((MicroKanren.State)s.get(0)).subst.isEmpty());

		// fix a var to ground term, left and right
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.equals(v, 4));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)), 4);

		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.equals(5, v));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)), 5);

		// share vars
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren
				.equals(u, v)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(1)), MicroKanren.var(0));

		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren
				.equals(v, u)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)), MicroKanren.var(1));

		// assign to pair of variables
		MicroKanren.Cons c = MicroKanren.cons(10, 20);
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren
				.equals(MicroKanren.cons(u, v), c)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)), 20);
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(1)), 10);

		// unfiy more complex structure
		MicroKanren.Cons c1 = MicroKanren.cons(30, c);
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren
				.equals(MicroKanren.cons(u, v), c1)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)), c);
		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(1)), 30);

		// life without occurs-check...
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> {
			MicroKanren.Cons l1 = MicroKanren.list(1, 2, 3, v, 5);
			MicroKanren.Cons l2 = MicroKanren.list(v, 2, 3, 4, 5);
			return MicroKanren.equals(l1, l2);
		});
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);

		// lists of unequal length
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> {
			MicroKanren.Cons l1 = MicroKanren.list(1, 2, 3, 4, 5);
			MicroKanren.Cons l2 = MicroKanren.list(v, 2, 3, v);
			return MicroKanren.equals(l1, l2);
		});
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);

		g = MicroKanren.callFresh((MicroKanren.LVar v) -> {
			MicroKanren.Cons l1 = MicroKanren.list(1, 2, 3, 4);
			MicroKanren.Cons l2 = MicroKanren.list(v, 2, 3, 4, v);
			return MicroKanren.equals(l1, l2);
		});
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);

	}

	@Test
	public void testDisj() {
		MicroKanren.Goal g = MicroKanren
				.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren.disj(
						MicroKanren.equals(v, 5), MicroKanren.equals(v, 6))));

		MicroKanren.Stream s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());

		assertEquals(((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)), 5);
		assertEquals(((MicroKanren.State)s.get(1)).subst.get(MicroKanren.var(0)), 6);
	}

	@Test
	public void testConj() {
		MicroKanren.Goal g = MicroKanren
				.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren.conj(
						MicroKanren.equals(v, 5), MicroKanren.equals(v, 6))));

		MicroKanren.Stream s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);

		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren
				.conj(MicroKanren.equals(v, 5), MicroKanren.equals(u, 6))));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(5, ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)));
		assertEquals(6, ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(1)));
	}

	@Test
	public void testConso() {
		// ground atomic values
		MicroKanren.Goal g = MicroKanren.conso(10, 20, MicroKanren.cons(10, 20));
		MicroKanren.Stream s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);

		// ground conses
		g = MicroKanren.conso(10, MicroKanren.list(20, 30), MicroKanren.list(10, 20, 30));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);

		// ground conses, failure
		g = MicroKanren.conso(10, MicroKanren.cons(20, 30), MicroKanren.list(10, 20, 40));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(s, null);

		// can't split an empty list
		g = MicroKanren.conso(MicroKanren.joker, MicroKanren.joker, MicroKanren.nil);
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);


		// complex head
		g = MicroKanren.conso(MicroKanren.cons(10, 20), MicroKanren.cons(30, 40),
				MicroKanren.cons(MicroKanren.cons(10, 20), MicroKanren.cons(30, 40)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(s, null);

		// var output
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.conso(10, MicroKanren.cons(20, 30), v));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(MicroKanren.cons(10, MicroKanren.cons(20, 30)), ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)));

		// var embedded in output
		// complex head
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.conso(MicroKanren.cons(10, 20),
				MicroKanren.cons(30, 40), MicroKanren.cons(MicroKanren.cons(10, v), MicroKanren.cons(30, 40))));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(20, ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)));

		// destructuring head
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.conso(v, MicroKanren.cons(20, 30),
				MicroKanren.cons(10, MicroKanren.cons(20, 30))));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(10, ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)));

		// destructuring dotted pair
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.conso(10, v, MicroKanren.cons(10, 20)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(20, ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)));

		// destructuring list
		g = MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren.conso(10, v,
				MicroKanren.cons(10, MicroKanren.cons(20, 30))));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(MicroKanren.cons(20, 30), ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(0)));

		// head-tail destructuring
		g = MicroKanren.callFresh((MicroKanren.LVar u) -> MicroKanren.callFresh((MicroKanren.LVar v) -> MicroKanren
				.conso(u, v, MicroKanren.cons(10, MicroKanren.cons(20, 30)))));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(10, (((MicroKanren.State)s.get(0))).subst.get(MicroKanren.var(0)));
		assertEquals(MicroKanren.cons(20, 30), ((MicroKanren.State)s.get(0)).subst.get(MicroKanren.var(1)));

		// destructuring the empty list fails
		g = MicroKanren.callFresh(
				(u) -> MicroKanren.callFresh(
						(v) -> MicroKanren.conso(u, v, MicroKanren.nil)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);
	}

	@Test
	public void testEmptyo() {
		MicroKanren.Stream s = (MicroKanren.Stream) MicroKanren.emptyo(MicroKanren.cons(1, 2)).run(MicroKanren.emptyState());
		assertEquals(null, s);

		s = (MicroKanren.Stream) MicroKanren.emptyo(MicroKanren.list(1, 2)).run(MicroKanren.emptyState());
		assertEquals(null, s);

		s = (MicroKanren.Stream) MicroKanren.emptyo(MicroKanren.nil).run(MicroKanren.emptyState());
		assertNotEquals(null, s);

		s = (MicroKanren.Stream) MicroKanren.emptyo(MicroKanren.list()).run(MicroKanren.emptyState());
		assertNotEquals(null, s);
		
		Object [] fullArray  = {1, 2};
		s = (MicroKanren.Stream) MicroKanren.emptyo(fullArray).run(MicroKanren.emptyState());
		assertEquals(null, s);
		
		Object [] emptyArray  = {};
		s = (MicroKanren.Stream) MicroKanren.emptyo(emptyArray).run(MicroKanren.emptyState());
		assertNotEquals(null, s);
		
		MicroKanren.Goal g = MicroKanren.callFresh((v) -> MicroKanren.emptyo(v));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertNotEquals(null, s);
		assertEquals(MicroKanren.nil, (((MicroKanren.State)s.get(0))).subst.get(MicroKanren.var(0)));
		
		g = MicroKanren.callFresh((v) -> MicroKanren.conj(
				MicroKanren.equals(v, 10),
				MicroKanren.emptyo(v)));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(null, s);
	}

	@Test
	public void testNot() {
		MicroKanren.Stream s = (MicroKanren.Stream) MicroKanren.not(MicroKanren.succeed).run(MicroKanren.emptyState());
		assertEquals(null, s);

		s = (MicroKanren.Stream) MicroKanren.not(MicroKanren.fail).run(MicroKanren.emptyState());
		assertNotEquals(null, s);
	}

	@Test
	public void testAppendo() {
		// ground lists

		MicroKanren.Stream s = MicroKanren.pull(
				MicroKanren.appendo(
						MicroKanren.list(1, 2), 
						MicroKanren.list(3, 4),
						MicroKanren.list(1, 2, 3, 4)).run(MicroKanren.emptyState()));
		assertNotEquals(s, null);

		s =  MicroKanren.pull( MicroKanren.appendo(MicroKanren.list(1, 2, 3), MicroKanren.list(3, 4),
				MicroKanren.list(1, 2, 3, 4)).run(MicroKanren.emptyState()));
		assertEquals(null, s);

		// conses and lists: fails
		s =  MicroKanren.pull( MicroKanren.appendo(MicroKanren.cons(1, 2), MicroKanren.list(3, 4), MicroKanren.list(1, 2, 3, 4)).run(
				MicroKanren.emptyState()));
		assertEquals(null, s);

		// right var
		MicroKanren.Goal g = MicroKanren.callFresh(
				(v) -> MicroKanren.appendo(MicroKanren.list(1, 2), MicroKanren.list(3, 4), v));
		s = MicroKanren.pull(g.run(MicroKanren.emptyState()));
		assertEquals(MicroKanren.list(1, 2, 3, 4), MicroKanren.deepWalk(MicroKanren.var(0), (((MicroKanren.State)s.get(0))).subst));

		// leftmost var
		g = MicroKanren.callFresh(
				(v) -> MicroKanren.appendo(v, MicroKanren.list(3, 4), MicroKanren.list(1, 2, 3, 4)));
		s =  MicroKanren.pull(g.run(MicroKanren.emptyState()));
		assertEquals(MicroKanren.list(1, 2), MicroKanren.deepWalk(MicroKanren.var(0), (((MicroKanren.State)s.get(0))).subst));

		// middle var
		g = MicroKanren.callFresh(
				(v) -> MicroKanren.appendo(MicroKanren.list(1, 2), v, MicroKanren.list(1, 2, 3, 4)));
		s =  MicroKanren.pull(g.run(MicroKanren.emptyState()));
		assertEquals(MicroKanren.list(3, 4), MicroKanren.deepWalk(MicroKanren.var(0), (((MicroKanren.State)s.get(0))).subst));

		// middle var, empty
		g = MicroKanren.callFresh(
				(v) -> MicroKanren.appendo(MicroKanren.list(1, 2, 3, 4), v, MicroKanren.list(1, 2, 3, 4)));
		s =  MicroKanren.pull(g.run(MicroKanren.emptyState()));
		assertEquals(MicroKanren.nil, MicroKanren.deepWalk(MicroKanren.var(0), (((MicroKanren.State)s.get(0))).subst));

		// embedded var
		g = MicroKanren.callFresh(
				(v) -> MicroKanren.appendo(MicroKanren.list(1, 2), MicroKanren.list(3, v), MicroKanren.list(1, 2, 3, 4)));
		s =  MicroKanren.pull(g.run(MicroKanren.emptyState()));
		assertEquals(4, MicroKanren.deepWalk(MicroKanren.var(0), (((MicroKanren.State)s.get(0))).subst));

		// full destructuring
		g = MicroKanren.callFresh(
				(u) -> MicroKanren.callFresh(
						(v) ->  MicroKanren.appendo(u, v, MicroKanren.list(1, 2, 3, 4))));
		s =  MicroKanren.pullAll(g.run(MicroKanren.emptyState()));
		assertEquals(5, s.size());

	}


	@Test
	public void testInfiniteStreams() {
		MicroKanren.Goal g = MicroKanren.callFresh(
				(u) -> MicroKanren.callFresh(
						(v) ->  MicroKanren.callFresh(
								(w) -> MicroKanren.appendo(u, v, w))));

		MicroKanren.IStream s = g.run(MicroKanren.emptyState());
		assertTrue(s instanceof MicroKanren.Stream);
		MicroKanren.ISolution s1 = ((MicroKanren.Stream)s).get(1);
		assertTrue(s1 instanceof MicroKanren.ImmatureStream);
	}

	@Test
	public void testTake() {
		MicroKanren.Goal g = MicroKanren.callFresh(
				(u) -> MicroKanren.callFresh(
						(v) ->  MicroKanren.callFresh(
								(w) -> MicroKanren.appendo(u, v, w))));
		MicroKanren.IStream s = g.run(MicroKanren.emptyState());

		assertEquals(0, MicroKanren.take(0, s).size());
		assertEquals(1, MicroKanren.take(1, s).size());
		assertEquals(2, MicroKanren.take(2, s).size());
		assertEquals(5, MicroKanren.take(5, s).size());
		assertEquals(10, MicroKanren.take(10, s).size());

		g = MicroKanren.callFresh(
				(u) -> MicroKanren.callFresh(
						(v) ->  MicroKanren.appendo(u, v, MicroKanren.list(1, 2, 3, 4))));
		s = g.run(MicroKanren.emptyState());
		assertEquals(5, MicroKanren.take(10, s).size());
	}
	
	@Test
	public void testMembero() {
		MicroKanren.Goal g = MicroKanren.callFresh(
				(u) -> MicroKanren.membero(u, MicroKanren.list(1, 2, 3, 4)));
		MicroKanren.Stream s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(4, s.size());
	}
	
	@Test
	public void testFacts() {
		MicroKanren.Fact f = new MicroKanren.Fact("id", "name", "col1", "col2");
		f.assertFact(10, "Xara", 2002, true);
		f.assertFact(20, "Django", 2005, false);
		f.assertFact(30, "Wulfur", 2013, false);
		f.assertFact(40, "Jordi", 2013, false);
		
		assertEquals(4, f.facts.size());
		assertEquals(3, f.query(MicroKanren.var(0), MicroKanren.var(1), MicroKanren.var(2), false).length);
		assertEquals(2, f.query(MicroKanren.var(0), MicroKanren.var(1), 2013, false).length);
		//MicroKanren.Cons<Object, Object> [] res = f.query("Xara", MicroKanren.var(1), 2013, false);
		Object [] [] res = f.query("Xara", MicroKanren.var(1), 2013, false);
		assertEquals(0, res.length);
		assertEquals(4, f.query(MicroKanren.var(0), MicroKanren.var(1), MicroKanren.var(2), MicroKanren.var(3)).length);
	}
	
	@Test
	public void testQueryo() {
		MicroKanren.Fact f = new MicroKanren.Fact("id", "name", "col1", "col2");
		f.assertFact(10, "Xara", 2002, true);
		f.assertFact(20, "Django", 2005, false);
		f.assertFact(30, "Wulfur", 2013, false);
		f.assertFact(40, "Jordi", 2013, false);
		
		MicroKanren.Goal g = MicroKanren.callFresh(
				(u) -> MicroKanren.queryo(f, 20, "Django", u, false));
		MicroKanren.Stream s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(1, s.size());
		
		g = MicroKanren.callFresh(
				(u) -> MicroKanren.callFresh(
						(v) -> MicroKanren.callFresh(
								(w) -> MicroKanren.queryo(f, u, v, w, false))));
		s = (MicroKanren.Stream)g.run(MicroKanren.emptyState());
		assertEquals(3, s.size());
	}	

}
