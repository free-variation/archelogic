:- use_module(library(apply)).
:- use_module(library(yall)).


debug(Value) :-
	nl, print('DEBUG '),
	print(Value),
	nl.

lambda_reduce(F, Goal) :-
	nonvar(F),
	F = beta(Lambda, Vars), 
	is_lambda(Lambda), !,
	lambda_calls(Lambda, Vars, ReducedLambda),
	lambda_reduce(ReducedLambda, Goal).

lambda_reduce(F, F) :- 
	nonvar(F),
	F = [], !.

lambda_reduce(F, F) :-
	var(F), !. 

lambda_reduce(F, F) :-
	atom(F), !.

lambda_reduce(F, Goal) :-
	nonvar(F), 
	\+atom(F),
	F =.. Terms,
	maplist(lambda_reduce, Terms, Terms1),
	Goal =.. Terms1.



% all humans run
%S ={X}/[P]>>(\+(human(X)); beta(P,[X])), P={}/[Y]>>E1^(runs(E1), subject(E1,Y)),lambda_reduce(beta(S, [P]),G).

f(A, B, Result) :-
	atom(A),
	Result =.. [A | [B]].

f(A, B, Result) :-
	compound(A),
	A =.. [Functor | Args],
	append(Args, [B], Args1),
	Result =.. [Functor | Args1].

% the marked form of functional application 
% for handling existentially-closed arguments
create_predicate(Functor, Arg, Result) :-
	nonvar(Arg),
	Arg = X^Formula, !,
	f(Functor, X, Result1),
	Formula1 = (Formula, Result1),
	Result = X^Formula1.

% also for a universally quantified expression
create_predicate(Functor, Arg, Result) :-
	nonvar(Arg),
	Arg = {X}/Formula, !,
	f(Functor, X, Result1),
	Formula1 = (Formula, Result1),
	Result = {X}/Formula1.

% also for a universally quantified expression over lambdas
create_predicate(Functor, Arg, Result) :-
	nonvar(Arg),
	Arg = {X}/[Y]>>Formula, !,
	f(Functor, X, Result1),
	Formula1 = (Formula, Result1),
	Result = {X}/[Y]>>Formula1.


create_predicate(Functor, Arg, Result) :-
	f(Functor, Arg, Result).


relations_of_type(Type, Relations, RelationsOfType) :-
	include({Type}/[Rel]>>(Rel=rel(Type, _, _)), Relations, RelationsOfType).

relations_for_governor(WordIndex, Relations, RelationsForGovernor) :-
	include({WordIndex}/[Rel]>>(Rel=rel(_, word(WordIndex, _, _, _), _)), Relations, RelationsForGovernor).

member_object(X, List) :-
	List = [L | Rest],
	(	X == L
	;	member_object(X, Rest)
	).

raise_universal_q(X^(Terms-_), Vars) :-
	maplist(raise_universal_q, Terms, V),!,
	flatten(V, V1),
	exclude({X}/[Y]>>member_object(Y,X), V1, V2),
	list_to_set(V2, Vars).

raise_universal_q(Term, Vars) :-
	term_variables(Term, Vars).


skolemize(X, Vars, _) :-
	exclude({X}/[Y]>>(X==Y), Vars, Vars1),
	gensym(s, Functor),
	X =.. [Functor | Vars1].

raise_existential_q(Vars, X^(Terms-T), BareTerms) :-
	!,
	T = [],
	% skolemize(X, Vars, Terms),
	maplist(raise_existential_q(Vars), Terms, BareTerms).

raise_existential_q(_, Term, Term).

cnf(LogicalForm, cnf(Vars, FlatTerms)) :-
	term_variables(LogicalForm, Vars),
	raise_existential_q(Vars, LogicalForm, BareTerms),
	flatten(BareTerms, FlatTerms).
	

logical_form(Relations, LogicalForm) :-	
	is_list(Relations),
	relations_of_type(root, Relations, [RootRelation]),
	logical_form(RootRelation, Relations, LogicalForm).

logical_form(RootRel, Relations, LogicalForm) :-
	RootRel = rel(root, _, _),
	clause(RootRel, Relations, LogicalForm), !.
	
% default: return the dependent as an entity
logical_form(rel(_, _, word(_, Form, _, _)), _, Form).

% ----- The core clause -----

clause(Rel, Relations, LogicalForm) :-
	Rel = rel(_, _, Word2),
	Word2 = word(WordIndex, _, _, _),
	relations_for_governor(WordIndex, Relations, PredRelations),

	SubjectRel = rel(nsubj, Word2, _),
	member(SubjectRel, PredRelations),
	subject(SubjectRel, Relations, SubjectLF),

	predicate(Rel, Relations, PredicateLF),
	LogicalForm = beta(SubjectLF, [PredicateLF]).


% ----- Nominal Expressions -----


