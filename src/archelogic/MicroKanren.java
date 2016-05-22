package archelogic;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

import clojure.lang.PersistentVector;


public class MicroKanren {

	// a class for conses
	public static class Cons {
		public final Object car;
		public final Object cdr;

		public Cons (Object car, Object cdr) {
			this.car = car;
			this.cdr = cdr;
		}

		public String toString() {
			if (car == null && cdr == null)
				return "()";
			else
				return "(" + this.car + " . " + this.cdr + ")";
		}

		public Cons map(Function<Object, Object> fun) {
			Object target = fun.apply(car);
			if (!isPair(cdr))
				return cons(target, fun.apply(cdr));
			if (isNil((Cons)cdr)) 
				return cons(target, cdr);
			else
				return cons(target, ((Cons)cdr).map(fun));
		}

		public boolean equals(Object o) {
			if (!(o instanceof Cons)) return false;

			Cons oCons = (Cons)o;
			if (car == null && cdr == null) 
				return oCons.car == null && oCons.cdr == null;
			else if (car == null)
				return oCons.car == null && cdr.equals(oCons.cdr);
			else
				return car.equals(oCons.car) && cdr.equals(oCons.cdr);

		}

		public Object nth(int i) {
			if (i < 0)
				throw new IndexOutOfBoundsException();
			Cons cons = this;
			for (;i > 1; i--) {
				if (!isPair(cons.cdr))
					throw new IndexOutOfBoundsException();
				cons = (Cons)cons.cdr;
			}

			Object ret = i == 0 ? cons.car : isPair(cons.cdr) ? ((Cons)cons.cdr).car : cons.cdr;
			if (ret == null)
				throw new IndexOutOfBoundsException();
			else return ret;

		}

		public int count() {
			Cons cons = this;

			int i = 0;
			while (isPair(cons.cdr)) {
				cons = (Cons)cons.cdr;
				i++;
			}

			return nil.equals(cons) ? i : cons.cdr == null ? i + 1 : i + 2; 
		}
		
		public Object [] toArray() {
			Cons cons = this;
			int len = cons.count();
			Object [] ret = new Object [len];
			
			if (len == 0)
				return ret;
			
			int i = 0;
			while (isPair(cons.cdr) && !nil.equals(cons.cdr)) {
				ret[i++] = cons.car;
				cons = (Cons)cons.cdr;
			}
			
			ret[i++] = cons.car;
			if (!nil.equals(cons.cdr) && cons.cdr != null)
				ret[i] = cons.cdr;
			
			return ret;
		}

		public static boolean isPair(Object o) {
			return o instanceof Cons;
		}
	}

	public static class NilCons extends Cons {
		public NilCons() {
			super(null, null);
		}

		public boolean equals(Object o) {
			return super.equals(o);
		}
	}

	public static NilCons nil = new NilCons();

	public static boolean isNil(Cons c) {
		return c instanceof NilCons;
	}

	public static Cons cons(Object car, Object cdr) {
		return new Cons(car, cdr);
	}

	public static boolean isPair(Object o) {
		return Cons.isPair(o); 
	}

	public static Cons list(Object ... elements) {
		if (elements.length == 0)
			return nil;
		else if (elements.length == 1)
			return cons(elements[0], nil);
		else if (elements.length == 2)
			return cons(elements[0], cons(elements[1], nil));
		else {
			Cons cons = cons(elements[elements.length - 2], cons(elements[elements.length - 1], nil));
			for (int i = elements.length - 3 ; i >= 0 ; i--)
				cons =  cons(elements[i], cons)	;

			return cons;
		}
	}

	public static Cons listVector(PersistentVector elements) {
		if (elements.count() == 0)
			return nil;
		else if (elements.count() == 1)
			return cons(elements.get(0), nil);
		else if (elements.count() == 2)
			return cons(elements.get(0), cons(elements.get(1), nil));
		else {
			Cons cons = cons(elements.get(elements.count() - 2), 
					cons(elements.get(elements.count() - 1), nil));
			for (int i = elements.count() - 3 ; i >= 0 ; i--)
				cons =  cons(elements.get(i), cons)	;

			return cons;
		}
	}

	// a class for logical variables
	public static class LVar {
		public final int id;

		public LVar (int id) {
			this.id = id;
		}

		public boolean equals(Object o) {
			return (o instanceof LVar) && this.id == ((LVar) o).id;
		}

		public String toString() {return "<" + id + ">";}

		public int hashCode() {
			return id;
		}

		public String reifyName() {
			return "_" + id;
		}
	}

	public static LVar var(int id) {
		return new LVar(id);
	}

	public static boolean isVar(Object v) {
		return v instanceof LVar;
	}

	// the default variable will match anything
	public static Character joker = '_';

	// substitution lists are just arrays of objects
	public static class Subst {
		public Object [] table;
		public boolean isEmpty;

		public Subst() {
			// default size is 16
			this.table = new Object[16];
			this.isEmpty = true;
		}

		public Subst(Subst subst) {
			this.table = Arrays.copyOf(subst.table, subst.table.length);
			this.isEmpty = subst.isEmpty;
		}

		public Subst put(LVar var, Object value) {
			if (var.id >= table.length) {
				table = Arrays.copyOf(table, 2 * table.length);
			}

			table[var.id] = value;
			isEmpty = false;

			return this;
		}

		public Object get(int i) {
			if (i >= table.length) 
				return null;
			else
				return table[i];
		}

		public Object get(LVar var) {
			return get(var.id);
		}

		public boolean isEmpty() {
			return isEmpty;
		}

		public int count() {
			int i = 0;
			while (table[i] != null) i++;

			return i;
		}

		public String toString() {
			return Arrays.toString(Arrays.copyOf(table, count()));
		}
	}

	// marker interface for all streams
	public static interface IStream {};

	// marker interface for logic solutions
	public static interface ISolution {};

	// states are a substitution list and a counter for the next variable
	public static class State implements ISolution {
		public final Subst subst;
		public int c;

		public State() {
			subst = new Subst();
			c = 0;
		}

		public State(Subst subst, int c) {
			this.subst = subst;
			this.c = c;
		}

		public String toString () {
			return subst.toString() + " c: " + c;
		}
	}

	// a stream is a list of solutions (states or immature streams)
	public static class Stream extends ArrayList<ISolution> implements IStream {
		private static final long serialVersionUID = -3200871927818052610L;

		public Stream() {
			super();
		}

		public Stream(ISolution sol) {
			this();
			this.add(sol);
		}
	}

	public static interface ImmatureStream extends IStream, ISolution {
		public IStream realize();
	}

	// a goal takes a state and returns a stream
	public interface Goal {
		public IStream run (State s);
	}

	// a delayed goal returns an immature stream
	public static class DelayedGoal implements Goal {
		Goal goal;

		public DelayedGoal(Goal goal) {
			this.goal = goal;
		}

		public IStream run(State state) {
			return (ImmatureStream) () -> goal.run(state);
		}
	}


	// a lambda_g takes a logical variable and returns a goal
	public interface LambdaGoal {
		public Goal call(LVar v);
	}

	// ============================================================================
	// microKanren core

	public static Object walk (Object u,  Subst s) {
		while (true) {
			if (!isVar(u)) 
				return u;
			Object pr = s.get((LVar)u);
			if (pr == null)
				return u;
			u = pr;
		}
	}

	public static Subst extend(LVar x, Object v, Subst s) {
		s = new Subst(s);
		s.put(x, v);
		return s;
	}