subject(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, _, HeadLemma, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	(	POS = 'NNP' ->
		LF = (HeadLemma = X)
	;	LF =.. [HeadLemma, X]
	),
	(	HeadRels = [] ->
		LogicalForm = {}/[P]>>(X^(LF, beta(P, [X]))) 
	;	subject_dp(HeadRels, POS, Relations, X^LF, LogicalForm)
	).
	
% base case: no relations left
subject_dp([], _, _, LogicalForm, LogicalForm).

% ignore predeterminers [TODO jds forever?]
subject_dp([rel('det:predet', _, _) | Rels], POS, Relations, LF, LogicalForm) :-
	subject_dp(Rels, POS, Relations, LF, LogicalForm).
	

% relative clause
subject_dp([Rel | Rels], POS, Relations, X^LF, LogicalForm) :-
 	Rel = rel('acl:relcl', _, _),
 	relative_clause(Rel, Relations, Y, RelativeClauseLF),
 	subject_dp(Rels, POS, Relations, Vars^LF1, LogicalForm).

% ----- generalized quantifiers -----
% determiner: all, every
subject_dp([Rel | Rels], POS, Relations, X^LF, LogicalForm) :-
	Rel = rel('det', _, word(_, _, Det, 'DT')),
	(	Det = all; Det = every),
	subject_dp(Rels, POS, Relations, {X}/[P]>>(\+LF;beta(P,[X])), LogicalForm).

% determiner: the
subject_dp([Rel | Rels], POS, Relations, X^LF, LogicalForm) :-
	Rel = rel('det', _, word(_, _, the, 'DT')),
	(	POS = 'NNS'	-> % check for plural nouns
		subject_dp(Rels, POS, Relations, {X}/[P]>>(\+LF;beta(P,[X])), LogicalForm)
	; 	subject_dp(Rels, POS,  Relations, {X}/[P]>>Y^(\+LF; (X = Y, beta(P,[X]))), LogicalForm)
	).

% determiner: a
subject_dp([Rel | Rels], POS, Relations, X^LF, LogicalForm) :-
	Rel = rel('det', _, word(_, _, a, 'DT')),
	subject_dp(Rels, POS, Relations, {}/[P]>>X^(LF, beta(P,[X])), LogicalForm).

% determiner: no
subject_dp([Rel | Rels], POS, Relations, X^LF, LogicalForm) :-
	Rel = rel('neg', _, word(_, _, no, 'DT')),
	subject_dp(Rels, POS, Relations, {}/[P]>>(\+X^(LF, beta(P,[X]))), LogicalForm).

% subject_dp default: behave as unmarked dp
subject_dp(Rels, POS, Relations, LF, LogicalForm) :-
	dp(Rels, POS, Relations, LF, LogicalForm).


% determiner: default (do nothing)
dp([], _, _, LogicalForm, LogicalForm).

dp([Rel | Rels], POS, Relations, LF, LogicalForm) :-
	Rel = rel('det', _, _),
	dp(Rels, POS, Relations, LF, LogicalForm).


% ----- Predicates -----

predicate(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(_, _, HeadLemma, _),
	Predicate =.. [HeadLemma, E],
	ObjectRel = rel(dobj, Word2, _),
	(	member(ObjectRel, Relations) ->
		(	object(ObjectRel, Relations, ObjectLF),
			LogicalForm = {}/[X]>>E^(Predicate, subject(E, X), beta(ObjectLF, [E]))
		)
	;	LogicalForm = {}/[X]>>E^(Predicate, subject(E, X))
	).



object(rel(_, _, Word2), Relations, LogicalForm) :-
	Word2 = word(WordIndex, _, HeadLemma, POS),
	relations_for_governor(WordIndex, Relations, HeadRels),
	(	POS = 'NNP' ->
		LF = (HeadLemma = X)
	;	f(HeadLemma, X, LF)
	),
	LF1 = {}/[E]>>(X^(LF, object(E,X))),
	(	HeadRels = [] ->
		LogicalForm = LF1
	;	object_dp(HeadRels, POS, Relations, LF1, LogicalForm)
	).

% determiner: all, every
object_dp([Rel | Rels], POS, Relations, {}/[E]>>(X^(LF, object(E,X))) , LogicalForm) :-
	Rel = rel('det', _, word(_, _, Det, 'DT')),
	(	Det = all; Det = every),
	object_dp(Rels, POS, Relations, {X}/[E]>>(\+LF ; object(E, X)), LogicalForm).


% determiner: the
object_dp([Rel | Rels], POS, Relations, {}/[E]>>(X^(LF, object(E,X))), LogicalForm) :-
	Rel = rel('det', _, word(_, _, the, 'DT')),
	(	POS = 'NNS' -> % check for plural nouns
		object_dp(Rels, POS, Relations, {X}/[E]>>(\+LF ; object(E, X)), LogicalForm)
	;	object_dp(Rels, POS, Relations, {X}/[E]>>(Y^(\+LF; (X = Y, object(E, X)))), LogicalForm)
	).


% determiner: no
object_dp([Rel | Rels], POS, Relations, {}/[E]>>(X^(LF, object(E,X))), LogicalForm) :-
	Rel = rel('neg', _, word(_, _, no, 'DT')),
	object_dp(Rels, POS, Relations, {}/[E]>>(\+X^(LF, object(E, X))), LogicalForm).


% object_dp default: behave as unmarked dp

object_dp(Rels, POS, Relations, LF, LogicalForm) :-
	dp(Rels, POS, Relations, LF, LogicalForm).

% ----- Relative Clauses -----
relative_pronoun(who).
relative_pronoun(whom).
relative_pronoun(that).
relative_pronoun(which).

relative_adverb(when).
relative_adverb(where).
relative_adverb(why).


relative_clause(rel('acl:relcl', _, Word2), Relations, X, RelativeClauseLF).