	public static Subst unify (Object u, Object v, Subst s) {
		u = walk(u, s);
		v = walk(v, s);

		if (u == null) {
			if (v == null)
				return s;
			else //cannot unify a null value with an existent
				return null;
		}

		// unification with wildcards always succeeds
		if (v == joker || u == joker) return s;

		// if both are variables and they are equal, no change to state
		if (isVar(u) && isVar(v) && u.equals(v)) return s;

		// if one is a variable, set it to the value of the other
		if (isVar(u)) return extend((LVar) u, v, s);
		if (isVar(v)) return extend((LVar) v, u, s);

		// if u and v are compatible structures, unify their elements
		// only arrays supported currently
		if (u.getClass().isArray() && v.getClass().isArray()) {
			Object [] ua = (Object [])u;
			Object [] va = (Object [])v;

			if (ua.length != va.length) return null;

			for (int i = 0; i < ua.length; i++) {
				s = unify(ua[i], va[i], s);
				if (s == null) return null;
			}

			return s;
		}

		// if u and v are conses, unify their heads and tails recursively.
		if (isPair(u) && isPair(v)) {
			s = unify(((Cons)u).car, ((Cons)v).car, s);
			if (s ==  null) return null;

			return unify(((Cons)u).cdr, ((Cons)v).cdr, s);
		}

		if (u.equals(v)) 
			return s;
		else		
			return null;

	}


	// the null stream
	public static final IStream mzero = null;

	// start a new stream
	public static IStream unit(State state) {
		return new Stream(state);
	}

	public static Goal equals(Object u, Object v) {
		return (State state) -> {
			Subst s = unify(u, v, state.subst);
			return s == mzero ? mzero : unit(new State(s, state.c));
		};
	}

	public static Goal callFresh(LambdaGoal lambdaG) {
		return (State state) -> lambdaG.call(var(state.c))
				.run(new State((Subst)state.subst, state.c + 1));
	}

	public static State emptyState() {
		return new State();
	}

	public static IStream mplus (IStream stream1, IStream stream2) {
		if (stream1 == mzero) return stream2;
		if (stream2 == mzero) return stream1;

		if (stream1 instanceof ImmatureStream) {
			return (ImmatureStream) () -> mplus(stream2, ((ImmatureStream)stream1).realize());
		} 

		if (stream2 instanceof ImmatureStream) {
			((Stream)stream1).add((ImmatureStream)stream2);
		} else {
			((Stream)stream1).addAll((Stream)stream2);
		}

		return stream1;
	}

	public static IStream bind (IStream inStream, Goal goal) {
		if (inStream == mzero) return mzero;

		if (inStream instanceof ImmatureStream) {
			return (ImmatureStream) () -> bind(((ImmatureStream)inStream).realize(), goal);
		}

		Stream mStream = (Stream)inStream;
		if (mStream.isEmpty()) return mStream;
		if (mStream.size() == 1)
			return goal.run((State)mStream.get(0));

		IStream [] streams = new Stream[mStream.size()];

		if (goal instanceof ParallelGoal) {
			List<GoalTask> tasks = new ArrayList<GoalTask>(streams.length);
			for (int i = 0; i < streams.length; i++) {
				tasks.add(new GoalTask(goal, (State)mStream.get(i)));
			}
			Collection<GoalTask> taskResults = ForkJoinTask.invokeAll(tasks);
			int i = 0;
			for (GoalTask res : taskResults) {
				streams[i++] = res.getRawResult();
			}

		} else
			for (int i = 0; i < mStream.size(); i++)
				streams[i] = goal.run((State)mStream.get(i));

		IStream outStream = mplus(streams[streams.length - 2], streams[streams.length - 1]);
		for (int i = streams.length - 3; i >= 0; i--)
			outStream = mplus(streams[i], outStream);

		return outStream;
	}


	public static Goal disj(Goal goal1, Goal goal2) {
		return (State state) -> {
			IStream s1 = goal1.run(state);
			IStream s2 = goal2.run(state);
			return mplus(s1, s2);
		};
	}

	public static Goal conj(Goal goal1, Goal goal2) {
		return (State state) -> {
			IStream s1 = goal1.run(state);
			IStream s2 = bind(s1, goal2);
			return s2;
		};
	}

	public static interface GoalCombiner {
		public Goal combine(Goal goal1, Goal goal2);
	}

	public static Goal combineAll(GoalCombiner combiner, Goal [] goals) {
		if (goals.length == 0)
			return succeed;
		else if (goals.length == 1)
			return goals[0];
		else {

			Goal g = combiner.combine(goals[goals.length - 2], goals[goals.length - 1]);
			for (int i = goals.length - 3; i >= 0; i -- ) {
				g = combiner.combine(goals[i], g);
			}
			return g;
		}
	}

	public static Goal combineAll(GoalCombiner combiner, PersistentVector goals) {
		if (goals.count() == 0)
			return succeed;
		else if (goals.count() == 1)
			return (Goal)goals.peek();
		else {

			Goal g = combiner.combine((Goal)goals.nth(goals.count() - 2), (Goal)goals.nth(goals.count() - 1));
			for (int i = goals.count() - 3; i >= 0; i -- ) {
				g = combiner.combine((Goal)goals.nth(i), g);
			}
			return g;
		}
	}

	public static Goal conjAll(Goal ... goals) {
		return combineAll((goal1, goal2) -> conj(goal1, goal2), goals);
	}

	public static Goal conjAllVector(PersistentVector goals) {
		return combineAll((goal1, goal2) -> conj(goal1, goal2), goals);
	}

	public static Goal disjAll(Goal ... goals) {
		return combineAll((goal1, goal2) -> disj(goal1, goal2), goals);
	}

	public static Goal disjAllVector(PersistentVector goals) {
		return combineAll((goal1, goal2) -> disj(goal1, goal2), goals);
	}


	// ============================================================================
	// Interface to MicroKanren


	public static Object[] reify(State state) {
		Object [] out = new Object [state.c];
		for (int i = 0; i < state.c; i++) {
			Object value = state.subst.get(i);
			out[i] = value == null ? var(i) : deepWalk(state.subst.get(i), state.subst);
		}

		return out;
	}


	public static Object deepWalk(Object u, Subst subst) {
		Object wa = walk(u, subst);
		if (isPair(wa)) {
			return ((Cons) wa).map((Object item) -> deepWalk(item, subst));
		}

		if (isVar(wa)) 
			return ((LVar)wa).reifyName();
		else
			return wa;
	}

	public static Goal Zzz(Goal g) {
		return new DelayedGoal(g);
	}

	public static Stream pull (IStream s) {
		while (s instanceof ImmatureStream) {
			s = ((ImmatureStream)s).realize();
		}

		return (Stream)s;
	}

	public static Stream take (int n, IStream s) {
		if (s == mzero) return (Stream)mzero;
		if (n == 0) return new Stream();

		Stream s1 = pull(s);
		Stream outStream = new Stream();

		int i = 0;
		for (ISolution sol : s1) {
			if (sol instanceof State) {
				outStream.add(sol);
				i++;
			} else {
				Stream realizedStream = take (n - i, (ImmatureStream)sol);
				outStream.addAll(realizedStream);
				i += realizedStream.size();
			}

			if (i == n)
				break;
		}

		return outStream;
	}

	// optimized form of take, to fully realize the stream
	public static Stream pullAll (IStream s) {
		if (s == mzero) return (Stream)mzero;

		Stream s1 = pull(s);
		if (s1 == mzero) return (Stream)mzero;

		Stream outStream = new Stream();
		for (ISolution sol : s1) {
			if (sol instanceof State)
				outStream.add(sol);
			else {
				Stream childStream = pullAll((ImmatureStream)sol);
				if (childStream != mzero)
					outStream.addAll(childStream);
			}
		}

		return outStream;
	}


	// ============================================================================
	// essential relations

	public static Goal fail = (state) -> mzero;

	public static Goal succeed = (state) -> unit(state);

	public static Goal conso(Object head, Object tail, Object out) {
		return (state) -> {
			Object wout = walk(out, state.subst);
			if (wout.equals(nil))
				return mzero;
			else
				return equals(cons(head, tail), out).run(state);
		};
	}

	public static Goal emptyo(Object o) {
		return (state) -> {
			Object wo = walk(o, state.subst);
			if (wo.getClass().isArray()) {
				return equals(o, new Object[0]).run(state);
			} else {
				return equals(o, nil).run(state);
			}
		};
	}

	public static Goal ntho(Object o, Object i, Object out) {
		return (state) -> {
			Object wo = walk(o, state.subst);
			Number wi = (Number)walk(i, state.subst);
			Object wout = walk(out, state.subst);
			if (wo.getClass().isArray()) {
				return equals(wout, ((Object []) wo)[wi.intValue()]).run(state);
			} else {
				return equals(wout, ((Cons)wo).nth(wi.intValue())).run(state);
			}
		};
	}

	public static Goal counto(Object o, Object out) {
		return (state) -> {
			Object wo = walk(o, state.subst);
			Object wout = walk(out, state.subst);
			if (wo.getClass().isArray()) {
				return equals(wout, ((Object []) wo).length).run(state);
			} else {
				return equals(wout, ((Cons)wo).count()).run(state);
			}
		};
	}

	public static Goal not(Goal g) {
		return (state) -> {
			IStream s = g.run(state);
			return s == mzero ? unit(state) : mzero;
		};
	}
	
	public static IStream _conde(Goal [] conjGoals, State state) {
		if (conjGoals.length == 0)
			return succeed.run(state);
		else if (conjGoals.length == 1)
			return conjGoals[0].run(state);
		else {
			Goal g = disjAll(conjGoals);
			return g.run(state);
		}

	}

	public static Goal conde(Goal [] ... goalSets) {
		return (state) -> {
			Goal [] conjGoals = new Goal[goalSets.length];
			int i = 0;
			for (Goal [] goalSet : goalSets) {
				conjGoals[i++] = conjAll(goalSet);
			}

			return _conde(conjGoals, state);
		};
	}

	public static Goal condeVector(PersistentVector goalSets) {
		return (state) -> {
			Goal [] conjGoals = new Goal[goalSets.count()];

			for (int i = 0; i < goalSets.count(); i++) {
				PersistentVector goalSet = (PersistentVector)goalSets.get(i);
				conjGoals[i] = conjAllVector(goalSet);
			}

			return _conde(conjGoals, state);
		};

	}

	public static Goal condu(Goal [] ... goals) {
		Goal [] conjGoals = new Goal[goals.length];
		int i = 0;
		for (Goal [] goalSet : goals) {
			conjGoals[i++] = conjAll(goalSet);
		}

		return (state) -> {
			IStream s = mzero;
			for (Goal goal : conjGoals) {
				s = goal.run(state);
				if (s != mzero)
					return s;
			}

			return s;
		};
	}

	public static Goal printlno(Object o) {
		return (state) -> {
			Object wo = deepWalk(o, state.subst);
			System.out.println(wo);
			return unit(state);
		};
	}

	public static IStream call(Goal goal, State state) {
		return goal.run(state);	
	}

	public static Goal [] goals(Goal ... goals) {
		return goals;
	}


	public static Goal appendo(Object a, Object b, Object c) {
		Goal recursiveGoal = callFresh((h) ->
		callFresh((t) ->
		callFresh((c1) -> 
		conj(conso(h, t, a),
				conj(conso(h, c1, c),
						appendo(t, b, c1))))));
		//Zzz(appendo(t, b, c1)))))));

		return conde(goals(emptyo(a), equals(b, c)), goals(recursiveGoal));
	}

	public static Goal membero(Object a, Object b) {
		return conde(goals(conso(a, joker, b)),
				goals(callFresh(
						(c) -> conj(conso(joker, c, b), 
								membero(a, c)))));
	}


	// ============================================================================
	// parallelization

	public static class ParallelGoal implements Goal {
		Goal goal;

		public ParallelGoal(Goal goal) {
			this.goal = goal;
		}

		@Override
		public IStream run(State state) {
			return goal.run(state);
		}

	}

	public static ParallelGoal par(Goal goal) {
		return new ParallelGoal(goal);
	}


	public static class GoalTask extends RecursiveTask<IStream> {
		private static final long serialVersionUID = -231570780773214413L;
		Goal goal;
		State state;

		public GoalTask(Goal goal, State state) {
			this.goal = goal;
			this.state = state;
		}

		@Override
		protected IStream compute() {
			return goal.run(state);
		}	
	}

	public static Goal pdisj(Goal goal1, Goal goal2) {
		return (State state) -> {
			IStream s1 = null;
			IStream s2 = null;

			if (goal1 instanceof ParallelGoal) {
				GoalTask t1 = new GoalTask(goal1, state);
				t1.fork();
				s2 = goal2.run(state);
				s1 = t1.join();
			} else if (goal2 instanceof ParallelGoal) {
				GoalTask t2 = new GoalTask(goal2, state);
				t2.fork();
				s1 = goal1.run(state);
				s2 = t2.join();
			} else {
				s1 = goal1.run(state);
				s2 = goal2.run(state);
			}
			return mplus(s1, s2);
		};
	}

	public static Goal pdisjAll (Goal ... goals) {
		return combineAll((goal1, goal2) -> pdisj(goal1, goal2), goals);
	}

	

	// ============================================================================
	// facts database

	public static class Fact {
		public String [] fields;
		public HashMap<Object, HashSet<Integer>> [] indices;
		public ArrayList<Object []> facts;

		public static Object[][] emptyResultSet = new Object[0][];

		@SuppressWarnings("unchecked")
		public Fact(String ... fields) {
			this.fields = fields;
			indices = (HashMap<Object, HashSet<Integer>> []) new HashMap[fields.length];
			for (int i = 0; i < fields.length; i++)
				indices[i] = new HashMap<Object,HashSet<Integer>>();
			facts = new ArrayList<Object []>();
		}

		public void assertFact(Object ... values) {
			//System.out.println("Assert: " + Arrays.toString(values));
			if (values.length != fields.length) 
				throw new IllegalArgumentException("Wrong number of fields in fact");
			facts.add(values);
			int pos = facts.size() - 1;

			for (int i = 0; i < fields.length; i++) {
				HashSet<Integer> indexEntry = indices[i].getOrDefault(values[i], new HashSet<Integer>());
				indexEntry.add(pos);
				indices[i].put(values[i], indexEntry);
			}
		}

		public Object [] getFact(int i) {
			return facts.get(i);
		}

		public Object[][] query(Object ... terms) {
			//System.out.println("Query: " + Arrays.toString(terms));

			if (terms.length != fields.length) 
				throw new IllegalArgumentException("Wrong number of terms in query: " + terms.length + " vs " + fields.length);

			ArrayList<HashSet<Integer>> idx = new ArrayList<HashSet<Integer>>();
			for (int i = 0; i < fields.length; i++) {
				if (!isVar(terms[i]) && !joker.equals(terms[i])) {
					HashSet<Integer> indexEntry = indices[i].getOrDefault(terms[i], new HashSet<Integer>());
					idx.add(indexEntry);
				}
			}

			if (idx.size() == 0)
				return facts.toArray(emptyResultSet);

			idx.sort((set1, set2) -> Integer.compare(set1.size(), set2.size()));

			ArrayList<Object []> resultSet = new ArrayList<Object[]>();
			for (Iterator<Integer> i = idx.get(0).iterator();i.hasNext();) {
				boolean keep = true;
				Integer id = i.next();
				for (int j = 1; j < idx.size(); j++) {
					if (!idx.get(j).contains(id)) {
						keep = false;
						break;
					}
				}
				if (keep) 
					resultSet.add(facts.get(id));
			}

			return resultSet.toArray(emptyResultSet);
		}
	}

	public static Fact fact(String ... fields) {
		return new Fact(fields);
	}

	public static IStream _queryo(Fact fact, State state, Object ... terms) {
		// run the query
		Object[][] res = fact.query(terms);

		// if no results, fail
		if (res.length == 0)
			return mzero;

		// turn each result row into a goal by unifying with the query
		Goal [] goals = new Goal[res.length];
		for (int i = 0; i < res.length; i++) {
			goals[i] = equals(terms, res[i]);
		}

		// return the disjunction of all the unifications

		return disjAll(goals).run(state);

	}

	public static Goal queryo(Fact fact, Object ... terms) {
		// walk variables in query
		return (state) -> {
			Object [] wterms = new Object[terms.length];
			for (int i = 0; i < terms.length; i++) {
				wterms[i] = walk(terms[i], state.subst);
			}

			return _queryo(fact, state, wterms);
		};
	}

	public static Goal queryoVect(Fact fact, PersistentVector terms) {
		// walk variables in query
		return (state) -> {
			Object [] wterms = new Object[terms.count()];
			for (int i = 0; i < terms.count(); i++) {
				wterms[i] = walk(terms.nth(i), state.subst);
			}

			return _queryo(fact, state, wterms);
		};
	}


	// =============================================================================
	// Arithmetic
	// these functions are non-relational.  The arguments must resolve to doubles.

	public static Goal pluso(Object a, Object b, Object c) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			return equals(wa.doubleValue() + wb.doubleValue(), c).run(state);
		};
	}

	public static Goal minuso(Object a, Object b, Object c) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			return equals(wa.doubleValue() - wb.doubleValue(), c).run(state);
		};
	}

	public static Goal multo(Object a, Object b, Object c) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			return equals(wa.doubleValue() * wb.doubleValue(), c).run(state);
		};
	}

	public static Goal divo(Object a, Object b, Object c) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			return equals(wa.doubleValue() / wb.doubleValue(), c).run(state);
		};
	}

	public static Goal eqo(Object a, Object b) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			if (wa.equals(wb)) 
				return unit(state);
			else 
				return mzero;
		};
	}

	public static Goal gto(Object a, Object b) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			if (wa.doubleValue() > wb.doubleValue()) 
				return unit(state);
			else 
				return mzero;
		};
	}

	public static Goal lto(Object a, Object b) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			if (wa.doubleValue() < wb.doubleValue()) 
				return unit(state);
			else 
				return mzero;
		};
	}

	public static Goal gtEqo(Object a, Object b) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			if (wa.doubleValue() >= wb.doubleValue()) 
				return unit(state);
			else 
				return mzero;
		};
	}

	public static Goal ltEqo(Object a, Object b) {
		return (state) -> {
			Number wa = (Number)walk(a, state.subst);
			Number wb = (Number)walk(b, state.subst);

			if (wa.doubleValue() <= wb.doubleValue()) 
				return unit(state);
			else 
				return mzero;
		};
	}



	// ============================================================================
	// main function as performance test

	public static void main(String [] args) {
		long t1 = System.currentTimeMillis();

		for (int i = 0; i < 20_000_000; i++) {
			Goal g = MicroKanren.callFresh(
					(u) -> MicroKanren.callFresh(
							(v) ->  MicroKanren.appendo(u, v, MicroKanren.list(1, 2, 3, 4))));
			MicroKanren.pullAll(g.run(MicroKanren.emptyState()));
		}
		System.out.println(System.currentTimeMillis() - t1);
		
	}

}
